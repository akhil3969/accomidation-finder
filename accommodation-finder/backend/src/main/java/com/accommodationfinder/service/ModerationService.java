package com.accommodationfinder.service;

import com.accommodationfinder.dto.AdminDtos.*;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reports in, decisions out - with a record of who decided what.
 *
 * <p>Every action a moderator takes is written to {@link ModerationAction}. Moderation
 * without an audit trail is just an unaccountable person with a delete button, and on a
 * platform where hiding a listing costs a landlord money, being able to answer "who did
 * this, and why" matters.
 */
@Service
public class ModerationService {

    private final ReportRepository reportRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ModerationActionRepository actionRepository;
    private final ScamDetectionService scamDetectionService;

    public ModerationService(ReportRepository reportRepository,
                             RoomRepository roomRepository,
                             UserRepository userRepository,
                             ModerationActionRepository actionRepository,
                             ScamDetectionService scamDetectionService) {
        this.reportRepository = reportRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.scamDetectionService = scamDetectionService;
    }

    // ------------------------------------------------------------ user facing

    @Transactional
    public ReportView submit(ReportRequest request, User reporter) {
        if (request.roomId() == null && request.reportedUserId() == null) {
            throw new BadRequestException("Tell us which listing or which person you are reporting");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setReason(request.reason());
        report.setDetails(request.details());

        if (request.roomId() != null) {
            if (reportRepository.existsByReporterIdAndRoomId(reporter.getId(), request.roomId())) {
                throw new ConflictException("You have already reported this listing. "
                        + "Our team is looking at it.");
            }
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room", request.roomId()));
            report.setRoom(room);
        }

        if (request.reportedUserId() != null) {
            User reported = userRepository.findById(request.reportedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.reportedUserId()));
            if (reported.getId().equals(reporter.getId())) {
                throw new BadRequestException("You cannot report yourself");
            }
            report.setReportedUser(reported);
        }

        Report saved = reportRepository.save(report);

        // A fresh report is itself a risk signal, so recompute the listing's score now.
        if (saved.getRoom() != null) {
            roomRepository.save(scamDetectionService.rescore(saved.getRoom()));
        }
        return toView(saved);
    }

    // ----------------------------------------------------------- moderator side

    @Transactional(readOnly = true)
    public Page<ReportView> queue(ReportStatus status, Pageable pageable) {
        Page<Report> page = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(this::toView);
    }

    @Transactional
    public ReportView resolve(Long reportId, ResolveReportRequest decision, User moderator) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        report.setStatus(decision.status());
        report.setModeratorNote(decision.moderatorNote());
        report.setHandledBy(moderator);
        if (decision.status() == ReportStatus.ACTION_TAKEN
                || decision.status() == ReportStatus.DISMISSED) {
            report.setResolvedAt(LocalDateTime.now());
        }

        if (Boolean.TRUE.equals(decision.hideListing()) && report.getRoom() != null) {
            Room room = report.getRoom();
            room.setHidden(true);
            room.setAvailable(false);
            roomRepository.save(room);
            record(moderator, "HIDE_LISTING", "ROOM", room.getId(), decision.moderatorNote());
        }

        if (Boolean.TRUE.equals(decision.banUser())) {
            User target = report.getReportedUser() != null
                    ? report.getReportedUser()
                    : (report.getRoom() != null ? report.getRoom().getLandlord() : null);
            if (target != null) {
                target.setEnabled(false);
                userRepository.save(target);
                record(moderator, "BAN_USER", "USER", target.getId(), decision.moderatorNote());
            }
        }

        record(moderator, "RESOLVE_REPORT", "REPORT", reportId,
                decision.status() + (decision.moderatorNote() == null ? "" : " - " + decision.moderatorNote()));

        return toView(reportRepository.save(report));
    }

    @Transactional
    public void setListingHidden(Long roomId, boolean hidden, String reason, User moderator) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        room.setHidden(hidden);
        if (hidden) {
            room.setAvailable(false);
        }
        roomRepository.save(room);
        record(moderator, hidden ? "HIDE_LISTING" : "RESTORE_LISTING", "ROOM", roomId, reason);
    }

    @Transactional
    public void setListingVerified(Long roomId, boolean verified, User moderator) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        room.setVerified(verified);
        roomRepository.save(room);
        record(moderator, verified ? "VERIFY_LISTING" : "UNVERIFY_LISTING", "ROOM", roomId, null);
    }

    @Transactional
    public UserDtos.UserSummary decideVerification(Long userId, VerificationDecision decision, User moderator) {
        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean approve = Boolean.TRUE.equals(decision.approve());
        landlord.setVerificationStatus(approve ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
        landlord.setVerificationNote(decision.note());
        landlord.setVerifiedAt(approve ? LocalDateTime.now() : null);
        userRepository.save(landlord);

        record(moderator, approve ? "VERIFY_LANDLORD" : "REJECT_LANDLORD",
                "USER", userId, decision.note());

        // Verification changes the risk picture for every listing they own.
        roomRepository.findByLandlordId(userId, Pageable.ofSize(100))
                .forEach(room -> roomRepository.save(scamDetectionService.rescore(room)));

        return UserDtos.UserSummary.from(landlord);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserSummary> awaitingVerification() {
        return userRepository.findByVerificationStatusOrderByIdDesc(VerificationStatus.PENDING)
                .stream()
                .map(UserDtos.UserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ModerationEntry> auditLog(Pageable pageable) {
        return actionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(action -> new ModerationEntry(
                        action.getId(),
                        action.getModerator() == null ? "system" : action.getModerator().getName(),
                        action.getAction(),
                        action.getTargetType(),
                        action.getTargetId(),
                        action.getReason(),
                        action.getCreatedAt()));
    }

    // ------------------------------------------------------------------ helpers

    private void record(User moderator, String action, String targetType, Long targetId, String reason) {
        actionRepository.save(new ModerationAction(moderator, action, targetType, targetId, reason));
    }

    private ReportView toView(Report report) {
        return new ReportView(
                report.getId(),
                UserDtos.UserSummary.publicProfile(report.getReporter()),
                report.getRoom() == null ? null : report.getRoom().getId(),
                report.getRoom() == null ? null : report.getRoom().getTitle(),
                report.getRoom() == null ? null : report.getRoom().getRiskScore(),
                UserDtos.UserSummary.publicProfile(report.getReportedUser()),
                report.getReason(),
                humanise(report.getReason()),
                report.getDetails(),
                report.getStatus(),
                report.getModeratorNote(),
                report.getHandledBy() == null ? null : report.getHandledBy().getName(),
                report.getCreatedAt(),
                report.getResolvedAt());
    }

    private String humanise(ReportReason reason) {
        return switch (reason) {
            case SUSPECTED_SCAM -> "Looks like a scam";
            case ASKED_FOR_MONEY_UPFRONT -> "Asked for money before a viewing";
            case LISTING_DOES_NOT_EXIST -> "The flat does not exist";
            case MISLEADING_PHOTOS -> "Photos do not match the flat";
            case DISCRIMINATION -> "Discrimination";
            case ABUSIVE_BEHAVIOUR -> "Abusive behaviour";
            case ALREADY_RENTED -> "Already rented out";
            case OTHER -> "Something else";
        };
    }
}

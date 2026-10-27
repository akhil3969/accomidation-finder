package com.accommodationfinder.service;

import com.accommodationfinder.dto.AccountDtos.ChecklistEntry;
import com.accommodationfinder.dto.AccountDtos.PersonalDataExport;
import com.accommodationfinder.dto.AccountDtos.Profile;
import com.accommodationfinder.dto.AccountDtos.ReportFiled;
import com.accommodationfinder.dto.NotificationDtos.NotificationResponse;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.BookingMapper;
import com.accommodationfinder.mapper.MessageMapper;
import com.accommodationfinder.mapper.ReviewMapper;
import com.accommodationfinder.mapper.RoomMapper;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The two GDPR rights that need code: access/portability (export) and erasure.
 *
 * Erasure anonymises rather than deleting the user row. Bookings and conversations are
 * shared records - the landlord has to keep their booking history, the tenant on the
 * other end of a chat keeps their half - so the row stays as an empty shell called
 * "Deleted user" and everything that identifies the person is removed or redacted.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    static final String DELETED_NAME = "Deleted user";
    static final String REDACTED_MESSAGE = "[Message removed - the sender deleted their account]";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChecklistProgressRepository checklistProgressRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final UserTokenRepository userTokenRepository;
    private final BookingMapper bookingMapper;
    private final RoomMapper roomMapper;
    private final MessageMapper messageMapper;
    private final ReviewMapper reviewMapper;
    private final UserNotificationService userNotificationService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AccountService(UserRepository userRepository,
                          BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          MessageRepository messageRepository,
                          ReviewRepository reviewRepository,
                          FavoriteRepository favoriteRepository,
                          ChecklistProgressRepository checklistProgressRepository,
                          ReportRepository reportRepository,
                          NotificationRepository notificationRepository,
                          UserTokenRepository userTokenRepository,
                          BookingMapper bookingMapper,
                          RoomMapper roomMapper,
                          MessageMapper messageMapper,
                          ReviewMapper reviewMapper,
                          UserNotificationService userNotificationService,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.checklistProgressRepository = checklistProgressRepository;
        this.reportRepository = reportRepository;
        this.notificationRepository = notificationRepository;
        this.userTokenRepository = userTokenRepository;
        this.bookingMapper = bookingMapper;
        this.roomMapper = roomMapper;
        this.messageMapper = messageMapper;
        this.reviewMapper = reviewMapper;
        this.userNotificationService = userNotificationService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ export

    @Transactional(readOnly = true)
    public PersonalDataExport export(Long userId) {
        User user = load(userId);
        return new PersonalDataExport(
                LocalDateTime.now(),
                "Everything AccomFinder holds about you, as of the time above. "
                        + "Other people's names appear only where they are part of your own records.",
                profileOf(user),
                bookingRepository.findAllByTenantIdOrderByRequestedAtDesc(userId).stream()
                        .map(bookingMapper::toResponse).toList(),
                roomRepository.findAllByLandlordId(userId).stream()
                        .map(roomMapper::toResponse).toList(),
                messageRepository.findAllForUser(userId).stream()
                        .map(messageMapper::toResponse).toList(),
                reviewRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                        .map(reviewMapper::toResponse).toList(),
                favoriteRepository.findRoomIdsByUserId(userId),
                checklistProgressRepository.findByUserId(userId).stream()
                        .map(p -> new ChecklistEntry(p.getItem().getTitle(), p.isDone(), p.getNote(), p.getUpdatedAt()))
                        .toList(),
                reportRepository.findByReporterIdOrderByCreatedAtDesc(userId).stream()
                        .map(r -> new ReportFiled(r.getId(),
                                r.getRoom() == null ? null : r.getRoom().getId(),
                                r.getReportedUser() == null ? null : r.getReportedUser().getId(),
                                String.valueOf(r.getReason()), r.getDetails(),
                                String.valueOf(r.getStatus()), r.getCreatedAt()))
                        .toList(),
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                        .map(NotificationResponse::from).toList());
    }

    // ----------------------------------------------------------------- erasure

    /** Self-service erasure. Asks for the password again so a stolen session cannot do it. */
    @Transactional
    public void deleteOwnAccount(User current, String password) {
        User user = load(current.getId());
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("That password is not correct");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ConflictException("You are the only administrator - make someone else an admin first");
        }
        erase(user);
    }

    /** Used by self-service deletion and by admins; leaves no personal data behind. */
    @Transactional
    public void erase(User target) {
        User user = load(target.getId());
        if (user.getDeletedAt() != null) {
            return;                                            // already erased - idempotent
        }
        Long id = user.getId();
        String originalEmail = user.getEmail();
        String originalName = user.getName();

        // Open bookings either side end cleanly, and the other party is told why.
        for (Booking booking : bookingRepository.findByTenantAndStatus(id, BookingStatus.PENDING)) {
            booking.setStatus(BookingStatus.CANCELLED);
            userNotificationService.notify(booking.getRoom().getLandlord(), NotificationType.BOOKING_CANCELLED,
                    "A booking request was withdrawn",
                    "The tenant who asked for " + booking.getRoom().getTitle() + " has closed their account.",
                    "/dashboard");
        }
        for (Booking booking : bookingRepository.findByLandlordAndStatus(id, BookingStatus.PENDING)) {
            booking.setStatus(BookingStatus.REJECTED);
            booking.setLandlordResponse("The landlord has closed their account.");
            userNotificationService.notify(booking.getTenant(), NotificationType.BOOKING_REJECTED,
                    "Your booking request was declined: " + booking.getRoom().getTitle(),
                    "The landlord has closed their account.", "/bookings");
        }

        // Listings leave the site but stay in the database for existing bookings and reports.
        for (Room room : roomRepository.findAllByLandlordId(id)) {
            room.setHidden(true);
            room.setAvailable(Boolean.FALSE);
        }

        messageRepository.redactSentBy(id, REDACTED_MESSAGE);
        reviewRepository.deleteByAuthorId(id);
        favoriteRepository.deleteByUserId(id);
        checklistProgressRepository.deleteByUserId(id);
        reportRepository.detachReporter(id);
        notificationRepository.deleteByRecipientId(id);
        userTokenRepository.deleteByUserId(id);

        anonymise(user);
        userRepository.save(user);

        emailService.sendAccountDeleted(originalEmail, originalName);
        log.info("Account {} erased", id);
    }

    private void anonymise(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setName(DELETED_NAME);
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");   // .invalid can never receive mail
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhone(null);
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setNationality(null);
        user.setEuNational(null);
        user.setDateOfBirth(null);
        user.setArrivalDate(null);
        user.setScholarshipHolder(false);
        user.setUniversity(null);
        user.setUniversityLatitude(null);
        user.setUniversityLongitude(null);
        user.setMonthlyIncome(null);
        user.setHasFrenchGuarantor(false);
        user.setProfileCompleted(false);
        user.setVerificationStatus(VerificationStatus.NOT_SUBMITTED);
        user.setVerificationNote(null);
        user.setVerifiedAt(null);
        user.setEmailVerified(false);
        user.setEmailNotifications(false);
        user.setEnabled(false);
        user.revokeAllTokens();                              // any token still out there dies
        user.setDeletedAt(now);
    }

    private User load(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static Profile profileOf(User u) {
        return new Profile(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getAvatarUrl(),
                u.getBio(), u.getCreatedAt(), u.isEmailVerified(), u.isEmailNotifications(),
                u.getTermsAcceptedAt(), u.getTermsVersion(), u.getNationality(), u.getEuNational(),
                u.getDateOfBirth(), u.getArrivalDate(), u.isStudent(), u.isScholarshipHolder(),
                u.getUniversity(), u.getMonthlyIncome(), u.isHasFrenchGuarantor(), u.getVerificationStatus());
    }
}

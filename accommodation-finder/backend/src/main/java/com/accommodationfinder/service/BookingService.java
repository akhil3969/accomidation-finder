package com.accommodationfinder.service;

import com.accommodationfinder.dto.BookingDtos.*;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.exception.ForbiddenException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.BookingMapper;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.BookingRepository;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          BookingMapper bookingMapper,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingMapper = bookingMapper;
        this.notificationService = notificationService;
    }

    // ---------------------------------------------------------------- commands

    @Transactional
    public BookingResponse request(BookingRequest request, User tenant) {
        Room room = roomRepository.findByIdWithLandlord(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.roomId()));

        validateDates(request.checkIn(), request.checkOut());

        if (room.getLandlord().getId().equals(tenant.getId())) {
            throw new BadRequestException("You cannot book your own listing");
        }
        if (!Boolean.TRUE.equals(room.getAvailable())) {
            throw new ConflictException("This listing is no longer available");
        }
        int guests = request.guests() == null ? 1 : request.guests();
        if (room.getMaxGuests() != null && guests > room.getMaxGuests()) {
            throw new BadRequestException("This listing accepts at most " + room.getMaxGuests() + " guest(s)");
        }
        if (bookingRepository.existsOverlappingBooking(room.getId(), request.checkIn(), request.checkOut())) {
            throw new ConflictException("Those dates are already requested or booked");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setTenant(tenant);
        booking.setCheckIn(request.checkIn());
        booking.setCheckOut(request.checkOut());
        booking.setGuests(guests);
        booking.setMessage(request.message());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(estimateTotal(room.getPrice(), request.checkIn(), request.checkOut()));

        BookingResponse response = bookingMapper.toResponse(bookingRepository.save(booking));
        notificationService.bookingEvent(room.getLandlord().getEmail(), response);
        return response;
    }

    /** Landlord accepts, rejects or completes a request. */
    @Transactional
    public BookingResponse decide(Long bookingId, BookingDecisionRequest decision, User actor) {
        Booking booking = getEntity(bookingId);
        Long landlordId = booking.getRoom().getLandlord().getId();
        SecurityUtils.requireOwnerOrAdmin(actor, landlordId, "respond to this booking");

        BookingStatus target = decision.status();
        if (target == BookingStatus.PENDING) {
            throw new BadRequestException("A booking cannot be moved back to PENDING");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking was cancelled by the tenant");
        }
        if (target == BookingStatus.CONFIRMED
                && bookingRepository.existsOverlappingConfirmedBookingExcluding(
                        booking.getRoom().getId(), booking.getId(),
                        booking.getCheckIn(), booking.getCheckOut())) {
            throw new ConflictException("Another confirmed booking already covers those dates");
        }

        booking.setStatus(target);
        booking.setLandlordResponse(decision.response());
        Booking saved = bookingRepository.save(booking);

        // A confirmed stay takes the listing off the market until the landlord re-opens it.
        if (target == BookingStatus.CONFIRMED) {
            Room room = saved.getRoom();
            if (Boolean.TRUE.equals(room.getAvailable())) {
                room.setAvailable(Boolean.FALSE);
                roomRepository.save(room);
                notificationService.availabilityChanged(
                        new com.accommodationfinder.dto.RoomDtos.AvailabilityEvent(
                                room.getId(), Boolean.FALSE, room.getTitle(), room.getCity()));
            }
        }

        BookingResponse response = bookingMapper.toResponse(saved);
        notificationService.bookingEvent(saved.getTenant().getEmail(), response);
        return response;
    }

    /** Tenant withdraws their own request. */
    @Transactional
    public BookingResponse cancel(Long bookingId, User actor) {
        Booking booking = getEntity(bookingId);
        boolean isTenant = booking.getTenant().getId().equals(actor.getId());
        if (!isTenant && !SecurityUtils.isAdmin(actor)) {
            throw new ForbiddenException("Only the tenant can cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("A completed stay cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        BookingResponse response = bookingMapper.toResponse(saved);
        notificationService.bookingEvent(saved.getRoom().getLandlord().getEmail(), response);
        return response;
    }

    // ---------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public Page<BookingResponse> myBookings(Long tenantId, Pageable pageable) {
        return bookingRepository.findByTenantIdOrderByRequestedAtDesc(tenantId, pageable)
                .map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> requestsForLandlord(Long landlordId, Pageable pageable) {
        return bookingRepository.findByRoomLandlordIdOrderByRequestedAtDesc(landlordId, pageable)
                .map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id, User actor) {
        Booking booking = getEntity(id);
        boolean allowed = booking.getTenant().getId().equals(actor.getId())
                || booking.getRoom().getLandlord().getId().equals(actor.getId())
                || SecurityUtils.isAdmin(actor);
        if (!allowed) {
            throw new ForbiddenException("You are not part of this booking");
        }
        return bookingMapper.toResponse(booking);
    }

    /** Dates already taken, so the frontend can grey them out on the calendar. */
    @Transactional(readOnly = true)
    public List<BookedRange> bookedRanges(Long roomId) {
        return bookingRepository.findByRoomIdAndStatusInOrderByCheckInAsc(roomId, BLOCKING_STATUSES)
                .stream()
                .map(booking -> new BookedRange(booking.getCheckIn(), booking.getCheckOut(), booking.getStatus()))
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private Booking getEntity(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out must be after check-in");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 730) {
            throw new BadRequestException("A stay cannot exceed 24 months");
        }
    }

    /** Monthly rent pro-rated over the length of the stay (30-day month). */
    private BigDecimal estimateTotal(BigDecimal monthlyPrice, LocalDate checkIn, LocalDate checkOut) {
        if (monthlyPrice == null) {
            return BigDecimal.ZERO;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return monthlyPrice
                .multiply(BigDecimal.valueOf(nights))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
    }
}

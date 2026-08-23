package com.accommodationfinder.service;

import com.accommodationfinder.dto.RoomDtos.*;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.RoomMapper;
import com.accommodationfinder.model.*;
import com.accommodationfinder.repository.*;
import com.accommodationfinder.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookingRepository bookingRepository;
    private final MessageRepository messageRepository;
    private final RoomMapper roomMapper;
    private final NotificationService notificationService;
    private final GeocodingService geocodingService;
    private final ScamDetectionService scamDetectionService;
    private final HousingAidService housingAidService;

    public RoomService(RoomRepository roomRepository,
                       ReviewRepository reviewRepository,
                       FavoriteRepository favoriteRepository,
                       BookingRepository bookingRepository,
                       MessageRepository messageRepository,
                       RoomMapper roomMapper,
                       NotificationService notificationService,
                       GeocodingService geocodingService,
                       ScamDetectionService scamDetectionService,
                       HousingAidService housingAidService) {
        this.roomRepository = roomRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.bookingRepository = bookingRepository;
        this.messageRepository = messageRepository;
        this.roomMapper = roomMapper;
        this.notificationService = notificationService;
        this.geocodingService = geocodingService;
        this.scamDetectionService = scamDetectionService;
        this.housingAidService = housingAidService;
    }

    // ---------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public Page<RoomResponse> search(RoomSearchCriteria criteria, Pageable pageable) {
        Page<Room> page = roomRepository.findAll(RoomSpecifications.fromCriteria(criteria), pageable);
        return decorate(page);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> findByLandlord(Long landlordId, Pageable pageable) {
        return decorate(roomRepository.findByLandlordId(landlordId, pageable));
    }

    @Transactional(readOnly = true)
    public RoomResponse findById(Long id) {
        Room room = getEntity(id);
        double average = reviewRepository.averageRatingForRoom(id);
        long count = reviewRepository.countByRoomId(id);
        Boolean favorite = SecurityUtils.currentUser()
                .map(user -> favoriteRepository.existsByUserIdAndRoomId(user.getId(), id))
                .orElse(null);
        return roomMapper.toResponse(room, average, count, favorite);
    }

    @Transactional(readOnly = true)
    public Room getEntity(Long id) {
        return roomRepository.findByIdWithLandlord(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    @Transactional(readOnly = true)
    public List<String> listCities() {
        return roomRepository.findDistinctCities();
    }

    // ---------------------------------------------------------------- commands

    @Transactional
    public RoomResponse create(RoomRequest request, User landlord) {
        Room room = new Room();
        roomMapper.apply(request, room);
        room.setLandlord(landlord);
        if (request.available() == null) {
            room.setAvailable(Boolean.TRUE);
        }
        enrich(room);
        Room saved = roomRepository.save(room);

        RoomResponse response = roomMapper.toResponse(saved, 0d, 0L, Boolean.FALSE);
        notificationService.roomPublished(response);
        return response;
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request, User actor) {
        Room room = getEntity(id);
        SecurityUtils.requireOwnerOrAdmin(actor, room.getLandlord().getId(), "edit this listing");

        boolean wasAvailable = Boolean.TRUE.equals(room.getAvailable());
        roomMapper.apply(request, room);
        enrich(room);
        Room saved = roomRepository.save(room);

        RoomResponse response = roomMapper.toResponse(saved,
                reviewRepository.averageRatingForRoom(id),
                reviewRepository.countByRoomId(id),
                null);

        notificationService.roomUpdated(response);
        if (wasAvailable != Boolean.TRUE.equals(saved.getAvailable())) {
            notificationService.availabilityChanged(new AvailabilityEvent(
                    saved.getId(), saved.getAvailable(), saved.getTitle(), saved.getCity()));
        }
        return response;
    }

    @Transactional
    public RoomResponse updateAvailability(Long id, boolean available, User actor) {
        Room room = getEntity(id);
        SecurityUtils.requireOwnerOrAdmin(actor, room.getLandlord().getId(), "change this listing");

        room.setAvailable(available);
        Room saved = roomRepository.save(room);

        notificationService.availabilityChanged(new AvailabilityEvent(
                saved.getId(), available, saved.getTitle(), saved.getCity()));

        return roomMapper.toResponse(saved,
                reviewRepository.averageRatingForRoom(id),
                reviewRepository.countByRoomId(id),
                null);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Room room = getEntity(id);
        SecurityUtils.requireOwnerOrAdmin(actor, room.getLandlord().getId(), "delete this listing");
        roomRepository.delete(room);
        notificationService.availabilityChanged(new AvailabilityEvent(id, false, room.getTitle(), room.getCity()));
    }

    // ---------------------------------------------------------------- stats

    @Transactional(readOnly = true)
    public LandlordStats landlordStats(Long landlordId) {
        return new LandlordStats(
                roomRepository.countByLandlordId(landlordId),
                roomRepository.countByLandlordIdAndAvailableTrue(landlordId),
                bookingRepository.countByRoomLandlordIdAndStatus(landlordId, BookingStatus.PENDING),
                bookingRepository.countByRoomLandlordIdAndStatus(landlordId, BookingStatus.CONFIRMED),
                messageRepository.countByRecipientIdAndReadFalse(landlordId));
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Fills in everything the landlord should not have to work out themselves.
     *
     * <p>Nobody knows their flat's latitude. Asking for it guarantees either a blank field or
     * a wrong one, and without coordinates the listing never appears on the map and we cannot
     * tell a student how far it is from campus. So we look the address up instead, and while
     * we are there we record the INSEE code and the CAF rent-ceiling zone, which is what the
     * housing-aid estimate depends on.
     *
     * <p>Then we score it for the scam patterns that target newcomers. Doing it on save means
     * a moderator sees a flagged listing straight away rather than after somebody is defrauded.
     */
    private void enrich(Room room) {
        boolean missingCoordinates = room.getLatitude() == null || room.getLongitude() == null;

        if (missingCoordinates && room.getAddress() != null && !room.getAddress().isBlank()) {
            geocodingService.geocode(room.getAddress(), room.getPostalCode(), room.getCity())
                    .ifPresent(result -> {
                        room.setLatitude(result.latitude());
                        room.setLongitude(result.longitude());
                        room.setInseeCode(result.inseeCode());
                        if (room.getPostalCode() == null || room.getPostalCode().isBlank()) {
                            room.setPostalCode(result.postcode());
                        }
                    });
        }

        room.setHousingZone(housingAidService.zoneForPostcode(room.getPostalCode()));
        scamDetectionService.rescore(room);
    }

    /**
     * Adds ratings and the caller's favourite flags to a page of rooms using
     * two extra queries in total rather than two per row.
     */
    private Page<RoomResponse> decorate(Page<Room> page) {
        List<Long> ids = page.getContent().stream().map(Room::getId).toList();
        if (ids.isEmpty()) {
            return page.map(room -> roomMapper.toResponse(room, 0d, 0L, null));
        }

        Map<Long, double[]> ratings = new HashMap<>();
        for (Object[] row : reviewRepository.aggregateRatings(ids)) {
            Long roomId = ((Number) row[0]).longValue();
            double average = row[1] == null ? 0d : ((Number) row[1]).doubleValue();
            double count = row[2] == null ? 0d : ((Number) row[2]).doubleValue();
            ratings.put(roomId, new double[]{average, count});
        }

        Set<Long> favorites = SecurityUtils.currentUser()
                .map(user -> new HashSet<>(favoriteRepository.findRoomIdsByUserId(user.getId())))
                .orElseGet(HashSet::new);
        boolean authenticated = SecurityUtils.currentUser().isPresent();

        return page.map(room -> {
            double[] rating = ratings.getOrDefault(room.getId(), new double[]{0d, 0d});
            return roomMapper.toResponse(room, rating[0], (long) rating[1],
                    authenticated ? favorites.contains(room.getId()) : null);
        });
    }
}

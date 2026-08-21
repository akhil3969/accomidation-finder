package com.accommodationfinder.service;

import com.accommodationfinder.dto.ReviewDtos.ReviewRequest;
import com.accommodationfinder.dto.ReviewDtos.ReviewResponse;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.ReviewMapper;
import com.accommodationfinder.model.BookingStatus;
import com.accommodationfinder.model.Review;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.BookingRepository;
import com.accommodationfinder.repository.ReviewRepository;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepository reviewRepository,
                         RoomRepository roomRepository,
                         BookingRepository bookingRepository,
                         ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> forRoom(Long roomId) {
        return reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse create(Long roomId, ReviewRequest request, User author) {
        Room room = roomRepository.findByIdWithLandlord(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        if (room.getLandlord().getId().equals(author.getId())) {
            throw new BadRequestException("You cannot review your own listing");
        }
        if (reviewRepository.existsByRoomIdAndAuthorId(roomId, author.getId())) {
            throw new ConflictException("You have already reviewed this listing");
        }
        // Only people who actually stayed there may review.
        boolean stayed = bookingRepository.existsByRoomIdAndTenantIdAndStatus(roomId, author.getId(), BookingStatus.COMPLETED)
                || bookingRepository.existsByRoomIdAndTenantIdAndStatus(roomId, author.getId(), BookingStatus.CONFIRMED);
        if (!stayed) {
            throw new BadRequestException("Only tenants with a confirmed stay can review this listing");
        }

        Review review = new Review();
        review.setRoom(room);
        review.setAuthor(author);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Transactional
    public void delete(Long reviewId, User actor) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        SecurityUtils.requireOwnerOrAdmin(actor, review.getAuthor().getId(), "delete this review");
        reviewRepository.delete(review);
    }
}

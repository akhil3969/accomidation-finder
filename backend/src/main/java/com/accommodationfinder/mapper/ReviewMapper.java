package com.accommodationfinder.mapper;

import com.accommodationfinder.dto.ReviewDtos.ReviewResponse;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.model.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(
                review.getId(),
                review.getRoom() == null ? null : review.getRoom().getId(),
                UserDtos.UserSummary.publicProfile(review.getAuthor()),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}

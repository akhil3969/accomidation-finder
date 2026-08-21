package com.accommodationfinder.mapper;

import com.accommodationfinder.dto.BookingDtos.BookingResponse;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.model.Booking;
import com.accommodationfinder.model.Room;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }
        Room room = booking.getRoom();
        String cover = (room != null && room.getImages() != null && !room.getImages().isEmpty())
                ? room.getImages().get(0)
                : null;

        return new BookingResponse(
                booking.getId(),
                room == null ? null : room.getId(),
                room == null ? null : room.getTitle(),
                room == null ? null : room.getCity(),
                cover,
                room == null ? null : room.getPrice(),
                UserDtos.UserSummary.publicProfile(booking.getTenant()),
                room == null ? null : UserDtos.UserSummary.publicProfile(room.getLandlord()),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getGuests(),
                booking.getMessage(),
                booking.getLandlordResponse(),
                booking.getStatus(),
                booking.getTotalPrice(),
                booking.getRequestedAt(),
                booking.getUpdatedAt());
    }
}

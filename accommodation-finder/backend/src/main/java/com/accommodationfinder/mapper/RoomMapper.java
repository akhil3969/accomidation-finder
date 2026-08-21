package com.accommodationfinder.mapper;

import com.accommodationfinder.dto.RoomDtos.RoomRequest;
import com.accommodationfinder.dto.RoomDtos.RoomResponse;
import com.accommodationfinder.dto.UserDtos;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.VerificationStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/** Entity <-> DTO translation for listings. */
@Component
public class RoomMapper {

    public RoomResponse toResponse(Room room, Double averageRating, Long reviewCount, Boolean favorite) {
        if (room == null) {
            return null;
        }
        return new RoomResponse(
                room.getId(),
                room.getTitle(),
                room.getDescription(),
                room.getCity(),
                room.getAddress(),
                room.getPostalCode(),
                room.getCountry(),
                room.getPrice(),
                room.getDeposit(),
                room.getSizeSqm(),
                room.getBedrooms(),
                room.getBathrooms(),
                room.getMaxGuests(),
                room.getAvailable(),
                room.getFurnished(),
                room.getBillsIncluded(),
                room.getAvailableFrom(),
                room.getMinStayMonths(),
                room.getRoomType(),
                room.getLatitude(),
                room.getLongitude(),
                new ArrayList<>(room.getImages()),
                new LinkedHashSet<>(room.getAmenities()),
                UserDtos.UserSummary.publicProfile(room.getLandlord()),
                averageRating == null ? 0d : Math.round(averageRating * 10) / 10d,
                reviewCount == null ? 0L : reviewCount,
                favorite,
                room.getChargesAmount(),
                room.totalMonthlyCost(),
                room.getHousingZone(),
                room.isVerified(),
                room.getLandlord() != null
                        && room.getLandlord().getVerificationStatus() == VerificationStatus.VERIFIED,
                room.getRiskScore(),
                riskLevel(room.getRiskScore()),
                room.getCreatedAt(),
                room.getUpdatedAt());
    }

    public RoomResponse toResponse(Room room) {
        return toResponse(room, 0d, 0L, null);
    }

    /** Turns the numeric score into the word the UI shows next to the listing. */
    private String riskLevel(Integer score) {
        int value = score == null ? 0 : score;
        if (value >= 55) {
            return "high";
        }
        if (value >= 25) {
            return "caution";
        }
        return "normal";
    }

    /** Applies a create/update payload onto an entity. Null fields keep their previous value. */
    public void apply(RoomRequest request, Room room) {
        room.setTitle(request.title());
        room.setDescription(request.description());
        room.setCity(request.city());
        room.setAddress(request.address());
        room.setPostalCode(request.postalCode());
        if (request.country() != null && !request.country().isBlank()) {
            room.setCountry(request.country());
        }
        room.setPrice(request.price());
        room.setDeposit(request.deposit());
        room.setChargesAmount(request.chargesAmount());
        room.setSizeSqm(request.sizeSqm());
        if (request.bedrooms() != null) {
            room.setBedrooms(request.bedrooms());
        }
        if (request.bathrooms() != null) {
            room.setBathrooms(request.bathrooms());
        }
        if (request.maxGuests() != null) {
            room.setMaxGuests(request.maxGuests());
        }
        if (request.available() != null) {
            room.setAvailable(request.available());
        }
        if (request.furnished() != null) {
            room.setFurnished(request.furnished());
        }
        if (request.billsIncluded() != null) {
            room.setBillsIncluded(request.billsIncluded());
        }
        if (request.availableFrom() != null) {
            room.setAvailableFrom(request.availableFrom());
        }
        if (request.minStayMonths() != null) {
            room.setMinStayMonths(request.minStayMonths());
        }
        room.setRoomType(request.roomType());
        room.setLatitude(request.latitude());
        room.setLongitude(request.longitude());
        if (request.images() != null) {
            room.setImages(request.images());
        }
        if (request.amenities() != null) {
            room.setAmenities(request.amenities());
        }
    }
}

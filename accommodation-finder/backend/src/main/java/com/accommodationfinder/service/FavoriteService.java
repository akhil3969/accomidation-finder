package com.accommodationfinder.service;

import com.accommodationfinder.dto.RoomDtos.RoomResponse;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.mapper.RoomMapper;
import com.accommodationfinder.model.Favorite;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.FavoriteRepository;
import com.accommodationfinder.repository.ReviewRepository;
import com.accommodationfinder.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final ReviewRepository reviewRepository;
    private final RoomMapper roomMapper;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           RoomRepository roomRepository,
                           ReviewRepository reviewRepository,
                           RoomMapper roomMapper) {
        this.favoriteRepository = favoriteRepository;
        this.roomRepository = roomRepository;
        this.reviewRepository = reviewRepository;
        this.roomMapper = roomMapper;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(User user) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(Favorite::getRoom)
                .map(room -> roomMapper.toResponse(room,
                        reviewRepository.averageRatingForRoom(room.getId()),
                        reviewRepository.countByRoomId(room.getId()),
                        Boolean.TRUE))
                .toList();
    }

    /** Adds the listing if missing, removes it if present. Returns the new state. */
    @Transactional
    public boolean toggle(Long roomId, User user) {
        return favoriteRepository.findByUserIdAndRoomId(user.getId(), roomId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    Room room = roomRepository.findById(roomId)
                            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
                    favoriteRepository.save(new Favorite(user, room));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public List<Long> favoriteRoomIds(User user) {
        return favoriteRepository.findRoomIdsByUserId(user.getId());
    }
}

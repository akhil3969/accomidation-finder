package com.accommodationfinder.controller;

import com.accommodationfinder.dto.RoomDtos.RoomResponse;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Favorites", description = "Listings saved by the signed-in user")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "Saved listings")
    public List<RoomResponse> list() {
        return favoriteService.list(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/ids")
    @Operation(summary = "Just the ids - lets the grid render heart icons in one request")
    public List<Long> ids() {
        return favoriteService.favoriteRoomIds(SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{roomId}/toggle")
    @Operation(summary = "Save or unsave a listing")
    public Map<String, Object> toggle(@PathVariable Long roomId) {
        boolean favorite = favoriteService.toggle(roomId, SecurityUtils.requireCurrentUser());
        return Map.of("roomId", roomId, "favorite", favorite);
    }
}

package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.FavoriteDTOs.*;
import com.example.demo.model.ItemType;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * Obtiene todos los favoritos del veterinario actual
     */
    @GetMapping
    @PreAuthorize("hasPermission('', 'GESTIONAR_FAVORITOS')")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getFavorites() {
        return ResponseEntity.ok(ApiResponse.success(
                favoriteService.getCurrentUserFavorites()));
    }

    /**
     * Alterna el estado de favorito de un item
     */
    @PostMapping("/toggle")
    @PreAuthorize("hasPermission('', 'GESTIONAR_FAVORITOS')")
    public ResponseEntity<ApiResponse<FavoriteResponse>> toggleFavorite(
            @RequestBody ToggleFavoriteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                favoriteService.toggleFavorite(request.getItemId(), ItemType.valueOf(request.getItemType()))));
    }

    /**
     * Verifica si un item está marcado como favorito
     */
    @GetMapping("/check")
    @PreAuthorize("hasPermission('', 'GESTIONAR_FAVORITOS')")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @RequestParam String itemId,
            @RequestParam String itemType) {
        return ResponseEntity.ok(ApiResponse.success(
                favoriteService.isFavorite(itemId, itemType)));
    }
}
package com.example.demo.dto;

import lombok.*;
import java.util.Date;

public class FavoriteDTOs {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteResponse {
        private String id;
        private String itemId;
        private String itemType;
        private String itemName;  // Nombre del servicio o producto
        private double price;
        private Date createdAt;
        private Integer quantity;    // null para servicios
        private Integer minThreshold; // null para servicios
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleFavoriteRequest {
        private String itemId;
        private String itemType;
    }
}

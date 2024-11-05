package com.example.demo.dto;

import com.example.demo.model.AlertStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

public class InventoryDTOs {

    @Data
    public static class AddInventoryItemRequest {
        private String name;
        private int quantity;
        private int minThreshold;
    }

    @Data
    public static class UpdateInventoryItemRequest {
        private int quantity;
        private int minThreshold;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InventoryItemResponse {
        private String id;
        private String name;
        private int quantity;
        private int minThreshold;
        private int recommendedOrderQuantity;
        private Date dateAdded;
        private Date lastUpdated;
        private String status; // Puede ser "OK", "WARNING", "CRITICAL"
        private boolean needsReorder;
    }
    @Data
    @Builder
    public static class LowStockAlertDTO {
        private String id;
        private String productId;
        private String productName;
        private int currentStock;
        private int minThreshold;
        private boolean isAcknowledged;
        private Date createdAt;
        private AlertStatus status; // CRITICAL, WARNING
    }

    @Data
    @Builder
    public static class RestockOrderDTO {
        private String id;
        private String productId;
        private String productName;
        private int currentStock;
        private int quantityToOrder;
        private OrderStatus status;
        private Date orderDate;
        private String requestedBy;
        private String notes;
    }

    @Data
    public static class UpdateThresholdRequest {
        @Min(value = 0, message = "El umbral mínimo no puede ser negativo")
        private int minThreshold;
        @Min(value = 0, message = "La cantidad recomendada de pedido no puede ser negativa")
        private Integer recommendedOrderQuantity;
    }
    public static enum OrderStatus {
        PENDING, APPROVED, COMPLETED, CANCELLED
    }
    @Data
    public static class CreateRestockOrderRequest {

        private String productId;
        @Min(1)
        private int quantity;
        private String notes;
    }



}
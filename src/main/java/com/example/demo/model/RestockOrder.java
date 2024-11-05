package com.example.demo.model;

import com.example.demo.dto.InventoryDTOs;
import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockOrder {
    private String id;
    private String productId;
    private String productName;
    private int currentStock;
    private int quantityToOrder;
    private InventoryDTOs.OrderStatus status;
    private Date orderDate;
    private String requestedBy;
    private String notes;
    private Date completedDate;
    private String completedBy;
}
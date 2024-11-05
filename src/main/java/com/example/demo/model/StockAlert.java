package com.example.demo.model;

import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlert {
    private String id;
    private String productId;
    private String productName;
    private int currentStock;
    private int minThreshold;
    private AlertStatus status;
    private Date createdAt;
    private Date acknowledgedAt;
    private String acknowledgedBy;
    private boolean isActive;
}
package com.example.demo.model;

import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlert {
    private String id;
    private String productId;
    private String productName;
    private int currentStock;
    private int minThreshold;
    private AlertStatus status;
    private boolean isAcknowledged;
    private Date createdAt;
    private String acknowledgedBy;
    private Date acknowledgedAt;
}
package com.example.demo.model;

import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {
    private String id;
    private String productId;
    private int quantity;
    private RequestStatus status;
    private Date requestDate;
    private String requestedBy;
    private String notes;
    private Date completedDate;
    private String completedBy;
}
package com.example.demo.model;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVeterinary {
    private String id;
    private String name;
    private String description;
    private double price;
    private int durationMinutes;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
}
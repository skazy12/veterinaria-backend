package com.example.demo.model;

import lombok.*;
import java.util.Date;
import java.util.List;

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
    private List<String> requirements;
    private List<String> recommendations;
    private List<String> warnings;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
}
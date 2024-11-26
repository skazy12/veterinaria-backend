package com.example.demo.model;


import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    private String id;
    private String userId;      // ID del veterinario
    private String itemId;      // ID del servicio o producto
    private String itemType;    // "SERVICE" o "PRODUCT"
    private Date createdAt;
    private Date updatedAt;
}
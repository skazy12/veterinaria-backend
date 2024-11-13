package com.example.demo.dto;

import com.example.demo.model.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private String id;
    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String name;
    @NotNull(message = "La categoría del servicio es obligatoria")
    private ServiceCategory category;
    private String description;
    private double price;
    private boolean isActive;
}
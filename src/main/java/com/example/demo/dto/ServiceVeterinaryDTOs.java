package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;

public class ServiceVeterinaryDTOs {

    @Data
    @Builder
    public static class CreateServiceRequest {
        @NotBlank(message = "El nombre del servicio es obligatorio")
        private String name;

        @NotBlank(message = "La descripción del servicio es obligatoria")
        @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
        private String description;

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio no puede ser negativo")
        private Double price;

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
        private Integer durationMinutes;
    }

    @Data
    @Builder
    public static class UpdateServiceRequest {
        @NotBlank(message = "El nombre del servicio es obligatorio")
        private String name;

        @NotBlank(message = "La descripción del servicio es obligatoria")
        @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
        private String description;

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio no puede ser negativo")
        private Double price;

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
        private Integer durationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceResponse {
        private String id;
        private String name;
        private String description;
        private double price;
        private int durationMinutes;
        private boolean isActive;
        private Date createdAt;
        private Date updatedAt;
    }
}
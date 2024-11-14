package com.example.demo.dto;

import com.example.demo.model.ServiceCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;
import java.util.List;

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

        private List<String> requirements;
        private List<String> recommendations;
        private List<String> warnings;
        private ServiceCategory category;
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

        private List<String> requirements;
        private List<String> recommendations;
        private List<String> warnings;
        private ServiceCategory category;

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
        private ServiceCategory category;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceDetailResponse {
        private String id;
        private String name;
        private String description;
        private double price;
        private int durationMinutes;
        private List<String> requirements;  // Lista de requisitos (ej: "ayuno previo")
        private List<String> recommendations; // Recomendaciones para el servicio
        private List<String> warnings;
        private ServiceCategory category;
        private boolean isActive;
        private Date createdAt;
        private Date updatedAt;
    }
    @Data
    @Builder
    public static class ServiceDetailRequest {
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

        private List<String> requirements;
        private List<String> recommendations;
        private List<String> warnings;
        private ServiceCategory category;
    }
}
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.Date;

public class HistorialClinicoDTOs {

    @Data
    @Builder
    public static class CreateHistorialRequest {
        @NotBlank(message = "El motivo de consulta es obligatorio")
        private String motivoConsulta;

        @NotBlank(message = "El diagnóstico es obligatorio")
        private String diagnostico;

        @NotBlank(message = "El tratamiento es obligatorio")
        private String tratamiento;

        private String observaciones;
    }

    @Data
    @Builder
    public static class UpdateHistorialRequest {
        @NotBlank(message = "El motivo de consulta es obligatorio")
        private String motivoConsulta;

        @NotBlank(message = "El diagnóstico es obligatorio")
        private String diagnostico;

        @NotBlank(message = "El tratamiento es obligatorio")
        private String tratamiento;

        private String observaciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistorialClinicoResponse {
        private String id;
        private String petId;
        private String veterinarianId;
        private String veterinarianName;  // Nombre completo del veterinario
        private String petName;           // Nombre de la mascota
        private String ownerName;         // Nombre del dueño
        private Date fechaVisita;
        private String motivoConsulta;
        private String diagnostico;
        private String tratamiento;
        private String observaciones;
        private Date fechaCreacion;
        private Date fechaActualizacion;
        private String estado;
    }
}

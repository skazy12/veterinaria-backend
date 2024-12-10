package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAppointmentDTO {
    private String id;
    private Date appointmentTime;
    // Información del cliente
    private String clientId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    // Información de la mascota
    private String petId;
    private String petName;
    private String petSpecies;
    private String petBreed;
    // Información del veterinario
    private String veterinarianId;
    private String veterinarianName;
    private String status;
    private String reason;
    private boolean canCancel;
    private boolean canReschedule;
}

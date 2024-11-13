package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

public class AppointmentDTOs {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppointmentResponse {
        private String id;
        private PetDTOs.PetResponse pet;
        private UserDTOs.UserResponse client;
        private UserDTOs.UserResponse veterinarian;
        private Date appointmentDate;
        private String reason;
        private String status;
        private String notes;
        private List<PetDTOs.MedicalRecordResponse> petHistory;
    }

    @Data
    public static class CreateAppointmentRequest {
        private String petId;
        private String clientId;
        private String veterinarianId;
        private Date appointmentDate;
        private String reason;
        private String notes;
    }

    @Data
    public static class UpdateAppointmentRequest {
        private Date appointmentDate;
        private String reason;
        private String status;
        private String notes;
    }

    @Data
    @AllArgsConstructor
    public static class AppointmentSummary {
        private List<AppointmentResponse> appointments;
        private int totalAppointments;
        private Date date;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppointmentSummaryByPet {
        private PetDTOs.PetResponse pet;
        private List<AppointmentResponse> appointments;
        private int totalAppointments;
    }

    @Data
    public static class RescheduleRequest {
        @NotNull(message = "La nueva fecha es obligatoria")
        private Date newDate;
        private String reason;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClientPetAppointmentDTO {
        private String id;
        private Date appointmentDate;
        private String reason;
        private String status;
        private String veterinarianName;
        private PetDTOs.PetResponse pet;
        private boolean canCancel;    // Para indicar si la cita puede ser cancelada
        private boolean canReschedule; // Para indicar si la cita puede ser reprogramada
    }

}
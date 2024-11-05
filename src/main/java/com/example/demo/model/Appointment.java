package com.example.demo.model;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private String id;
    private String petId;
    private String clientId;
    private String veterinarianId;
    private Date appointmentDate;
    private String reason;
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    private String notes;
    private Date createdAt;
    private Date updatedAt;
}
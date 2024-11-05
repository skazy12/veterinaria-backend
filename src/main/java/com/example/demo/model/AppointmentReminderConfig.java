package com.example.demo.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReminderConfig {
    private String id;
    private int reminderHoursBefore;  // Horas antes de la cita para enviar recordatorio
    private boolean isEnabled;
    private String emailTemplate;
    private String confirmationLink;
}
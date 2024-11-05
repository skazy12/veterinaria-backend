package com.example.demo.dto;

import lombok.*;

@Data
@Builder
public class ReminderDTO {
    private int reminderHoursBefore;
    private String emailTemplate;
    private boolean isEnabled;
}
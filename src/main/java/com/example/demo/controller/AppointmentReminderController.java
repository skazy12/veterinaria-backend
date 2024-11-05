package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ReminderDTO;
import com.example.demo.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reminders")
public class AppointmentReminderController {

    @Autowired
    private ReminderService reminderService;

    @PutMapping("/config")
    @PreAuthorize("hasPermission('', 'GESTIONAR_RECORDATORIOS')")
    public ResponseEntity<ApiResponse<Void>> updateReminderConfig(@RequestBody ReminderDTO config) {
        reminderService.updateReminderConfig(config);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAppointment(
            @RequestParam String id,
            @RequestParam String token) {
        reminderService.confirmAppointment(id, token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
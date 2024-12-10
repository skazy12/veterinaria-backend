package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AppointmentDTOs;
import com.example.demo.dto.AppointmentDTOs.*;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.service.AppointmentService;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/daily")
    @PreAuthorize("hasPermission(null, 'VER_CITAS_DIARIAS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<AppointmentResponse>>> getDailyAppointments(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date,
            @RequestParam String veterinarianId) {
        try {
            log.info("Fetching daily appointments for date: {}, veterinarianId: {}", date, veterinarianId);
            if (date == null) {
                date = new Date();
            }
            return ResponseEntity.ok(ApiResponse.success(
                    appointmentService.getVeterinarianDailyAppointments(veterinarianId, date, paginationRequest)));
        } catch (Exception e) {
            log.error("Error fetching daily appointments: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Error fetching daily appointments."));
        }
    }

    @GetMapping("/my-pets")
    @PreAuthorize("hasPermission('', 'VER_CITAS_MASCOTAS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<AppointmentSummaryByPet>>> getClientPetsAppointments(
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getClientPetsAppointments(paginationRequest)));
    }
    @PostMapping("/schedule")
    @PreAuthorize("hasPermission(null, 'PROGRAMAR_CITA')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> scheduleAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.createAppointment(request)
        ));
    }

    @PostMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasPermission(null, 'REPROGRAMAR_CITA') and @appointmentService.isOwner(#appointmentId)")
    public ResponseEntity<ApiResponse<AppointmentDTOs.AppointmentResponse>> rescheduleAppointment(
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDTOs.RescheduleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.rescheduleAppointment(appointmentId, request)
        ));
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasPermission(null, 'PROGRAMAR_CITA') or @appointmentService.isOwner(#appointmentId)")
    public ResponseEntity<ApiResponse<AppointmentDTOs.AppointmentResponse>> cancelAppointment(
            @PathVariable String appointmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.cancelAppointment(appointmentId)
        ));
    }
}
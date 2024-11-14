package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AppointmentDTOs;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.service.ClientAppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// controller/ClientAppointmentController.java
@RestController
@RequestMapping("/api/client/appointments")
public class ClientAppointmentController {

    @Autowired
    private ClientAppointmentService clientAppointmentService;

    // Obtener todas las citas del cliente actual
    @GetMapping
    @PreAuthorize("hasPermission(null, 'VER_CITAS_DE_MASCOTAS_MIAS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<AppointmentDTOs.ClientPetAppointmentDTO>>> getMyAppointments(
            @ModelAttribute PaginationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientAppointmentService.getClientAppointments(request)));
    }

    // Cancelar una cita
    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasPermission(null, 'CANCELAR_MI_CITA')")
    public ResponseEntity<ApiResponse<AppointmentDTOs.ClientPetAppointmentDTO>> cancelAppointment(
            @PathVariable String appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                clientAppointmentService.cancelAppointment(appointmentId)));
    }

    // Reprogramar una cita
    @PostMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasPermission(null, 'REPROGRAMAR_MI_CITA')")
    public ResponseEntity<ApiResponse<AppointmentDTOs.ClientPetAppointmentDTO>> rescheduleAppointment(
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDTOs.RescheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientAppointmentService.rescheduleAppointment(appointmentId, request)));
    }
}
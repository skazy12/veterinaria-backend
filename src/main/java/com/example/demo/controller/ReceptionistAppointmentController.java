package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DailyAppointmentDTO;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/receptionist/appointments")
public class ReceptionistAppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/daily")
    @PreAuthorize("hasPermission('', 'VER_CITAS_DIARIAS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<DailyAppointmentDTO>>> getDailyAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        PaginationRequest request = new PaginationRequest();
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("appointmentDate");
        request.setSortDirection("ASC");

        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getDailyAppointmentsForReceptionist(date, request)
        ));
    }
}
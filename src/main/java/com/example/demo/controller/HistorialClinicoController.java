package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.HistorialClinicoDTOs.*;
import com.example.demo.service.HistorialClinicoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial-clinico")
public class HistorialClinicoController {

    @Autowired
    private HistorialClinicoService historialClinicoService;

    @PostMapping("/mascota/{petId}")
    @PreAuthorize("hasPermission('', 'CREAR_HISTORIAL_CLINICO')")
    public ResponseEntity<ApiResponse<HistorialClinicoResponse>> createHistorial(
            @PathVariable String petId,
            @Valid @RequestBody CreateHistorialRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                historialClinicoService.createHistorial(petId, request)));
    }

    @PutMapping("/{historialId}")
    @PreAuthorize("hasPermission('', 'ACTUALIZAR_HISTORIAL_CLINICO')")
    public ResponseEntity<ApiResponse<HistorialClinicoResponse>> updateHistorial(
            @PathVariable String historialId,
            @Valid @RequestBody UpdateHistorialRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                historialClinicoService.updateHistorial(historialId, request)));
    }

    @GetMapping("/mascota/{petId}")
    @PreAuthorize("hasPermission('', 'VER_HISTORIAL_CLINICO')")
    public ResponseEntity<ApiResponse<List<HistorialClinicoResponse>>> getHistorialByPetId(
            @PathVariable String petId) {
        return ResponseEntity.ok(ApiResponse.success(
                historialClinicoService.getHistorialByPetId(petId)));
    }

    @GetMapping("/{historialId}")
    @PreAuthorize("hasPermission('', 'VER_HISTORIAL_CLINICO')")
    public ResponseEntity<ApiResponse<HistorialClinicoResponse>> getHistorialById(
            @PathVariable String historialId) {
        return ResponseEntity.ok(ApiResponse.success(
                historialClinicoService.getHistorialById(historialId)));
    }
}
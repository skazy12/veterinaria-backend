package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.VeterinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/veterinary")
public class VeterinaryController {

    @Autowired
    private VeterinaryService veterinaryService;

    @GetMapping("/search")
    @PreAuthorize("hasPermission('', 'BUSCAR_CLIENTE')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDTOs.ClientWithPetsDTO>>> searchClients(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String petName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate consultationDate) {

        UserDTOs.ClientSearchCriteria criteria = new UserDTOs.ClientSearchCriteria(clientName, petName, consultationDate);
        return ResponseEntity.ok(ApiResponse.success(
                veterinaryService.searchClients(criteria, paginationRequest)));
    }

    @GetMapping("/clients/{clientId}/pets")
    @PreAuthorize("hasPermission(null, 'BUSCAR_CLIENTE_MASCOTA')")
    public ResponseEntity<ApiResponse<UserDTOs.ClientWithPetsDTO>> getClientWithPets(@PathVariable String clientId) {
        return ResponseEntity.ok(ApiResponse.success(veterinaryService.getClientWithPets(clientId)));
    }

    @PostMapping("/pets/{petId}/medical-records")
    @PreAuthorize("hasPermission(null, 'AGREGAR_MEDICAL_RECORD')")
    public ResponseEntity<ApiResponse<PetDTOs.MedicalRecordResponse>> addMedicalRecord(
            @PathVariable String petId,
            @RequestBody PetDTOs.AddMedicalRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(veterinaryService.addMedicalRecord(petId, request)));
    }

    @GetMapping("/pets/{petId}/medical-records")
    @PreAuthorize("hasPermission(null, 'OBTENER_MEDICAL_HISTORY')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PetDTOs.MedicalRecordResponse>>> getMedicalHistory(
            @PathVariable String petId,
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                veterinaryService.getMedicalHistory(petId, paginationRequest)));
    }
}
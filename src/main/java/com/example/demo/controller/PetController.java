package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.PetDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    @Autowired
    private PetService petService;
    @GetMapping("/me")
    @PreAuthorize("hasPermission('', 'VER_MIS_MASCOTAS')")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getCurrentUserPets() {
        return ResponseEntity.ok(ApiResponse.success(petService.getCurrentUserPets()));
    }

    @PostMapping
    @PreAuthorize("hasPermission('', 'AGREGAR_MASCOTA')")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(@RequestBody CreatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.createPet(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('', 'EDITAR_MASCOTA') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(@PathVariable String id, @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.updatePet(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('', 'VER_MASCOTA_XID') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<PetResponse>> getPetById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPetById(id)));
    }

    @GetMapping("/{id}/medical-history")
    @PreAuthorize("hasPermission('', 'VER_HISTORIAL_MEDICO') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<PaginatedResponse<MedicalRecordResponse>>> getPetMedicalHistory(
            @PathVariable String id,
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPetMedicalHistory(id, paginationRequest)));
    }

    @PostMapping("/{id}/medical-record")
    @PreAuthorize("hasPermission('', 'AGREGAR_HISTORIAL_MEDICO')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> addMedicalRecord(@PathVariable String id, @RequestBody AddMedicalRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.addMedicalRecord(id, request)));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('', 'ELIMINAR_MASCOTA') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<Void>> deletePet(@PathVariable String id) {
        try {
            petService.deletePet(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (CustomExceptions.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("PET_NOT_FOUND", e.getMessage()));
        } catch (CustomExceptions.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
        } catch (CustomExceptions.ProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }
    @GetMapping
    @PreAuthorize("hasPermission('', 'VER_TODAS_LAS_MASCOTAS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PetResponse>>> getAllPets(
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(petService.getAllPets(paginationRequest)));
    }
    @GetMapping("/user/{userId}/pets")
    @PreAuthorize("hasPermission('', 'VER_MASCOTAS_USUARIO')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PetResponse>>> getPetsByUserId(
            @PathVariable String userId,
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPetsByUserId(userId, paginationRequest)));
    }
}
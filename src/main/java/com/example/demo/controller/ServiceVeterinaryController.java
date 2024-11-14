package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.ServiceVeterinaryDTOs.*;
import com.example.demo.model.ServiceCategory;
import com.example.demo.service.ServiceVeterinaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceVeterinaryController {

    @Autowired
    private ServiceVeterinaryService serviceVeterinaryService;

    @GetMapping
    @PreAuthorize("hasPermission('', 'VER_SERVICIOS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ServiceResponse>>> getAllServices(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) ServiceCategory category) {

        // Si se proporciona una categoría, configurar el filtro
        if (category != null) {
            paginationRequest.setFilterBy("category");
            paginationRequest.setFilterValue(category.name());
        }

        return ResponseEntity.ok(ApiResponse.success(
                serviceVeterinaryService.getAllServices(paginationRequest)));
    }

    @PostMapping
    @PreAuthorize("hasPermission('', 'GESTIONAR_SERVICIOS')")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceVeterinaryService.createService(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('', 'GESTIONAR_SERVICIOS')")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable String id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceVeterinaryService.updateService(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('', 'GESTIONAR_SERVICIOS')")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable String id) {
        serviceVeterinaryService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasPermission('', 'GESTIONAR_SERVICIOS')")
    public ResponseEntity<ApiResponse<ServiceResponse>> toggleServiceStatus(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceVeterinaryService.toggleServiceStatus(id)));
    }
}

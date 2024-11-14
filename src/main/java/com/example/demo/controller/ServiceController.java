package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.ServiceCategory;
import com.example.demo.service.VetServiceListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private VetServiceListService serviceListService;

    /**
     * Obtiene la lista de servicios, opcionalmente filtrada y paginada
     */
    @GetMapping("/list")
    @PreAuthorize("hasPermission('', 'VER_SERVICIOS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ServiceDTO>>> getServiceList(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) ServiceCategory category) {

        ServiceSearchRequest searchRequest = ServiceSearchRequest.builder()
                .searchTerm(searchTerm)
                .category(category != null ? category.name() : null)
                .onlyActive(true) // Por defecto solo mostramos servicios activos
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                serviceListService.getServiceList(searchRequest, paginationRequest)));
    }

    /**
     * Obtiene la lista de servicios agrupados por categoría
     */
    @GetMapping("/by-category")
    @PreAuthorize("hasPermission('', 'VER_SERVICIOS')")
    public ResponseEntity<ApiResponse<ServiceListResponse>> getServicesByCategory(
            @RequestParam(required = false) String searchTerm) {

        ServiceSearchRequest searchRequest = ServiceSearchRequest.builder()
                .searchTerm(searchTerm)
                .onlyActive(true)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                serviceListService.getServicesByCategory(searchRequest)));
    }

    /**
     * Obtiene todas las categorías de servicios disponibles
     */
    @GetMapping("/categories")
    @PreAuthorize("hasPermission('', 'VER_SERVICIOS')")
    public ResponseEntity<ApiResponse<ServiceCategory[]>> getServiceCategories() {
        return ResponseEntity.ok(ApiResponse.success(ServiceCategory.values()));
    }
}

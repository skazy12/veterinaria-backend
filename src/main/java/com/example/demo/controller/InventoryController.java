package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.InventoryDTOs.*;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryItemResponse>>> getAllItems(
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getAllItems(paginationRequest)));
    }
    @GetMapping("/search")
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryItemResponse>>> searchInventory(
            @RequestParam String searchTerm,
            @ModelAttribute PaginationRequest paginationRequest) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    inventoryService.searchByName(searchTerm, paginationRequest)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<PaginatedResponse<LowStockAlertDTO>>> getLowStockItems(
            @ModelAttribute PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getLowStockItems(paginationRequest)));
    }

    @PostMapping("/{productId}/threshold")
    @PreAuthorize("hasPermission('', 'AGESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateThreshold(
            @PathVariable String productId,
            @Valid @RequestBody UpdateThresholdRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.updateThreshold(productId, request.getMinThreshold())));
    }

    @PostMapping("/restock-orders")
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<RestockOrderDTO>> createRestockOrder(
            @Valid @RequestBody CreateRestockOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.createRestockOrder(request)));
    }

}
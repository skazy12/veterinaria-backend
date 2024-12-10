package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.InventoryDTOs.*;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.model.InventoryItem;
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

    @PostMapping("/items")
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> addInventoryItem(
            @RequestBody AddInventoryItemRequest request) {
        try {
            // Convertimos el DTO a entidad
            InventoryItem newItem = new InventoryItem();
            newItem.setName(request.getName());
            newItem.setQuantity(request.getQuantity());
            newItem.setMinThreshold(request.getMinThreshold());
            newItem.setPrice(request.getPrice());

            // Llamamos al servicio para crear el item
            InventoryItem createdItem = inventoryService.addInventoryItem(newItem);

            // Convertimos la respuesta a DTO
            InventoryItemResponse response = convertToResponse(createdItem);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERROR_ADDING_ITEM", e.getMessage()));
        }
    }

    /**
     * Endpoint para actualizar un item existente
     */
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasPermission('', 'GESTIONAR_INVENTARIO')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateInventoryItem(
            @PathVariable String itemId,
            @RequestBody UpdateInventoryItemRequest request) {
        try {
            // Convertimos el DTO a entidad
            InventoryItem itemToUpdate = new InventoryItem();
            itemToUpdate.setQuantity(request.getQuantity());
            itemToUpdate.setMinThreshold(request.getMinThreshold());
            itemToUpdate.setPrice(request.getPrice());

            // Llamamos al servicio para actualizar
            InventoryItem updatedItem = inventoryService.updateInventoryItem(itemId, itemToUpdate);

            // Convertimos la respuesta a DTO
            InventoryItemResponse response = convertToResponse(updatedItem);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERROR_UPDATING_ITEM", e.getMessage()));
        }
    }

    /**
     * Convierte un InventoryItem a InventoryItemResponse
     */
    private InventoryItemResponse convertToResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .minThreshold(item.getMinThreshold())
                .price(item.getPrice())
                .recommendedOrderQuantity(item.getRecommendedOrderQuantity())
                .dateAdded(item.getDateAdded())
                .lastUpdated(item.getLastUpdated())
                .build();
    }


}
package com.example.demo.controller;


import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaymentHistoryDTOs.*;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.service.PaymentHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/payment-history")
public class PaymentHistoryController {

    @Autowired
    private PaymentHistoryService paymentHistoryService;

    /**
     * Obtiene el historial de pagos paginado para el cliente actual
     */
    @GetMapping
    @PreAuthorize("hasPermission('', 'VER_HISTORIAL_PAGOS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<PaymentHistoryResponse>>> getPaymentHistory(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaFin,
            @RequestParam(required = false) String petId,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Double montoMaximo) {

        // Establecer valores por defecto para la paginación
        if (paginationRequest.getSize() <= 0) {
            paginationRequest.setSize(10);  // Tamaño de página por defecto
        }
        if (paginationRequest.getSortBy() == null) {
            paginationRequest.setSortBy("fechaVisita");  // Campo de ordenamiento por defecto
        }
        if (paginationRequest.getSortDirection() == null) {
            paginationRequest.setSortDirection("DESC");  // Dirección de ordenamiento por defecto
        }

        PaymentHistoryFilterRequest filterRequest = PaymentHistoryFilterRequest.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .petId(petId)
                .montoMinimo(montoMinimo)
                .montoMaximo(montoMaximo)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                paymentHistoryService.getPaymentHistory(filterRequest, paginationRequest)
        ));
    }

    /**
     * Obtiene el detalle completo de un pago específico
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasPermission('', 'VER_DETALLE_PAGO')")
    public ResponseEntity<ApiResponse<PaymentHistoryResponse>> getPaymentDetail(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentHistoryService.getPaymentDetail(paymentId)
        ));
    }

    /**
     * Obtiene el resumen de gastos por período
     */
    @GetMapping("/summary")
    @PreAuthorize("hasPermission('', 'VER_RESUMEN_PAGOS')")
    public ResponseEntity<ApiResponse<Object>> getPaymentSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaFin) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentHistoryService.getPaymentSummary(fechaInicio, fechaFin)
        ));
    }
}

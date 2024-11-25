package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ServiceReportDTOs.*;
import com.example.demo.service.ServiceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Service Reports", description = "Endpoints para la gestión de reportes de servicios")
public class ServiceReportController {

    @Autowired
    private ServiceReportService serviceReportService;

    /**
     * Obtiene el reporte de servicios para un período específico usando parámetros de consulta
     */
    @GetMapping("/services")
    @PreAuthorize("hasPermission(null, 'GENERAR_REPORTE_SERVICIOS')")
    @Operation(summary = "Obtener reporte de servicios por período",
            description = "Genera un reporte de servicios usando parámetros de consulta")
    public ResponseEntity<ApiResponse<ServiceReportResponse>> getServiceReport(
            @Parameter(description = "Fecha de inicio", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,

            @Parameter(description = "Fecha de fin", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,

            @Parameter(description = "Período (WEEKLY, MONTHLY, YEARLY)")
            @RequestParam(required = false) String period,

            @Parameter(description = "Categoría de servicio")
            @RequestParam(required = false) String category,

            @Parameter(description = "ID del servicio específico")
            @RequestParam(required = false) String serviceId) {

        ReportFilterRequest filter = ReportFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period(period)
                .category(category)
                .serviceId(serviceId)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                serviceReportService.generateReport(filter)));
    }
}
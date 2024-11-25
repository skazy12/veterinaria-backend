package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.Map;
import java.util.List;

public class ServiceReportDTOs {

    /**
     * DTO para las métricas de uso de un servicio específico
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceMetricsDTO {
        private String serviceId;
        private String serviceName;
        private long totalUsage;               // Número total de veces utilizado
        private double totalRevenue;           // Ingresos totales generados
        private double averageRevenue;         // Ingreso promedio por uso
        private List<MonthlyMetric> monthlyMetrics;  // Métricas mensuales
        private List<String> topPets;          // Mascotas que más utilizan el servicio
    }

    /**
     * DTO para las métricas mensuales
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyMetric {
        private int year;
        private int month;
        private long usage;           // Uso en ese mes
        private double revenue;       // Ingresos en ese mes
    }

    /**
     * DTO para el reporte general de servicios
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceReportResponse {
        private Date startDate;
        private Date endDate;
        private double totalRevenue;
        private long totalServicesUsed;
        private List<ServiceMetricsDTO> servicesMetrics;
        private Map<String, Double> revenueByCategory;    // Ingresos por categoría
        private Map<String, Long> usageByCategory;        // Uso por categoría
    }

    /**
     * DTO para los filtros del reporte
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportFilterRequest {
        private Date startDate;
        private Date endDate;
        private String period;        // "WEEKLY", "MONTHLY", "YEARLY"
        private String category;      // Opcional: filtrar por categoría
        private String serviceId;     // Opcional: filtrar por servicio específico
    }
}
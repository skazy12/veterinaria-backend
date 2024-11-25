package com.example.demo.service;

import com.example.demo.dto.ServiceReportDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.*;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ServiceReportService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceReportService.class);

    @Autowired
    private Firestore firestore;

    /**
     * Genera un reporte de servicios basado en los filtros proporcionados
     */
    public ServiceReportResponse generateReport(ReportFilterRequest filter) {
        try {
            validateDates(filter.getStartDate(), filter.getEndDate());

            List<HistorialClinico> historiales = getHistorialesInPeriod(
                    filter.getStartDate(),
                    filter.getEndDate()
            );

            logger.info("Found {} clinical records in period", historiales.size());

            // Si es categoría OTROS, manejamos solo servicios adicionales
            if (filter.getCategory() != null && filter.getCategory().equals("OTROS")) {
                return generateAdditionalServicesReport(historiales, filter);
            }

            // Si no hay historiales, retornar reporte vacío
            if (historiales.isEmpty()) {
                return createEmptyReport(filter.getStartDate(), filter.getEndDate());
            }

            // Extraer servicios regulares
            List<ServicioRealizado> serviciosRealizados = extractServiciosRealizados(
                    historiales,
                    filter.getCategory()
            );

            // Si no hay filtro de categoría, incluir también servicios adicionales
            List<ServiceMetricsDTO> servicesMetrics = new ArrayList<>();

            if (filter.getCategory() == null) {
                // Procesar servicios regulares
                servicesMetrics.addAll(calculateServicesMetrics(serviciosRealizados, historiales));

                // Procesar servicios adicionales
                List<ServicioAdicionalWrapper> serviciosAdicionales = new ArrayList<>();
                for (HistorialClinico historial : historiales) {
                    if (historial.getServiciosAdicionales() != null) {
                        for (ServicioAdicional servicio : historial.getServiciosAdicionales()) {
                            serviciosAdicionales.add(new ServicioAdicionalWrapper(
                                    servicio,
                                    historial.getFechaVisita()
                            ));
                        }
                    }
                }

                // Agrupar servicios adicionales por descripción
                Map<String, List<ServicioAdicionalWrapper>> serviciosPorDescripcion =
                        serviciosAdicionales.stream()
                                .collect(Collectors.groupingBy(w -> w.servicio.getDescripcion()));

                // Agregar métricas de servicios adicionales
                serviciosPorDescripcion.forEach((descripcion, servicios) -> {
                    double serviceRevenue = servicios.stream()
                            .mapToDouble(w -> w.servicio.getPrecio())
                            .sum();

                    servicesMetrics.add(ServiceMetricsDTO.builder()
                            .serviceId("adicional-" + UUID.randomUUID().toString())
                            .serviceName(descripcion)
                            .totalUsage(servicios.size())
                            .totalRevenue(serviceRevenue)
                            .averageRevenue(serviceRevenue / servicios.size())
                            .monthlyMetrics(calculateMonthlyMetricsForAdicionales(servicios))
                            .build());
                });
            } else {
                // Si hay filtro de categoría, solo incluir servicios regulares
                servicesMetrics.addAll(calculateServicesMetrics(serviciosRealizados, historiales));
            }

            // Calcular métricas por categoría
            Map<String, Double> revenueByCategory = calculateRevenueByCategory(servicesMetrics);
            Map<String, Long> usageByCategory = calculateUsageByCategory(servicesMetrics);

            // Calcular totales
            double totalRevenue = servicesMetrics.stream()
                    .mapToDouble(ServiceMetricsDTO::getTotalRevenue)
                    .sum();

            long totalServicesUsed = servicesMetrics.stream()
                    .mapToLong(ServiceMetricsDTO::getTotalUsage)
                    .sum();

            return ServiceReportResponse.builder()
                    .startDate(filter.getStartDate())
                    .endDate(filter.getEndDate())
                    .totalRevenue(totalRevenue)
                    .totalServicesUsed(totalServicesUsed)
                    .servicesMetrics(servicesMetrics)
                    .revenueByCategory(revenueByCategory)
                    .usageByCategory(usageByCategory)
                    .build();

        } catch (Exception e) {
            logger.error("Error generating service report: ", e);
            throw new CustomExceptions.ProcessingException(
                    "Error generating service report: " + e.getMessage());
        }
    }

    /**
     * Genera un reporte específico para servicios adicionales
     */
    private ServiceReportResponse generateAdditionalServicesReport(
            List<HistorialClinico> historiales,
            ReportFilterRequest filter) {

        // Extraer todos los servicios adicionales
        List<ServicioAdicionalWrapper> serviciosAdicionales = new ArrayList<>();

        for (HistorialClinico historial : historiales) {
            if (historial.getServiciosAdicionales() != null) {
                for (ServicioAdicional servicio : historial.getServiciosAdicionales()) {
                    serviciosAdicionales.add(new ServicioAdicionalWrapper(
                            servicio,
                            historial.getFechaVisita()
                    ));
                }
            }
        }

        // Calcular métricas
        double totalRevenue = serviciosAdicionales.stream()
                .mapToDouble(w -> w.servicio.getPrecio())
                .sum();

        // Agrupar por descripción para calcular uso
        Map<String, List<ServicioAdicionalWrapper>> serviciosPorDescripcion =
                serviciosAdicionales.stream()
                        .collect(Collectors.groupingBy(w -> w.servicio.getDescripcion()));

        // Crear métricas por servicio
        List<ServiceMetricsDTO> servicesMetrics = serviciosPorDescripcion.entrySet().stream()
                .map(entry -> {
                    String descripcion = entry.getKey();
                    List<ServicioAdicionalWrapper> servicios = entry.getValue();

                    double serviceRevenue = servicios.stream()
                            .mapToDouble(w -> w.servicio.getPrecio())
                            .sum();

                    return ServiceMetricsDTO.builder()
                            .serviceId("adicional-" + UUID.randomUUID().toString())
                            .serviceName(descripcion)
                            .totalUsage(servicios.size())
                            .totalRevenue(serviceRevenue)
                            .averageRevenue(serviceRevenue / servicios.size())
                            .monthlyMetrics(calculateMonthlyMetricsForAdicionales(servicios))
                            .build();
                })
                .collect(Collectors.toList());

        return ServiceReportResponse.builder()
                .startDate(filter.getStartDate())
                .endDate(filter.getEndDate())
                .totalRevenue(totalRevenue)
                .totalServicesUsed(serviciosAdicionales.size())
                .servicesMetrics(servicesMetrics)
                .revenueByCategory(Map.of("OTROS", totalRevenue))
                .usageByCategory(Map.of("OTROS", (long) serviciosAdicionales.size()))
                .build();
    }

    /**
     * Clase auxiliar para mantener juntos el servicio adicional y su fecha
     */
    private static class ServicioAdicionalWrapper {
        final ServicioAdicional servicio;
        final Date fecha;

        ServicioAdicionalWrapper(ServicioAdicional servicio, Date fecha) {
            this.servicio = servicio;
            this.fecha = fecha;
        }
    }

    /**
     * Valida las fechas del filtro
     */
    private void validateDates(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    /**
     * Crea un reporte vacío con valores inicializados
     */
    private ServiceReportResponse createEmptyReport(Date startDate, Date endDate) {
        return ServiceReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(0.0)
                .totalServicesUsed(0)
                .servicesMetrics(new ArrayList<>())
                .revenueByCategory(new HashMap<>())
                .usageByCategory(new HashMap<>())
                .build();
    }

    /**
     * Obtiene los historiales clínicos en un período específico
     */
    private List<HistorialClinico> getHistorialesInPeriod(Date startDate, Date endDate)
            throws Exception {
        QuerySnapshot snapshot = firestore.collection("historial_clinico")
                .whereGreaterThanOrEqualTo("fechaVisita", startDate)
                .whereLessThanOrEqualTo("fechaVisita", endDate)
                .get()
                .get();

        return snapshot.getDocuments().stream()
                .map(doc -> {
                    HistorialClinico historial = doc.toObject(HistorialClinico.class);
                    if (historial != null) {
                        historial.setId(doc.getId());
                    }
                    return historial;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extrae y filtra los servicios realizados
     */
    private List<ServicioRealizado> extractServiciosRealizados(
            List<HistorialClinico> historiales,
            String categoryFilter) {

        List<ServicioRealizado> servicios = historiales.stream()
                .filter(h -> h.getServiciosRealizados() != null)
                .flatMap(h -> h.getServiciosRealizados().stream())
                .collect(Collectors.toList());

        // Si no hay filtro de categoría, retornamos todos
        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return servicios;
        }

        // Filtrar por categoría
        try {
            ServiceCategory targetCategory = ServiceCategory.valueOf(categoryFilter);
            return servicios.stream()
                    .filter(s -> {
                        try {
                            DocumentSnapshot serviceDoc = firestore.collection("veterinary_services")
                                    .document(s.getServiceId())
                                    .get()
                                    .get();

                            return serviceDoc.exists() &&
                                    targetCategory.equals(serviceDoc.get("category", ServiceCategory.class));
                        } catch (Exception e) {
                            logger.warn("Error checking service category: {}", e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid category filter: {}", categoryFilter);
            return new ArrayList<>();
        }
    }

    /**
     * Calcula las métricas para cada servicio
     */
    private List<ServiceMetricsDTO> calculateServicesMetrics(
            List<ServicioRealizado> servicios,
            List<HistorialClinico> historiales) {

        // Agrupar servicios por ID
        Map<String, List<ServicioRealizado>> serviciosPorId = servicios.stream()
                .collect(Collectors.groupingBy(ServicioRealizado::getServiceId));

        // Obtener fechas por servicio
        Map<String, List<Date>> fechasPorServicio = createFechasPorServicio(
                historiales,
                serviciosPorId.keySet()
        );

        // Calcular métricas para cada servicio
        return serviciosPorId.entrySet().stream()
                .map(entry -> {
                    String serviceId = entry.getKey();
                    List<ServicioRealizado> usos = entry.getValue();
                    List<Date> fechasUso = fechasPorServicio.get(serviceId);

                    double totalRevenue = usos.stream()
                            .mapToDouble(s -> s.getPrecioPersonalizado() != null ?
                                    s.getPrecioPersonalizado() : s.getPrecioBase())
                            .sum();

                    return ServiceMetricsDTO.builder()
                            .serviceId(serviceId)
                            .serviceName(usos.get(0).getServiceName())
                            .totalUsage(usos.size())
                            .totalRevenue(totalRevenue)
                            .averageRevenue(totalRevenue / usos.size())
                            .monthlyMetrics(calculateMonthlyMetricsForServicios(usos, fechasUso))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Crea un mapa de fechas de uso por servicio
     */
    private Map<String, List<Date>> createFechasPorServicio(
            List<HistorialClinico> historiales,
            Set<String> serviceIds) {

        Map<String, List<Date>> fechasPorServicio = new HashMap<>();

        for (HistorialClinico historial : historiales) {
            Date fecha = historial.getFechaVisita();
            if (fecha == null) continue;

            if (historial.getServiciosRealizados() != null) {
                for (ServicioRealizado servicio : historial.getServiciosRealizados()) {
                    if (servicio != null && serviceIds.contains(servicio.getServiceId())) {
                        fechasPorServicio
                                .computeIfAbsent(servicio.getServiceId(), k -> new ArrayList<>())
                                .add(fecha);
                    }
                }
            }
        }

        return fechasPorServicio;
    }

    /**
     * Calcula métricas mensuales para servicios regulares
     */
    private List<MonthlyMetric> calculateMonthlyMetricsForServicios(
            List<ServicioRealizado> servicios,
            List<Date> fechas) {

        if (fechas == null || fechas.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, MonthlyMetricBuilder> monthlyData = new HashMap<>();

        for (int i = 0; i < servicios.size(); i++) {
            ServicioRealizado servicio = servicios.get(i);
            Date fecha = fechas.get(i);

            if (fecha != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);

                MonthlyMetricBuilder builder = monthlyData.computeIfAbsent(
                        key,
                        k -> new MonthlyMetricBuilder(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH)
                        )
                );

                builder.addUsage();
                builder.addRevenue(servicio.getPrecioPersonalizado() != null ?
                        servicio.getPrecioPersonalizado() : servicio.getPrecioBase());
            }
        }

        return monthlyData.values().stream()
                .map(MonthlyMetricBuilder::build)
                .sorted(Comparator
                        .comparingInt(MonthlyMetric::getYear)
                        .thenComparingInt(MonthlyMetric::getMonth))
                .collect(Collectors.toList());
    }

    /**
     * Calcula métricas mensuales para servicios adicionales
     */
    private List<MonthlyMetric> calculateMonthlyMetricsForAdicionales(
            List<ServicioAdicionalWrapper> servicios) {

        Map<String, MonthlyMetricBuilder> monthlyData = new HashMap<>();

        for (ServicioAdicionalWrapper wrapper : servicios) {
            if (wrapper.fecha != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(wrapper.fecha);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);

                MonthlyMetricBuilder builder = monthlyData.computeIfAbsent(
                        key,
                        k -> new MonthlyMetricBuilder(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH)
                        )
                );

                builder.addUsage();
                builder.addRevenue(wrapper.servicio.getPrecio());
            }
        }

        return monthlyData.values().stream()
                .map(MonthlyMetricBuilder::build)
                .sorted(Comparator
                        .comparingInt(MonthlyMetric::getYear)
                        .thenComparingInt(MonthlyMetric::getMonth))
                .collect(Collectors.toList());
    }
    private static class MonthlyMetricBuilder {
        private final int year;
        private final int month;
        private long usage;
        private double revenue;

        MonthlyMetricBuilder(int year, int month) {
            this.year = year;
            this.month = month;
            this.usage = 0;
            this.revenue = 0.0;
        }

        void addUsage() {
            usage++;
        }

        void addRevenue(double amount) {
            revenue += amount;
        }

        MonthlyMetric build() {
            return MonthlyMetric.builder()
                    .year(year)
                    .month(month)
                    .usage(usage)
                    .revenue(revenue)
                    .build();
        }
    }

    /**
     * Calcula los ingresos por categoría
     */
    private Map<String, Double> calculateRevenueByCategory(List<ServiceMetricsDTO> servicesMetrics) {
        Map<String, Double> revenueByCategory = new HashMap<>();

        for (ServiceMetricsDTO metric : servicesMetrics) {
            try {
                // Si es un servicio adicional
                if (metric.getServiceId().startsWith("adicional-")) {
                    revenueByCategory.merge(
                            ServiceCategory.OTROS.name(),
                            metric.getTotalRevenue(),
                            Double::sum
                    );
                    continue;
                }

                // Para servicios regulares
                DocumentSnapshot serviceDoc = firestore.collection("veterinary_services")
                        .document(metric.getServiceId())
                        .get()
                        .get();

                if (serviceDoc.exists()) {
                    ServiceCategory category = serviceDoc.get("category", ServiceCategory.class);
                    if (category != null) {
                        revenueByCategory.merge(
                                category.name(),
                                metric.getTotalRevenue(),
                                Double::sum
                        );
                    }
                }
            } catch (Exception e) {
                logger.warn("Error calculating revenue for service {}: {}",
                        metric.getServiceId(), e.getMessage());
            }
        }

        return revenueByCategory;
    }

    /**
     * Calcula el uso por categoría
     */
    private Map<String, Long> calculateUsageByCategory(List<ServiceMetricsDTO> servicesMetrics) {
        Map<String, Long> usageByCategory = new HashMap<>();

        for (ServiceMetricsDTO metric : servicesMetrics) {
            try {
                // Si es un servicio adicional
                if (metric.getServiceId().startsWith("adicional-")) {
                    usageByCategory.merge(
                            ServiceCategory.OTROS.name(),
                            metric.getTotalUsage(),
                            Long::sum
                    );
                    continue;
                }

                // Para servicios regulares
                DocumentSnapshot serviceDoc = firestore.collection("veterinary_services")
                        .document(metric.getServiceId())
                        .get()
                        .get();

                if (serviceDoc.exists()) {
                    ServiceCategory category = serviceDoc.get("category", ServiceCategory.class);
                    if (category != null) {
                        usageByCategory.merge(
                                category.name(),
                                metric.getTotalUsage(),
                                Long::sum
                        );
                    }
                }
            } catch (Exception e) {
                logger.warn("Error calculating usage for service {}: {}",
                        metric.getServiceId(), e.getMessage());
            }
        }

        return usageByCategory;
    }

    /**
     * Calcula el ingreso total de todos los servicios
     */
    private double calculateTotalRevenue(List<ServicioRealizado> servicios) {
        return servicios.stream()
                .mapToDouble(s -> s.getPrecioPersonalizado() != null ?
                        s.getPrecioPersonalizado() : s.getPrecioBase())
                .sum();
    }
}



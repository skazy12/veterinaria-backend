package com.example.demo.service;

import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.PaymentHistoryDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.HistorialClinico;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentHistoryService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    /**
     * Obtiene el historial de pagos filtrado y paginado para el cliente actual
     */
    public PaginatedResponse<PaymentHistoryResponse> getPaymentHistory(
            PaymentHistoryFilterRequest filterRequest,
            PaginationRequest paginationRequest) {

        try {
            // Obtener ID del cliente actual
            String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

            // Log para debugging
            log.info("Buscando historial para cliente: {}", clientId);

            // Obtener todas las mascotas del cliente
            List<String> clientPetIds = petService.getCurrentUserPets().stream()
                    .map(pet -> pet.getId())
                    .collect(Collectors.toList());

            log.info("Mascotas encontradas: {}", clientPetIds);

            if (clientPetIds.isEmpty()) {
                return PaginatedResponse.of(new ArrayList<>(), paginationRequest, 0);
            }

            // Construir query base
            CollectionReference historicalRef = firestore.collection("historial_clinico");
            Query query = historicalRef.whereIn("petId", clientPetIds);

            // Log para debugging
            log.info("Aplicando filtros - Fecha inicio: {}, Fecha fin: {}",
                    filterRequest.getFechaInicio(), filterRequest.getFechaFin());

            // Aplicar filtros
            if (filterRequest.getFechaInicio() != null) {
                query = query.whereGreaterThanOrEqualTo("fechaVisita", filterRequest.getFechaInicio());
            }
            if (filterRequest.getFechaFin() != null) {
                query = query.whereLessThanOrEqualTo("fechaVisita", filterRequest.getFechaFin());
            }
            if (filterRequest.getPetId() != null) {
                query = query.whereEqualTo("petId", filterRequest.getPetId());
            }

            // Ordenar por fecha
            query = query.orderBy("fechaVisita", Query.Direction.DESCENDING);

            // Aplicar paginación
            query = query.offset(paginationRequest.getPage() * paginationRequest.getSize())
                    .limit(paginationRequest.getSize());

            // Ejecutar query y obtener resultados
            QuerySnapshot querySnapshot = query.get().get();

            log.info("Documentos encontrados: {}", querySnapshot.size());

            // Convertir documentos a DTOs
            List<PaymentHistoryResponse> payments = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        HistorialClinico historial = doc.toObject(HistorialClinico.class);
                        if (historial != null) {
                            historial.setId(doc.getId()); // Importante: establecer el ID del documento
                        }
                        return convertToPaymentResponse(historial);
                    })
                    .filter(payment -> isWithinMontoRange(payment, filterRequest))
                    .collect(Collectors.toList());

            // Obtener total de elementos
            long totalElements = historicalRef
                    .whereIn("petId", clientPetIds)
                    .get()
                    .get()
                    .size();

            log.info("Total de elementos encontrados: {}", totalElements);

            return new PaginatedResponse<>(
                    payments,
                    paginationRequest.getPage(),
                    paginationRequest.getSize(),
                    totalElements,
                    (int) Math.ceil((double) totalElements / paginationRequest.getSize())
            );

        } catch (Exception e) {
            log.error("Error obteniendo historial de pagos: ", e);
            throw new CustomExceptions.ProcessingException(
                    "Error al obtener historial de pagos: " + e.getMessage());
        }
    }

    /**
     * Obtiene el detalle completo de un pago específico
     */
    public PaymentHistoryResponse getPaymentDetail(String paymentId) {
        try {
            DocumentSnapshot doc = firestore.collection("historial_clinico")
                    .document(paymentId)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Payment record not found");
            }

            HistorialClinico historial = doc.toObject(HistorialClinico.class);

            // Verificar que la mascota pertenece al cliente actual
            String clientId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!isPetOwner(historial.getPetId(), clientId)) {
                throw new CustomExceptions.UnauthorizedException("Unauthorized to view this payment");
            }

            return convertToPaymentResponse(historial);

        } catch (Exception e) {
            log.error("Error getting payment detail: ", e);
            throw new CustomExceptions.ProcessingException(
                    "Error fetching payment detail: " + e.getMessage());
        }
    }

    /**
     * Obtiene el resumen de gastos por período
     */
    public Map<String, Object> getPaymentSummary(Date fechaInicio, Date fechaFin) {
        try {
            String clientId = SecurityContextHolder.getContext().getAuthentication().getName();
            List<String> clientPetIds = petService.getCurrentUserPets().stream()
                    .map(pet -> pet.getId())
                    .collect(Collectors.toList());

            QuerySnapshot querySnapshot = firestore.collection("historial_clinico")
                    .whereIn("petId", clientPetIds)
                    .whereGreaterThanOrEqualTo("fechaVisita", fechaInicio)
                    .whereLessThanOrEqualTo("fechaVisita", fechaFin)
                    .get()
                    .get();

            double totalGastado = 0.0;
            Map<String, Double> gastosPorMascota = new HashMap<>();
            Map<String, Double> gastosPorServicio = new HashMap<>();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                HistorialClinico historial = doc.toObject(HistorialClinico.class);
                if (historial != null) {
                    double montoHistorial = historial.getPrecioTotal();
                    totalGastado += montoHistorial;

                    // Acumular por mascota
                    gastosPorMascota.merge(historial.getPetId(), montoHistorial, Double::sum);

                    // Acumular por tipo de servicio
                    procesarServiciosParaResumen(historial, gastosPorServicio);
                }
            }

            return Map.of(
                    "totalGastado", totalGastado,
                    "gastosPorMascota", gastosPorMascota,
                    "gastosPorServicio", gastosPorServicio
            );

        } catch (Exception e) {
            log.error("Error getting payment summary: ", e);
            throw new CustomExceptions.ProcessingException(
                    "Error calculating payment summary: " + e.getMessage());
        }
    }

    /**
     * Métodos auxiliares
     */
    private PaymentHistoryResponse convertToPaymentResponse(HistorialClinico historial) {
        try {
            return PaymentHistoryResponse.builder()
                    .id(historial.getId())
                    .fecha(historial.getFechaVisita())
                    .petId(historial.getPetId())
                    .petName(petService.getPetById(historial.getPetId()).getName())
                    .montoTotal(historial.getPrecioTotal())
                    .serviciosRealizados(convertirServiciosRealizados(historial))
                    .serviciosAdicionales(convertirServiciosAdicionales(historial))
                    .veterinarioNombre(obtenerNombreVeterinario(historial.getVeterinarianId()))
                    .razon(historial.getMotivoConsulta())
                    .build();
        } catch (Exception e) {
            log.error("Error converting payment response: ", e);
            throw new CustomExceptions.ProcessingException("Error converting payment data");
        }
    }

    private List<ServicioDetalleDTO> convertirServiciosRealizados(HistorialClinico historial) {
        return historial.getServiciosRealizados().stream()
                .map(servicio -> ServicioDetalleDTO.builder()
                        .servicioId(servicio.getServiceId())
                        .nombre(servicio.getServiceName())
                        .precioBase(servicio.getPrecioBase())
                        .precioPersonalizado(servicio.getPrecioPersonalizado())
                        .notas(servicio.getNotas())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ServicioAdicionalDTO> convertirServiciosAdicionales(HistorialClinico historial) {
        return historial.getServiciosAdicionales().stream()
                .map(servicio -> ServicioAdicionalDTO.builder()
                        .descripcion(servicio.getDescripcion())
                        .precio(servicio.getPrecio())
                        .notas(servicio.getNotas())
                        .build())
                .collect(Collectors.toList());
    }
    // Continuación de PaymentHistoryService.java

    /**
     * Verifica si el monto del pago está dentro del rango especificado en el filtro
     */
    private boolean isWithinMontoRange(PaymentHistoryResponse payment, PaymentHistoryFilterRequest filter) {
        if (filter.getMontoMinimo() != null && payment.getMontoTotal() < filter.getMontoMinimo()) {
            return false;
        }
        if (filter.getMontoMaximo() != null && payment.getMontoTotal() > filter.getMontoMaximo()) {
            return false;
        }
        return true;
    }

    /**
     * Verifica si el usuario actual es dueño de la mascota
     */
    private boolean isPetOwner(String petId, String userId) throws Exception {
        return petService.getPetById(petId).getOwnerId().equals(userId);
    }

    /**
     * Obtiene el nombre completo del veterinario
     */
    private String obtenerNombreVeterinario(String veterinarianId) {
        try {
            var veterinario = userService.getUserById(veterinarianId);
            return String.format("Dr. %s %s", veterinario.getNombre(), veterinario.getApellido());
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del veterinario {}: {}", veterinarianId, e.getMessage());
            return "Veterinario no disponible";
        }
    }

    /**
     * Procesa los servicios para el resumen de gastos
     */
    private void procesarServiciosParaResumen(HistorialClinico historial, Map<String, Double> gastosPorServicio) {
        // Procesar servicios regulares
        if (historial.getServiciosRealizados() != null) {
            historial.getServiciosRealizados().forEach(servicio -> {
                double precio = servicio.getPrecioPersonalizado() != null ?
                        servicio.getPrecioPersonalizado() :
                        servicio.getPrecioBase();

                gastosPorServicio.merge(servicio.getServiceName(), precio, Double::sum);
            });
        }

        // Procesar servicios adicionales
        if (historial.getServiciosAdicionales() != null) {
            historial.getServiciosAdicionales().forEach(servicio -> {
                gastosPorServicio.merge("Otros - " + servicio.getDescripcion(),
                        servicio.getPrecio(),
                        Double::sum);
            });
        }
    }
}
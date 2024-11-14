package com.example.demo.service;

import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.ServiceVeterinaryDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.ServiceCategory;
import com.example.demo.model.ServiceVeterinary;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ServiceVeterinaryService {

    @Autowired
    private Firestore firestore;

    /**
     * Obtiene todos los servicios veterinarios con paginación
     */
    public PaginatedResponse<ServiceResponse> getAllServices(PaginationRequest request) {
        try {
            // Crear referencia a la colección
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef;

            // Aplicar filtros si existen
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                // Si el filtro es por categoría, necesitamos convertir el string a enum
                if (request.getFilterBy().equals("category")) {
                    try {
                        ServiceCategory category = ServiceCategory.valueOf(request.getFilterValue());
                        query = query.whereEqualTo("category", category);
                    } catch (IllegalArgumentException e) {
                        throw new CustomExceptions.ProcessingException("Invalid category value");
                    }
                } else {
                    // Para otros filtros mantener el comportamiento actual
                    query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
                }
            }

            // Ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(request.getSortBy(), direction);

            // Paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<ServiceResponse> services = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
                        return convertToServiceResponse(service);
                    })
                    .collect(Collectors.toList());

            // Obtener total de elementos filtrados
            long totalElements;
            if (request.getFilterBy() != null && request.getFilterBy().equals("category")) {
                totalElements = firestore.collection("veterinary_services")
                        .whereEqualTo("category", ServiceCategory.valueOf(request.getFilterValue()))
                        .get().get().size();
            } else {
                totalElements = firestore.collection("veterinary_services")
                        .get().get().size();
            }

            return PaginatedResponse.of(services, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching services: " + e.getMessage());
        }
    }

    /**
     * Crea un nuevo servicio veterinario
     */
    public ServiceResponse createService(CreateServiceRequest request) {
        try {
            // Crear nuevo servicio
            ServiceVeterinary service = ServiceVeterinary.builder()
                    .id(UUID.randomUUID().toString())
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .durationMinutes(request.getDurationMinutes())
                    .isActive(true)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .recommendations(request.getRecommendations())
                    .warnings(request.getWarnings())
                    .requirements(request.getRequirements())
                    .category(request.getCategory())
                    .build();

            // Guardar en Firestore
            firestore.collection("veterinary_services")
                    .document(service.getId())
                    .set(service)
                    .get();

            return convertToServiceResponse(service);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error creating service: " + e.getMessage());
        }
    }

    /**
     * Actualiza un servicio existente
     */
    public ServiceResponse updateService(String id, UpdateServiceRequest request) {
        try {
            // Verificar que el servicio existe
            DocumentSnapshot doc = firestore.collection("veterinary_services")
                    .document(id)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Service not found");
            }

            // Actualizar servicio
            ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
            service.setName(request.getName());
            service.setDescription(request.getDescription());
            service.setPrice(request.getPrice());
            service.setDurationMinutes(request.getDurationMinutes());
            service.setUpdatedAt(new Date());
            service.setRecommendations(request.getRecommendations());
            service.setWarnings(request.getWarnings());
            service.setRequirements(request.getRequirements());
            service.setCategory(request.getCategory());


            // Guardar cambios
            firestore.collection("veterinary_services")
                    .document(id)
                    .set(service)
                    .get();

            return convertToServiceResponse(service);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error updating service: " + e.getMessage());
        }
    }

    /**
     * Elimina un servicio verificando que no tenga citas activas
     */
    public void deleteService(String id) {
        try {
            // Verificar que el servicio existe
            DocumentSnapshot serviceDoc = firestore.collection("veterinary_services")
                    .document(id)
                    .get()
                    .get();

            if (!serviceDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Service not found");
            }

            // Verificar si hay citas activas que usen este servicio
            QuerySnapshot activeAppointments = firestore.collection("appointments")
                    .whereEqualTo("serviceId", id)
                    .whereIn("status", List.of("SCHEDULED", "CONFIRMED"))
                    .get()
                    .get();

            if (!activeAppointments.isEmpty()) {
                throw new CustomExceptions.ProcessingException(
                        "Cannot delete service with active appointments");
            }

            // Eliminar servicio
            firestore.collection("veterinary_services")
                    .document(id)
                    .delete()
                    .get();

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error deleting service: " + e.getMessage());
        }
    }

    /**
     * Activa o desactiva un servicio
     */
    public ServiceResponse toggleServiceStatus(String id) {
        try {
            DocumentSnapshot doc = firestore.collection("veterinary_services")
                    .document(id)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Service not found");
            }

            ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
            service.setActive(!service.isActive());
            service.setUpdatedAt(new Date());

            firestore.collection("veterinary_services")
                    .document(id)
                    .set(service)
                    .get();

            return convertToServiceResponse(service);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error toggling service status: " + e.getMessage());
        }
    }

    public ServiceDetailResponse getServiceDetails(String id) {
        try {
            DocumentSnapshot doc = firestore.collection("veterinary_services")
                    .document(id)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Servicio no encontrado");
            }

            ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
            return convertToServiceDetailResponse(service);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error obteniendo detalles del servicio: " + e.getMessage());
        }
    }
    /**
     * Obtiene la lista de servicios activos con sus detalles completos
     * @return Lista de servicios activos
     */
    public List<ServiceDetailResponse> getActiveServices() {
        try {
            QuerySnapshot snapshot = firestore.collection("veterinary_services")
                    .whereEqualTo("active", true)
                    .orderBy("name")
                    .get()
                    .get();

            return snapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(ServiceVeterinary.class))
                    .map(this::convertToServiceDetailResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error obteniendo servicios activos: " + e.getMessage());
        }
    }
    private ServiceDetailResponse convertToServiceDetailResponse(ServiceVeterinary service) {
        if (service == null) return null;

        return ServiceDetailResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .requirements(service.getRequirements())
                .recommendations(service.getRecommendations())
                .warnings(service.getWarnings())
                .isActive(service.isActive())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }


    /**
     * Convierte un ServiceVeterinary a ServiceResponse
     */
    private ServiceResponse convertToServiceResponse(ServiceVeterinary service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .isActive(service.isActive())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .category(service.getCategory())
                .build();
    }
}
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.ServiceCategory;
import com.example.demo.model.ServiceVeterinary;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VetServiceListService {

    @Autowired
    private Firestore firestore;

    /**
     * Obtiene la lista de servicios aplicando filtros y paginación
     */
    public PaginatedResponse<ServiceDTO> getServiceList(
            ServiceSearchRequest searchRequest,
            PaginationRequest paginationRequest) {
        try {
            // Crear query base
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef;

            // Aplicar filtros
            if (Boolean.TRUE.equals(searchRequest.getOnlyActive())) {
                query = query.whereEqualTo("isActive", true);
            }

            if (searchRequest.getCategory() != null) {
                query = query.whereEqualTo("category", searchRequest.getCategory());
            }

            if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().isEmpty()) {
                // Búsqueda por nombre (case-insensitive)
                String searchTermLower = searchRequest.getSearchTerm().toLowerCase();
                query = query.orderBy("name")
                        .startAt(searchTermLower)
                        .endAt(searchTermLower + "\uf8ff");
            }

            // Aplicar ordenamiento
            Query.Direction direction = paginationRequest.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;

            query = query.orderBy(paginationRequest.getSortBy(), direction);

            // Aplicar paginación
            query = query.offset(paginationRequest.getPage() * paginationRequest.getSize())
                    .limit(paginationRequest.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<ServiceDTO> services = querySnapshot.getDocuments().stream()
                    .map(doc -> convertToServiceDTO(doc.toObject(ServiceVeterinary.class)))
                    .collect(Collectors.toList());

            // Contar total de elementos
            long totalElements = servicesRef.get().get().size();

            return PaginatedResponse.of(services, paginationRequest, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching services: " + e.getMessage());
        }
    }

    /**
     * Obtiene los servicios agrupados por categoría
     */
    public ServiceListResponse getServicesByCategory(ServiceSearchRequest searchRequest) {
        try {
            // Obtener todos los servicios activos
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef.whereEqualTo("isActive", true);

            // Aplicar búsqueda si existe término
            if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().isEmpty()) {
                String searchTermLower = searchRequest.getSearchTerm().toLowerCase();
                query = query.orderBy("name")
                        .startAt(searchTermLower)
                        .endAt(searchTermLower + "\uf8ff");
            }

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Agrupar por categoría
            Map<String, List<ServiceDTO>> servicesByCategory = querySnapshot.getDocuments().stream()
                    .map(doc -> convertToServiceDTO(doc.toObject(ServiceVeterinary.class)))
                    .collect(Collectors.groupingBy(
                            service -> service.getCategory().name(),
                            TreeMap::new,
                            Collectors.toList()
                    ));

            return ServiceListResponse.builder()
                    .servicesByCategory(servicesByCategory)
                    .totalServices(querySnapshot.size())
                    .build();

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching services by category: " + e.getMessage());
        }
    }

    /**
     * Convierte un ServiceVeterinary a ServiceDTO
     */
    private ServiceDTO convertToServiceDTO(ServiceVeterinary service) {
        return ServiceDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .category(ServiceCategory.valueOf(service.getCategory().name()))
                .description(service.getDescription())
                .price(service.getPrice())
                .isActive(service.isActive())
                .build();
    }
}
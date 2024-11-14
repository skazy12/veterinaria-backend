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
    /*
    public PaginatedResponse<ServiceDTO> getServiceList(ServiceSearchRequest searchRequest, PaginationRequest paginationRequest) {
        try {
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef.whereEqualTo("active", true);

            // Solo aplicar filtro si filterBy y filterValue están presentes
            boolean hasNameFilter = paginationRequest.getFilterBy() != null &&
                    paginationRequest.getFilterValue() != null &&
                    !paginationRequest.getFilterValue().isEmpty() &&
                    "name".equalsIgnoreCase(paginationRequest.getFilterBy());

            // Obtener todos los servicios activos
            QuerySnapshot allServices = query.get().get();
            List<ServiceDTO> filteredServices;

            if (hasNameFilter) {
                // Filtrado por nombre (case-insensitive)
                String searchTerm = paginationRequest.getFilterValue().toLowerCase();
                filteredServices = allServices.getDocuments().stream()
                        .map(doc -> {
                            ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
                            if (service != null) {
                                service.setId(doc.getId());
                                return service;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .filter(service -> service.getName().toLowerCase().contains(searchTerm))
                        .map(this::convertToServiceDTO)
                        .collect(Collectors.toList());
            } else {
                // Sin filtro - convertir todos los servicios
                filteredServices = allServices.getDocuments().stream()
                        .map(doc -> {
                            ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
                            if (service != null) {
                                service.setId(doc.getId());
                                return convertToServiceDTO(service);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            // Ordenar resultados si se especifica
            if (paginationRequest.getSortBy() != null && paginationRequest.getSortDirection() != null) {
                boolean isAscending = paginationRequest.getSortDirection().equalsIgnoreCase("ASC");
                filteredServices.sort((s1, s2) -> {
                    int comparison = s1.getName().compareToIgnoreCase(s2.getName());
                    return isAscending ? comparison : -comparison;
                });
            }

            // Aplicar paginación
            int startIndex = paginationRequest.getPage() * paginationRequest.getSize();
            int endIndex = Math.min(startIndex + paginationRequest.getSize(), filteredServices.size());

            List<ServiceDTO> paginatedServices = filteredServices.subList(
                    startIndex,
                    Math.max(startIndex, endIndex)
            );

            return new PaginatedResponse<>(
                    paginatedServices,
                    paginationRequest.getPage(),
                    paginationRequest.getSize(),
                    filteredServices.size(),
                    (int) Math.ceil((double) filteredServices.size() / paginationRequest.getSize())
            );

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching services: " + e.getMessage());
        }
    }

     */
    public PaginatedResponse<ServiceDTO> getServiceList(ServiceSearchRequest searchRequest, PaginationRequest paginationRequest) {
        try {
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef.whereEqualTo("active", true);

            // Obtener todos los servicios activos
            QuerySnapshot allServices = query.get().get();
            List<ServiceDTO> filteredServices = new ArrayList<>();

            // Verificar si hay filtros
            boolean hasNameFilter = paginationRequest.getFilterBy() != null &&
                    paginationRequest.getFilterValue() != null &&
                    !paginationRequest.getFilterValue().isEmpty() &&
                    "name".equalsIgnoreCase(paginationRequest.getFilterBy());

            boolean hasCategoryFilter = searchRequest.getCategory() != null;

            // Aplicar filtros
            filteredServices = allServices.getDocuments().stream()
                    .map(doc -> {
                        ServiceVeterinary service = doc.toObject(ServiceVeterinary.class);
                        if (service != null) {
                            service.setId(doc.getId());
                            return service;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .filter(service -> {
                        boolean matchesName = true;
                        boolean matchesCategory = true;

                        // Filtrar por nombre si existe el filtro
                        if (hasNameFilter) {
                            String searchTerm = paginationRequest.getFilterValue().toLowerCase();
                            matchesName = service.getName().toLowerCase().contains(searchTerm);
                        }

                        // Filtrar por categoría si existe el filtro
                        if (hasCategoryFilter) {
                            matchesCategory = service.getCategory().equals(ServiceCategory.valueOf(searchRequest.getCategory()));
                        }

                        return matchesName && matchesCategory;
                    })
                    .map(this::convertToServiceDTO)
                    .collect(Collectors.toList());

            // Ordenar resultados si se especifica
            if (paginationRequest.getSortBy() != null && paginationRequest.getSortDirection() != null) {
                boolean isAscending = paginationRequest.getSortDirection().equalsIgnoreCase("ASC");
                filteredServices.sort((s1, s2) -> {
                    int comparison = s1.getName().compareToIgnoreCase(s2.getName());
                    return isAscending ? comparison : -comparison;
                });
            }

            // Aplicar paginación
            int startIndex = paginationRequest.getPage() * paginationRequest.getSize();
            int endIndex = Math.min(startIndex + paginationRequest.getSize(), filteredServices.size());

            List<ServiceDTO> paginatedServices = filteredServices.subList(
                    startIndex,
                    Math.max(startIndex, endIndex)
            );

            return new PaginatedResponse<>(
                    paginatedServices,
                    paginationRequest.getPage(),
                    paginationRequest.getSize(),
                    filteredServices.size(),
                    (int) Math.ceil((double) filteredServices.size() / paginationRequest.getSize())
            );

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching services: " + e.getMessage());
        }
    }
    /**
     * Obtiene los servicios agrupados por categoría
     */
    public ServiceListResponse getServicesByCategory(ServiceSearchRequest searchRequest) {
        try {
            // Obtener todos los servicios activos
            CollectionReference servicesRef = firestore.collection("veterinary_services");
            Query query = servicesRef.whereEqualTo("active", true);

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
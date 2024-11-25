package com.example.demo.service;


import com.example.demo.dto.HistorialClinicoDTOs.*;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.ServiceVeterinaryDTOs;
import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.HistorialClinico;
import com.example.demo.model.ServicioAdicional;
import com.example.demo.model.ServicioRealizado;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class HistorialClinicoService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private ServiceVeterinaryService serviceVeterinaryService;

    public HistorialClinicoResponse createHistorial(String petId, CreateHistorialRequest request) {
        String veterinarianId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Verificar que la mascota existe
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Mascota no encontrada");
            }

            // Procesar servicios realizados
            List<ServicioRealizado> serviciosRealizados = new ArrayList<>();
            double precioTotal = 0.0;

            if (request.getServiciosRealizados() != null) {
                for (ServicioRealizadoRequest servicioRequest : request.getServiciosRealizados()) {
                    // Obtener detalles del servicio
                    ServiceVeterinaryDTOs.ServiceDetailResponse serviceDetail =
                            serviceVeterinaryService.getServiceDetails(servicioRequest.getServiceId());

                    // Calcular precio final del servicio
                    double precioServicio = servicioRequest.getPrecioPersonalizado() != null
                            ? servicioRequest.getPrecioPersonalizado()
                            : serviceDetail.getPrice();

                    // Crear registro del servicio
                    ServicioRealizado servicio = ServicioRealizado.builder()
                            .serviceId(serviceDetail.getId())
                            .serviceName(serviceDetail.getName())
                            .precioBase(serviceDetail.getPrice())
                            .precioPersonalizado(servicioRequest.getPrecioPersonalizado())
                            .notas(servicioRequest.getNotas())
                            .build();

                    serviciosRealizados.add(servicio);
                    precioTotal += precioServicio;
                }
            }

            // Procesar servicios adicionales
            List<ServicioAdicional> serviciosAdicionales = new ArrayList<>();
            if (request.getServiciosAdicionales() != null) {
                for (ServicioAdicionalRequest servicioRequest : request.getServiciosAdicionales()) {
                    ServicioAdicional servicio = ServicioAdicional.builder()
                            .descripcion(servicioRequest.getDescripcion())
                            .precio(servicioRequest.getPrecio())
                            .notas(servicioRequest.getNotas())
                            .build();

                    serviciosAdicionales.add(servicio);
                    precioTotal += servicioRequest.getPrecio();
                }
            }

            // Crear el historial clínico
            HistorialClinico historial = HistorialClinico.builder()
                    .id(UUID.randomUUID().toString())
                    .petId(petId)
                    .veterinarianId(veterinarianId)
                    .fechaVisita(new Date())
                    .motivoConsulta(request.getMotivoConsulta())
                    .diagnostico(request.getDiagnostico())
                    .tratamiento(request.getTratamiento())
                    .observaciones(request.getObservaciones())
                    .serviciosRealizados(serviciosRealizados)
                    .serviciosAdicionales(serviciosAdicionales)
                    .precioTotal(precioTotal)
                    .fechaCreacion(new Date())
                    .fechaActualizacion(new Date())
                    .estado("ACTIVO")
                    .build();

            // Guardar en Firestore
            firestore.collection("historial_clinico")
                    .document(historial.getId())
                    .set(historial)
                    .get();

            return enrichHistorialResponse(historial);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error creating historial clinico: " + e.getMessage());
        }
    }

    public HistorialClinicoResponse updateHistorial(String historialId, UpdateHistorialRequest request) {
        try {
            DocumentReference historialRef = firestore.collection("historial_clinico").document(historialId);
            DocumentSnapshot historialDoc = historialRef.get().get();

            if (!historialDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Historial clínico no encontrado");
            }

            HistorialClinico historial = historialDoc.toObject(HistorialClinico.class);

            // Actualizar campos básicos
            historial.setMotivoConsulta(request.getMotivoConsulta());
            historial.setDiagnostico(request.getDiagnostico());
            historial.setTratamiento(request.getTratamiento());
            historial.setObservaciones(request.getObservaciones());

            // Actualizar servicios realizados y recalcular precio total
            double precioTotal = 0.0;

            if (request.getServiciosRealizados() != null) {
                List<ServicioRealizado> serviciosRealizados = new ArrayList<>();
                for (ServicioRealizadoRequest servicioRequest : request.getServiciosRealizados()) {
                    // Obtener detalles del servicio
                    ServiceVeterinaryDTOs.ServiceDetailResponse serviceDetail =
                            serviceVeterinaryService.getServiceDetails(servicioRequest.getServiceId());

                    // Calcular precio final del servicio
                    double precioServicio = servicioRequest.getPrecioPersonalizado() != null
                            ? servicioRequest.getPrecioPersonalizado()
                            : serviceDetail.getPrice();

                    // Crear registro del servicio
                    ServicioRealizado servicio = ServicioRealizado.builder()
                            .serviceId(serviceDetail.getId())
                            .serviceName(serviceDetail.getName())
                            .precioBase(serviceDetail.getPrice())
                            .precioPersonalizado(servicioRequest.getPrecioPersonalizado())
                            .notas(servicioRequest.getNotas())
                            .build();

                    serviciosRealizados.add(servicio);
                    precioTotal += precioServicio;
                }
                historial.setServiciosRealizados(serviciosRealizados);
            }

            // Actualizar servicios adicionales
            if (request.getServiciosAdicionales() != null) {
                List<ServicioAdicional> serviciosAdicionales = new ArrayList<>();
                for (ServicioAdicionalRequest servicioRequest : request.getServiciosAdicionales()) {
                    ServicioAdicional servicio = ServicioAdicional.builder()
                            .descripcion(servicioRequest.getDescripcion())
                            .precio(servicioRequest.getPrecio())
                            .notas(servicioRequest.getNotas())
                            .build();

                    serviciosAdicionales.add(servicio);
                    precioTotal += servicioRequest.getPrecio();
                }
                historial.setServiciosAdicionales(serviciosAdicionales);
            }

            // Actualizar precio total y fecha de actualización
            historial.setPrecioTotal(precioTotal);
            historial.setFechaActualizacion(new Date());

            // Guardar cambios
            historialRef.set(historial).get();

            return enrichHistorialResponse(historial);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error updating historial clinico: " + e.getMessage());
        }
    }

    public List<HistorialClinicoResponse> getHistorialByPetId(String petId) {
        try {
            QuerySnapshot querySnapshot = firestore.collection("historial_clinico")
                    .whereEqualTo("petId", petId)
                    .orderBy("fechaVisita", Query.Direction.DESCENDING)
                    .get()
                    .get();

            List<HistorialClinicoResponse> historialList = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                HistorialClinico historial = doc.toObject(HistorialClinico.class);
                historialList.add(enrichHistorialResponse(historial));
            }

            return historialList;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching historial clinico: " + e.getMessage());
        }
    }

    public HistorialClinicoResponse getHistorialById(String historialId) {
        try {
            DocumentSnapshot doc = firestore.collection("historial_clinico")
                    .document(historialId)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Historial clínico no encontrado");
            }

            HistorialClinico historial = doc.toObject(HistorialClinico.class);
            return enrichHistorialResponse(historial);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching historial clinico: " + e.getMessage());
        }
    }

    private HistorialClinicoResponse enrichHistorialResponse(HistorialClinico historial) {
        try {
            // Obtener información del veterinario
            UserDTOs.UserResponse veterinarian = userService.getUserById(historial.getVeterinarianId());

            // Obtener información de la mascota
            PetDTOs.PetResponse pet = petService.getPetById(historial.getPetId());

            // Obtener información del dueño
            UserDTOs.UserResponse owner = userService.getUserById(pet.getOwnerId());

            // Convertir servicios realizados
            List<ServicioRealizadoResponse> serviciosRealizadosResponse =
                    historial.getServiciosRealizados().stream()
                            .map(servicio -> ServicioRealizadoResponse.builder()
                                    .serviceId(servicio.getServiceId())
                                    .serviceName(servicio.getServiceName())
                                    .precioBase(servicio.getPrecioBase())
                                    .precioPersonalizado(servicio.getPrecioPersonalizado())
                                    .notas(servicio.getNotas())
                                    .build())
                            .collect(Collectors.toList());

            // Convertir servicios adicionales
            List<ServicioAdicionalResponse> serviciosAdicionalesResponse =
                    historial.getServiciosAdicionales().stream()
                            .map(servicio -> ServicioAdicionalResponse.builder()
                                    .descripcion(servicio.getDescripcion())
                                    .precio(servicio.getPrecio())
                                    .notas(servicio.getNotas())
                                    .build())
                            .collect(Collectors.toList());

            return HistorialClinicoResponse.builder()
                    .id(historial.getId())
                    .petId(historial.getPetId())
                    .veterinarianId(historial.getVeterinarianId())
                    .veterinarianName(veterinarian.getNombre() + " " + veterinarian.getApellido())
                    .petName(pet.getName())
                    .ownerName(owner.getNombre() + " " + owner.getApellido())
                    .fechaVisita(historial.getFechaVisita())
                    .motivoConsulta(historial.getMotivoConsulta())
                    .diagnostico(historial.getDiagnostico())
                    .tratamiento(historial.getTratamiento())
                    .observaciones(historial.getObservaciones())
                    .serviciosRealizados(serviciosRealizadosResponse)
                    .serviciosAdicionales(serviciosAdicionalesResponse)
                    .precioTotal(historial.getPrecioTotal())
                    .fechaCreacion(historial.getFechaCreacion())
                    .fechaActualizacion(historial.getFechaActualizacion())
                    .estado(historial.getEstado())
                    .build();
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error enriching historial response: " + e.getMessage());
        }
    }
}
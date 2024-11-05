package com.example.demo.service;


import com.example.demo.dto.HistorialClinicoDTOs.*;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.HistorialClinico;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class HistorialClinicoService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    public HistorialClinicoResponse createHistorial(String petId, CreateHistorialRequest request) {
        String veterinarianId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Verificar que la mascota existe
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Mascota no encontrada");
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
            throw new CustomExceptions.ProcessingException("Error creating historial clinico: " + e.getMessage());
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
            historial.setMotivoConsulta(request.getMotivoConsulta());
            historial.setDiagnostico(request.getDiagnostico());
            historial.setTratamiento(request.getTratamiento());
            historial.setObservaciones(request.getObservaciones());
            historial.setFechaActualizacion(new Date());

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

    private HistorialClinicoResponse enrichHistorialResponse(HistorialClinico historial)
            throws ExecutionException, InterruptedException {
        // Obtener información del veterinario
        UserDTOs.UserResponse veterinarian = userService.getUserById(historial.getVeterinarianId());

        // Obtener información de la mascota
        PetDTOs.PetResponse pet = petService.getPetById(historial.getPetId());

        // Obtener información del dueño
        UserDTOs.UserResponse owner = userService.getUserById(pet.getOwnerId());

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
                .fechaCreacion(historial.getFechaCreacion())
                .fechaActualizacion(historial.getFechaActualizacion())
                .estado(historial.getEstado())
                .build();
    }
}
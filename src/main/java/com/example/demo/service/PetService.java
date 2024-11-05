package com.example.demo.service;

import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.PetDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.MedicalRecord;
import com.example.demo.model.Pet;
import com.example.demo.model.User;
import com.example.demo.repository.PetRepository;
import com.example.demo.util.FirestorePaginationUtils;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class PetService {
    @Autowired
    private Firestore firestore;
    @Autowired
    private PetRepository petRepository;

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
    public List<PetResponse> getCurrentUserPets() {
        String uid = getCurrentUserUid();
        return getPetsByUserId(uid);
    }

    public PetResponse createPet(CreatePetRequest request) {
        String ownerId = getCurrentUserUid();

        // Crear un nuevo documento en Firestore y obtener su ID
        DocumentReference newPetRef = firestore.collection("pets").document();
        String newPetId = newPetRef.getId();

        Pet pet = new Pet();
        pet.setId(newPetId);  // Asignar el ID generado
        pet.setName(request.getName());
        pet.setSpecies(request.getSpecies());
        pet.setBreed(request.getBreed());
        pet.setAge(request.getAge());
        pet.setOwnerId(ownerId);

        try {
            // Guardar la mascota en Firestore usando el ID generado
            newPetRef.set(pet).get();

            // Crear y devolver la respuesta
            return convertToPetResponse(pet);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error al crear la mascota: " + e.getMessage());
        }
    }
    private String savePetToFirestore(Pet petToSave) throws InterruptedException, ExecutionException {
        // Add the pet to Firestore and get the auto-generated ID
        return getFirestore().collection("pets").add(petToSave).get().getId();
    }

    public PetResponse updatePet(String id, UpdatePetRequest request) {
        try {
            Pet pet = getFirestore().collection("pets").document(id).get().get().toObject(Pet.class);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Pet not found with id: " + id);
            }
            pet.setName(request.getName());
            pet.setSpecies(request.getSpecies());
            pet.setBreed(request.getBreed());
            pet.setAge(request.getAge());

            getFirestore().collection("pets").document(id).set(pet).get();
            return convertToPetResponse(pet);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating pet: " + e.getMessage());
        }
    }
    public PetResponse getPetById(String id) {
        try {
            Pet pet = getFirestore().collection("pets").document(id).get().get().toObject(Pet.class);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Pet not found with id: " + id);
            }
            return convertToPetResponse(pet);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching pet: " + e.getMessage());
        }
    }

    public List<PetResponse> getPetsByUserId(String userId) {
        try {
            List<PetResponse> pets = new ArrayList<>();
            getFirestore().collection("pets").whereEqualTo("ownerId", userId).get().get().getDocuments().forEach(doc -> {
                Pet pet = doc.toObject(Pet.class);
                pets.add(convertToPetResponse(pet));
            });
            return pets;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user's pets: " + e.getMessage());
        }
    }

    public PaginatedResponse<MedicalRecordResponse> getPetMedicalHistory(String petId, PaginationRequest request) {
        try {
            CollectionReference recordsRef = firestore.collection("pets")
                    .document(petId)
                    .collection("medicalRecords");

            Query query = recordsRef;

            // Aplicar filtros
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
            }

            // Ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(request.getSortBy(), direction);

            // Paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            QuerySnapshot querySnapshot = query.get().get();

            List<MedicalRecordResponse> records = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        MedicalRecord record = doc.toObject(MedicalRecord.class);
                        return convertToMedicalRecordResponse(record);
                    })
                    .collect(Collectors.toList());

            long totalElements = FirestorePaginationUtils.getTotalElements(recordsRef);

            return PaginatedResponse.of(records, request, totalElements);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching medical history: " + e.getMessage());
        }
    }
    public MedicalRecordResponse addMedicalRecord(String petId, AddMedicalRecordRequest request) {
        try {
            MedicalRecordResponse record = new MedicalRecordResponse();
            record.setId(UUID.randomUUID().toString());
            record.setDate(new Date());
            record.setDiagnosis(request.getDiagnosis());
            record.setTreatment(request.getTreatment());
            record.setVeterinarianId(getCurrentUserUid());

            getFirestore().collection("pets").document(petId).collection("medicalRecords").document(record.getId()).set(record).get();
            return record;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding medical record: " + e.getMessage());
        }
    }

    public boolean isOwner(String petId) {
        String currentUserId = getCurrentUserUid();
        try {
            Pet pet = getFirestore().collection("pets").document(petId).get().get().toObject(Pet.class);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Pet not found with id: " + petId);
            }
            return pet.getOwnerId().equals(currentUserId);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error checking pet ownership: " + e.getMessage());
        }
    }
    private String getCurrentUserUid() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PetResponse convertToPetResponse(Pet pet) {
        PetResponse response=new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setSpecies(pet.getSpecies());
        response.setBreed(pet.getBreed());
        response.setAge(pet.getAge());
        response.setOwnerId(pet.getOwnerId());
        return response;
    }
    private Pet convertToPet(PetResponse petResponse) {
        Pet pet = new Pet();
        pet.setId(petResponse.getId());
        pet.setName(petResponse.getName());
        pet.setAge(petResponse.getAge());
        // Set other fields as necessary
        return pet;
    }
    public void deletePet(String id) {
        try {
            // Obtener el UID del usuario actual
            String currentUserUid = getCurrentUserUid();

            // Verificar si la mascota existe y pertenece al usuario actual
            DocumentSnapshot petDoc = firestore.collection("pets").document(id).get().get();

            if (!petDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Mascota no encontrada con id: " + id);
            }

            Pet pet = petDoc.toObject(Pet.class);
            if (pet == null || !pet.getOwnerId().equals(currentUserUid)) {
                throw new CustomExceptions.UnauthorizedException("No tienes permiso para eliminar esta mascota");
            }

            // Eliminar la mascota
            firestore.collection("pets").document(id).delete().get();

            // Opcionalmente, puedes también eliminar registros relacionados, como historial médico
            // Esto depende de cómo esté estructurada tu base de datos
            firestore.collection("pets").document(id).collection("medicalRecords").get().get()
                    .getDocuments().forEach(doc -> doc.getReference().delete());

        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error al eliminar la mascota: " + e.getMessage());
        }
    }
    public PaginatedResponse<PetResponse> getAllPets(PaginationRequest request) {
        try {
            // Obtener referencia a la colección
            CollectionReference petsRef = firestore.collection("pets");

            // Determinar dirección de ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;

            // Obtener datos paginados
            QuerySnapshot querySnapshot = FirestorePaginationUtils.getPaginatedData(
                    petsRef, request, direction
            );

            // Convertir documentos a PetResponse
            List<PetResponse> pets = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Pet pet = doc.toObject(Pet.class);
                        return convertToPetResponse(pet);
                    })
                    .collect(Collectors.toList());

            // Obtener total de elementos
            long totalElements = FirestorePaginationUtils.getTotalElements(petsRef);

            // Crear respuesta paginada
            return PaginatedResponse.of(pets, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error al obtener las mascotas: " + e.getMessage());
        }
    }
    public PaginatedResponse<PetResponse> getPetsByUserId(String userId, PaginationRequest request) {
        try {
            CollectionReference petsRef = firestore.collection("pets");
            Query query = petsRef.whereEqualTo("ownerId", userId);

            // Aplicar filtros adicionales si existen
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
            }

            // Aplicar ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(request.getSortBy(), direction);

            // Aplicar paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<PetResponse> pets = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Pet pet = doc.toObject(Pet.class);
                        return convertToPetResponse(pet);
                    })
                    .collect(Collectors.toList());

            // Obtener total de elementos para este usuario
            long totalElements = petsRef
                    .whereEqualTo("ownerId", userId)
                    .get().get().size();

            return PaginatedResponse.of(pets, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching user's pets: " + e.getMessage());
        }
    }
    public MedicalRecordResponse convertToMedicalRecordResponse(MedicalRecord record) {
        if (record == null) {
            return null;
        }

        MedicalRecordResponse response = new MedicalRecordResponse();
        response.setId(record.getId());
        response.setDate(record.getDate());
        response.setDiagnosis(record.getDiagnosis());
        response.setTreatment(record.getTreatment());
        response.setNotes(record.getNotes());
        response.setVeterinarianId(record.getVeterinarianId());

        // Obtener información del veterinario si está disponible
        try {
            if (record.getVeterinarianId() != null) {
                DocumentSnapshot vetDoc = firestore.collection("users")
                        .document(record.getVeterinarianId())
                        .get()
                        .get();

                if (vetDoc.exists()) {
                    User vet = vetDoc.toObject(User.class);
                    if (vet != null) {
                        response.setVeterinarianName(vet.getNombre() + " " + vet.getApellido());
                    }
                }
            }
        } catch (Exception e) {
            // Si hay error al obtener el veterinario, dejamos el nombre como null
            //logger.warn("Error getting veterinarian info for medical record: {}", e.getMessage());
        }

        return response;
    }

}
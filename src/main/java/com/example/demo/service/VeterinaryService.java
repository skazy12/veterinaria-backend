package com.example.demo.service;

import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.MedicalRecord;
import com.example.demo.model.Pet;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.util.FirestorePaginationUtils;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class VeterinaryService {
    private static final Logger logger = LoggerFactory.getLogger(VeterinaryService.class);

    // Constantes para mensajes de error comunes
    private static final String ERROR_FETCHING_PETS = "Error al obtener las mascotas: {}";
    private static final String ERROR_FETCHING_HISTORY = "Error al obtener el historial médico: {}";
    private static final String ERROR_VETERINARIAN_NOT_FOUND = "No se pudo encontrar el veterinario con ID: {}";
    private static final String ERROR_CONVERTING_RECORD = "Error al convertir el registro médico: {}";


    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    /**
     * Busca clientes y sus mascotas según los criterios especificados
     */
    /*
    public List<UserDTOs.ClientWithPetsDTO> searchClients(UserDTOs.ClientSearchCriteria criteria) {
        try {
            // Crear query base
            Query query = firestore.collection("users");

            // Aplicar filtros según los criterios
            if (criteria.getClientName() != null) {
                query = query.whereGreaterThanOrEqualTo("nombre", criteria.getClientName())
                        .whereLessThanOrEqualTo("nombre", criteria.getClientName() + '\uf8ff');
            }

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();
            List<UserDTOs.ClientWithPetsDTO> results = new ArrayList<>();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                User user = document.toObject(User.class);
                if (user != null) {
                    // Obtener mascotas del cliente
                    List<PetDTOs.PetWithHistoryDTO> pets = getPetsWithHistory(user.getUid());

                    // Filtrar por nombre de mascota si es necesario
                    if (criteria.getPetName() != null) {
                        pets = pets.stream()
                                .filter(pet -> pet.getName().toLowerCase()
                                        .contains(criteria.getPetName().toLowerCase()))
                                .collect(Collectors.toList());
                    }

                    // Filtrar por fecha de consulta si es necesario
                    if (criteria.getConsultationDate() != null) {
                        pets = filterPetsByConsultationDate(pets, criteria.getConsultationDate());
                    }

                    // Si hay mascotas que cumplen con los filtros, agregar el cliente al resultado
                    if (!pets.isEmpty()) {
                        UserDTOs.ClientWithPetsDTO clientDTO = convertToClientWithPetsDTO(user, pets);
                        results.add(clientDTO);
                    }
                }
            }

            return results;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error searching clients: " + e.getMessage());
        }
    }

     */
    public PaginatedResponse<UserDTOs.ClientWithPetsDTO> searchClients(
            UserDTOs.ClientSearchCriteria criteria, PaginationRequest request) {
        try {
            CollectionReference usersRef = firestore.collection("users");
            Query query = usersRef;

            // Aplicar filtros de búsqueda
            if (criteria.getClientName() != null) {
                query = query.whereGreaterThanOrEqualTo("nombre", criteria.getClientName())
                        .whereLessThanOrEqualTo("nombre", criteria.getClientName() + '\uf8ff');
            }

            // Aplicar filtros adicionales
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

            List<UserDTOs.ClientWithPetsDTO> clients = new ArrayList<>();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    List<PetDTOs.PetWithHistoryDTO> pets = getPetsWithHistory(user.getUid());

                    // Filtrar por nombre de mascota si es necesario
                    if (criteria.getPetName() != null) {
                        pets = pets.stream()
                                .filter(pet -> pet.getName().toLowerCase()
                                        .contains(criteria.getPetName().toLowerCase()))
                                .collect(Collectors.toList());
                    }

                    // Filtrar por fecha de consulta si es necesario
                    if (criteria.getConsultationDate() != null) {
                        pets = filterPetsByConsultationDate(pets, criteria.getConsultationDate());
                    }

                    if (!pets.isEmpty()) {
                        clients.add(convertToClientWithPetsDTO(user, pets));
                    }
                }
            }

            long totalElements = firestore.collection("users")
                    .get().get().size();

            return PaginatedResponse.of(clients, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error searching clients: " + e.getMessage());
        }
    }

    /**
     * Agrega un registro médico a una mascota
     */
    public PetDTOs.MedicalRecordResponse addMedicalRecord(String petId, PetDTOs.AddMedicalRecordRequest request) {
        String currentVetId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Verificar que la mascota existe
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Mascota no encontrada");
            }

            // Crear el registro médico
            MedicalRecord record = new MedicalRecord();
            record.setId(UUID.randomUUID().toString());
            record.setDate(new Date());
            record.setDiagnosis(request.getDiagnosis());
            record.setTreatment(request.getTreatment());
            record.setNotes(request.getNotes());
            record.setVeterinarianId(currentVetId);

            // Guardar el registro
            firestore.collection("pets").document(petId)
                    .collection("medicalRecords")
                    .document(record.getId())
                    .set(record)
                    .get();

            // Notificar al dueño
            notifyOwner(pet.getOwnerId(), "Nuevo registro médico",
                    "Se ha actualizado el historial médico de " + pet.getName());

            return convertToMedicalRecordResponse(record);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error adding medical record: " + e.getMessage());
        }
    }

    /**
     * Obtiene el historial médico de una mascota
     */
    public PaginatedResponse<PetDTOs.MedicalRecordResponse> getMedicalHistory(
            String petId, PaginationRequest request) {
        try {
            CollectionReference recordsRef = firestore.collection("pets")
                    .document(petId)
                    .collection("medicalRecords");

            Query query = recordsRef;

            // Aplicar filtros
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
            }

            // Ordenamiento por defecto por fecha si no se especifica otro campo
            String sortBy = request.getSortBy().equals("id") ? "date" : request.getSortBy();
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(sortBy, direction);

            // Paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            QuerySnapshot querySnapshot = query.get().get();

            List<PetDTOs.MedicalRecordResponse> history = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                MedicalRecord record = doc.toObject(MedicalRecord.class);
                if (record != null) {
                    history.add(convertToMedicalRecordResponse(record));
                }
            }

            long totalElements = FirestorePaginationUtils.getTotalElements(recordsRef);

            return PaginatedResponse.of(history, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching medical history: " + e.getMessage());
        }
    }

    // Métodos privados auxiliares...




    private void notifyOwner(String ownerId, String title, String message) {
        // Implementar notificación (por ejemplo, usando Firebase Cloud Messaging)
    }
    private UserDTOs.ClientWithPetsDTO convertToClientWithPetsDTO(User user, List<PetDTOs.PetWithHistoryDTO> pets) {
        UserDTOs.ClientWithPetsDTO dto = new UserDTOs.ClientWithPetsDTO();
        dto.setUid(user.getUid());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setEmail(user.getEmail());
        dto.setTelefono(user.getTelefono());
        dto.setDireccion(user.getDireccion());
        dto.setMascotas(pets);
        return dto;
    }

    /**
     * Convierte una Pet y su historial médico a PetWithHistoryDTO
     */
    private PetDTOs.PetWithHistoryDTO convertToPetWithHistoryDTO(Pet pet, List<PetDTOs.MedicalRecordResponse> history) {
        PetDTOs.PetWithHistoryDTO dto = new PetDTOs.PetWithHistoryDTO();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setSpecies(pet.getSpecies());
        dto.setBreed(pet.getBreed());
        dto.setAge(pet.getAge());
        dto.setMedicalHistory(history);
        return dto;
    }

    /**
     * Convierte un MedicalRecord a MedicalRecordResponse
     * Incluye información del veterinario
     */
    private PetDTOs.MedicalRecordResponse convertToMedicalRecordResponse(MedicalRecord record) {
        PetDTOs.MedicalRecordResponse response = new PetDTOs.MedicalRecordResponse();
        response.setId(record.getId());
        response.setDate(record.getDate());
        response.setDiagnosis(record.getDiagnosis());
        response.setTreatment(record.getTreatment());
        response.setNotes(record.getNotes());
        response.setVeterinarianId(record.getVeterinarianId());

        // Obtener el nombre del veterinario
        try {
            UserDTOs.UserResponse veterinarian = userService.getUserById(record.getVeterinarianId());
            response.setVeterinarianName(veterinarian.getNombre() + " " + veterinarian.getApellido());
        } catch (Exception e) {
            response.setVeterinarianName("Veterinario no encontrado");
            //logger.warn("No se pudo obtener el nombre del veterinario para el registro médico: " + record.getId());
        }

        return response;
    }

    /**
     * Obtiene las mascotas con su historial médico para un cliente específico
     */
    private List<PetDTOs.PetWithHistoryDTO> getPetsWithHistory(String userId) throws ExecutionException, InterruptedException {
        // Obtener todas las mascotas del usuario
        QuerySnapshot petsSnapshot = firestore.collection("pets")
                .whereEqualTo("ownerId", userId)
                .get()
                .get();

        List<PetDTOs.PetWithHistoryDTO> petsWithHistory = new ArrayList<>();
        for (DocumentSnapshot petDoc : petsSnapshot.getDocuments()) {
            Pet pet = petDoc.toObject(Pet.class);
            if (pet != null) {
                // Obtener historial médico básico (últimas 5 entradas)
                QuerySnapshot historySnapshot = firestore.collection("pets")
                        .document(pet.getId())
                        .collection("medicalRecords")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .get();

                List<PetDTOs.MedicalRecordResponse> history = historySnapshot.getDocuments().stream()
                        .map(doc -> {
                            MedicalRecord record = doc.toObject(MedicalRecord.class);
                            return convertToMedicalRecordResponse(record);
                        })
                        .collect(Collectors.toList());

                petsWithHistory.add(convertToPetWithHistoryDTO(pet, history));
            }
        }

        return petsWithHistory;
    }
    /**
     * Convierte un AddMedicalRecordRequest a MedicalRecord
     */
    private MedicalRecord convertToMedicalRecord(PetDTOs.AddMedicalRecordRequest request, String veterinarianId) {
        MedicalRecord record = new MedicalRecord();
        record.setId(UUID.randomUUID().toString());
        record.setDate(new Date());
        record.setDiagnosis(request.getDiagnosis());
        record.setTreatment(request.getTreatment());
        record.setNotes(request.getNotes());
        record.setVeterinarianId(veterinarianId);
        record.setAttachments(new ArrayList<>()); // Lista vacía de adjuntos inicialmente
        return record;
    }

    /**
     * Convierte datos del Firestore a MedicalRecord
     */
    private MedicalRecord convertFirestoreToMedicalRecord(DocumentSnapshot doc) {
        MedicalRecord record = new MedicalRecord();
        record.setId(doc.getId());
        record.setDate(doc.getDate("date"));
        record.setDiagnosis(doc.getString("diagnosis"));
        record.setTreatment(doc.getString("treatment"));
        record.setNotes(doc.getString("notes"));
        record.setVeterinarianId(doc.getString("veterinarianId"));

        // Convertir la lista de adjuntos si existe
        @SuppressWarnings("unchecked")
        List<String> attachments = (List<String>) doc.get("attachments");
        record.setAttachments(attachments != null ? attachments : new ArrayList<>());

        return record;
    }

    //----
    public UserDTOs.ClientWithPetsDTO getClientWithPets(String clientId) {
        try {
            // Verificar que el usuario actual es veterinario
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            UserDTOs.UserResponse currentUser = userService.getUserById(currentUserId);
            /*
            if (!currentUser.getRoles().contains(Role.VETERINARIO) ) {
                throw new CustomExceptions.UnauthorizedException("Solo los veterinarios pueden acceder a esta información");
            }

             */
            // Obtener información del cliente
            DocumentSnapshot clientDoc = firestore.collection("users")
                    .document(clientId)
                    .get()
                    .get();

            if (!clientDoc.exists()) {
                logger.warn("Cliente no encontrado con ID: {}", clientId);
                throw new CustomExceptions.NotFoundException("Cliente no encontrado");
            }

            User client = clientDoc.toObject(User.class);
            if (client == null) {
                logger.error("Error al convertir documento a User para el cliente ID: {}", clientId);
                throw new CustomExceptions.ProcessingException("Error al procesar información del cliente");
            }

            // Obtener todas las mascotas del cliente con su historial médico
            List<PetDTOs.PetWithHistoryDTO> petsWithHistory = getPetsWithHistory(clientId);

            // Organizar mascotas por última visita
            petsWithHistory.sort((pet1, pet2) -> {
                Date lastVisit1 = getLastVisitDate(pet1.getMedicalHistory());
                Date lastVisit2 = getLastVisitDate(pet2.getMedicalHistory());
                return lastVisit2.compareTo(lastVisit1); // Orden descendente
            });

            // Convertir y retornar el DTO completo
            UserDTOs.ClientWithPetsDTO clientDTO = convertToClientWithPetsDTO(client, petsWithHistory);

            logger.info("Información recuperada exitosamente para cliente ID: {}", clientId);
            return clientDTO;

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error al obtener información del cliente y mascotas: {}", e.getMessage());
            throw new CustomExceptions.ProcessingException("Error al obtener información del cliente: " + e.getMessage());
        }
    }
    // Método auxiliar para filtrar mascotas por fecha de consulta
    private List<PetDTOs.PetWithHistoryDTO> filterPetsByConsultationDate(
            List<PetDTOs.PetWithHistoryDTO> pets, LocalDate consultationDate) {
        return pets.stream()
                .filter(pet -> pet.getMedicalHistory().stream()
                        .anyMatch(record -> isSameDate(record.getDate(), consultationDate)))
                .collect(Collectors.toList());
    }

    // Método auxiliar para verificar si dos fechas son iguales
    private boolean isSameDate(Date date1, LocalDate date2) {
        if (date1 == null || date2 == null) return false;
        LocalDate localDate1 = date1.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate1.equals(date2);
    }

    /**
     * Obtiene la fecha de la última visita del historial médico
     * @param history Lista de registros médicos
     * @return Date con la fecha de la última visita o fecha actual si no hay historial
     */
    private Date getLastVisitDate(List<PetDTOs.MedicalRecordResponse> history) {
        if (history == null || history.isEmpty()) {
            return new Date(0L); // Si no hay historial, retorna la fecha más antigua posible
        }
        return history.stream()
                .map(PetDTOs.MedicalRecordResponse::getDate)
                .max(Date::compareTo)
                .orElse(new Date(0L));
    }

    /**
     * Verifica si una mascota tiene consulta en una fecha específica
     * @param history Historial médico de la mascota
     * @param targetDate Fecha a verificar
     * @return true si hay consulta en esa fecha
     */
    private boolean hasConsultationOnDate(List<PetDTOs.MedicalRecordResponse> history, LocalDate targetDate) {
        if (history == null || history.isEmpty()) {
            return false;
        }

        return history.stream()
                .map(record -> record.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate())
                .anyMatch(date -> date.equals(targetDate));
    }

    /**
     * Agrega información adicional relevante al DTO del cliente
     * @param clientDTO DTO base del cliente
     * @param petsWithHistory Lista de mascotas con historial
     * @return ClientWithPetsDTO enriquecido con información adicional
     */
    private void enrichClientDTO(UserDTOs.ClientWithPetsDTO clientDTO, List<PetDTOs.PetWithHistoryDTO> petsWithHistory) {
        // Calcular estadísticas útiles para el veterinario
        int totalPets = petsWithHistory.size();
        long activePets = petsWithHistory.stream()
                .filter(pet -> !pet.getMedicalHistory().isEmpty())
                .count();

        // Aquí podrías agregar más información relevante al DTO si es necesario
        // Por ejemplo, última visita general, próximas vacunas pendientes, etc.
    }

    /**
     * Valida los permisos del veterinario para acceder a la información
     * @param veterinarianId ID del veterinario
     * @throws CustomExceptions.UnauthorizedException si no tiene permisos
     */
    private void validateVeterinarianAccess(String veterinarianId) {
        try {
            UserDTOs.UserResponse veterinarian = userService.getUserById(veterinarianId);
            if (!veterinarian.getRoles().contains(Role.VETERINARIO)) {
                logger.warn("Intento de acceso no autorizado por usuario ID: {}", veterinarianId);
                throw new CustomExceptions.UnauthorizedException("No tiene permisos para acceder a esta información");
            }
        } catch (CustomExceptions.UserNotFoundException e) {
            logger.error("Veterinario no encontrado ID: {}", veterinarianId);
            throw new CustomExceptions.UnauthorizedException("Usuario no autorizado");
        }
    }
}
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.dto.AppointmentDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Appointment;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReceptionistAppointmentService receptionistAppointmentService;

    private static final long MINIMUM_CANCELLATION_HOURS = 24;
    private static final long MINIMUM_RESCHEDULE_HOURS = 24;
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    /**
     * Obtiene las citas del día para un veterinario específico
     */
    public PaginatedResponse<AppointmentResponse> getVeterinarianDailyAppointments(
            String veterinarianId, Date date, PaginationRequest request) {
        try {
            CollectionReference appointmentsRef = firestore.collection("appointments");

            // Convertir la fecha a LocalDate para comparar solo la fecha sin hora
            LocalDate appointmentDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Obtener el inicio y fin del día
            Date startOfDay = Date.from(appointmentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endOfDay = Date.from(appointmentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Construir query base con filtros de fecha y veterinario
            Query query = appointmentsRef
                    .whereEqualTo("veterinarianId", veterinarianId)
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay);

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

            // Convertir y enriquecer resultados
            List<AppointmentResponse> appointments = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                if (appointment != null) {
                    appointments.add(enrichAppointmentResponse(appointment));
                }
            }

            // Contar total de elementos para este día y veterinario
            long totalElements = appointmentsRef
                    .whereEqualTo("veterinarianId", veterinarianId)
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay)
                    .get().get().size();

            return PaginatedResponse.of(appointments, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error getting daily appointments: " + e.getMessage());
        }
    }
    public PaginatedResponse<AppointmentResponse> getDailyAppointments(
            String veterinarianId, Date date, PaginationRequest request) {
        try {
            CollectionReference appointmentsRef = firestore.collection("appointments");

            // Obtener inicio y fin del día
            LocalDate appointmentDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            Date startOfDay = Date.from(appointmentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endOfDay = Date.from(appointmentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Query base con filtros de fecha y veterinario
            Query query = appointmentsRef
                    .whereEqualTo("veterinarianId", veterinarianId)
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay);

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

            List<AppointmentResponse> appointments = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Appointment appointment = doc.toObject(Appointment.class);
                        try {
                            return enrichAppointmentResponse(appointment);
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            // Contar total de citas para este día y veterinario
            long totalElements = appointmentsRef
                    .whereEqualTo("veterinarianId", veterinarianId)
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay)
                    .get().get().size();

            return PaginatedResponse.of(appointments, request, totalElements);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching daily appointments: " + e.getMessage());
        }
    }

    /**
     * Enriquece la respuesta de la cita con información detallada
     */
    private AppointmentResponse enrichAppointmentResponse(Appointment appointment) throws ExecutionException, InterruptedException {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setReason(appointment.getReason());
        response.setStatus(appointment.getStatus());
        response.setNotes(appointment.getNotes());

        // Obtener información del cliente
        response.setClient(userService.getUserById(appointment.getClientId()));

        // Obtener información del veterinario
        response.setVeterinarian(userService.getUserById(appointment.getVeterinarianId()));

        // Obtener información de la mascota
        response.setPet(petService.getPetById(appointment.getPetId()));

        // Obtener historial médico de la mascota - Agregamos el objeto de paginación
        PaginationRequest defaultPagination = new PaginationRequest();
        defaultPagination.setPage(0);
        defaultPagination.setSize(5); // Limitamos a los últimos 5 registros
        defaultPagination.setSortBy("date");
        defaultPagination.setSortDirection("DESC");

        PaginatedResponse<PetDTOs.MedicalRecordResponse> historyResponse =
                petService.getPetMedicalHistory(appointment.getPetId(), defaultPagination);
        response.setPetHistory(historyResponse.getContent());

        return response;
    }
    /**
     * Obtiene todas las citas de las mascotas del cliente actual
     */
    public PaginatedResponse<AppointmentSummaryByPet> getClientPetsAppointments(
            PaginationRequest paginationRequest) {
        try {
            String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

            // Obtener las mascotas del cliente
            List<PetDTOs.PetResponse> clientPets = petService.getPetsByUserId(clientId);

            List<AppointmentSummaryByPet> summaries = new ArrayList<>();

            for (PetDTOs.PetResponse pet : clientPets) {
                // Consultar citas para cada mascota
                CollectionReference appointmentsRef = firestore.collection("appointments");
                Query query = appointmentsRef
                        .whereEqualTo("petId", pet.getId())
                        .whereGreaterThanOrEqualTo("appointmentDate", new Date());

                // Aplicar filtros si existen
                if (paginationRequest.getFilterBy() != null &&
                        paginationRequest.getFilterValue() != null) {
                    query = query.whereEqualTo(
                            paginationRequest.getFilterBy(),
                            paginationRequest.getFilterValue()
                    );
                }

                // Aplicar ordenamiento
                String sortBy = paginationRequest.getSortBy().equals("id") ?
                        "appointmentDate" : paginationRequest.getSortBy();
                Query.Direction direction = paginationRequest.getSortDirection()
                        .equalsIgnoreCase("DESC") ?
                        Query.Direction.DESCENDING :
                        Query.Direction.ASCENDING;

                query = query.orderBy(sortBy, direction);

                // Aplicar paginación
                query = query
                        .offset(paginationRequest.getPage() * paginationRequest.getSize())
                        .limit(paginationRequest.getSize());

                // Ejecutar query
                QuerySnapshot querySnapshot = query.get().get();

                // Convertir resultados
                List<AppointmentResponse> appointments = querySnapshot.getDocuments()
                        .stream()
                        .map(doc -> {
                            try {
                                Appointment appointment = doc.toObject(Appointment.class);
                                return enrichAppointmentResponse(appointment);
                            } catch (Exception e) {
                                logger.error("Error enriching appointment", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!appointments.isEmpty()) {
                    summaries.add(new AppointmentSummaryByPet(
                            pet,
                            appointments,
                            appointments.size()
                    ));
                }
            }

            // Calcular el total de elementos para la paginación
            long totalElements = summaries.size();

            return PaginatedResponse.of(
                    summaries,
                    paginationRequest,
                    totalElements
            );

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching client pets appointments: " + e.getMessage()
            );
        }
    }

    /**
     * Reprograma una cita existente
     */
    public AppointmentDTOs.AppointmentResponse rescheduleAppointment(
            String appointmentId,
            AppointmentDTOs.RescheduleRequest request
    ) {
        try {
            DocumentReference appointmentRef = firestore.collection("appointments").document(appointmentId);
            DocumentSnapshot appointmentDoc = appointmentRef.get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar tiempo mínimo para reprogramar
            if (!canReschedule(appointment.getAppointmentDate())) {
                throw new CustomExceptions.UnauthorizedException(
                        "Las citas solo pueden reprogramarse con " + MINIMUM_RESCHEDULE_HOURS + " horas de anticipación"
                );
            }

            // Guardar la fecha anterior para la notificación
            Date oldDate = appointment.getAppointmentDate();

            // Actualizar la cita
            appointment.setAppointmentDate(request.getNewDate());
            appointment.setNotes(appointment.getNotes() + "\nReprogramada: " + request.getReason());
            appointment.setUpdatedAt(new Date());

            appointmentRef.set(appointment).get();

            // Enviar notificaciones
            sendRescheduleNotifications(appointment, oldDate, request.getNewDate());

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error rescheduling appointment: " + e.getMessage());
        }
    }

    /**
     * Cancela una cita existente
     */
    public AppointmentDTOs.AppointmentResponse cancelAppointment(String appointmentId) {
        try {
            DocumentReference appointmentRef = firestore.collection("appointments").document(appointmentId);
            DocumentSnapshot appointmentDoc = appointmentRef.get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar tiempo mínimo para cancelar
            if (!canCancel(appointment.getAppointmentDate())) {
                throw new CustomExceptions.UnauthorizedException(
                        "Las citas solo pueden cancelarse con " + MINIMUM_CANCELLATION_HOURS + " horas de anticipación"
                );
            }

            // Actualizar el estado de la cita
            appointment.setStatus("CANCELLED");
            appointment.setUpdatedAt(new Date());

            appointmentRef.set(appointment).get();

            // Enviar notificaciones
            sendCancellationNotifications(appointment);

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error canceling appointment: " + e.getMessage());
        }
    }

    /**
     * Verifica si el usuario autenticado es dueño de la cita
     */
    public boolean isOwner(String appointmentId) {
        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            DocumentSnapshot appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .get();

            if (!appointmentDoc.exists()) {
                return false;
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);
            return appointment != null && appointment.getClientId().equals(currentUserId);
        } catch (Exception e) {
            //logger.error("Error checking appointment ownership: {}", e.getMessage());
            return false;
        }
    }

    private boolean canReschedule(Date appointmentDate) {
        return getHoursDifference(appointmentDate) >= MINIMUM_RESCHEDULE_HOURS;
    }

    private boolean canCancel(Date appointmentDate) {
        return getHoursDifference(appointmentDate) >= MINIMUM_CANCELLATION_HOURS;
    }

    private long getHoursDifference(Date appointmentDate) {
        long diffInMillies = appointmentDate.getTime() - new Date().getTime();
        return diffInMillies / (60 * 60 * 1000);
    }

    private void sendRescheduleNotifications(Appointment appointment, Date oldDate, Date newDate) {
        // Notificar al cliente
        notificationService.sendAppointmentRescheduledNotification(
                appointment.getClientId(),
                appointment.getPetId(),
                oldDate,
                newDate
        );

        // Notificar al veterinario
        notificationService.sendVeterinarianAppointmentRescheduledNotification(
                appointment.getVeterinarianId(),
                appointment.getPetId(),
                oldDate,
                newDate
        );
    }

    private void sendCancellationNotifications(Appointment appointment) {
        // Notificar al cliente
        notificationService.sendAppointmentCancelledNotification(
                appointment.getClientId(),
                appointment.getPetId(),
                appointment.getAppointmentDate()
        );

        // Notificar al veterinario
        notificationService.sendVeterinarianAppointmentCancelledNotification(
                appointment.getVeterinarianId(),
                appointment.getPetId(),
                appointment.getAppointmentDate()
        );
    }
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        //String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Validar que la mascota pertenece al cliente
            /*
            PetDTOs.PetResponse pet = petService.getPetById(request.getPetId());
            if (!pet.getOwnerId().equals(clientId)) {
                throw new CustomExceptions.UnauthorizedException("No tienes permiso para agendar citas para esta mascota");
            }
            */


            // Validar que la fecha es futura
            if (request.getAppointmentDate().before(new Date())) {
                throw new IllegalArgumentException("La fecha de la cita debe ser futura");
            }

            // Crear la cita
            Appointment appointment = Appointment.builder()
                    .id(UUID.randomUUID().toString())
                    .petId(request.getPetId())
                    .clientId(request.getClientId())
                    .veterinarianId(request.getVeterinarianId())
                    .appointmentDate(request.getAppointmentDate())
                    .reason(request.getReason())
                    .status("SCHEDULED")
                    .notes(request.getNotes())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();

            // Guardar en Firestore
            firestore.collection("appointments")
                    .document(appointment.getId())
                    .set(appointment)
                    .get();

            // Enviar notificaciones
            sendNewAppointmentNotifications(appointment);

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error creating appointment: " + e.getMessage());
        }
    }

    private void sendNewAppointmentNotifications(Appointment appointment) {
        try {
            // Notificar al cliente
            UserDTOs.UserResponse client = userService.getUserById(appointment.getClientId());
            PetDTOs.PetResponse pet = petService.getPetById(appointment.getPetId());
            UserDTOs.UserResponse vet = userService.getUserById(appointment.getVeterinarianId());

            String clientSubject = "Nueva cita programada para " + pet.getName();
            String clientContent = notificationService.generateNewAppointmentEmail(pet.getName(),
                    appointment.getAppointmentDate(),
                    vet.getNombre() + " " + vet.getApellido());

            notificationService.sendEmail(client.getEmail(), clientSubject, clientContent);

            // Notificar al veterinario
            String vetSubject = "Nueva cita programada";
            String vetContent = notificationService.generateNewAppointmentVetEmail(pet.getName(),
                    appointment.getAppointmentDate(),
                    client.getNombre() + " " + client.getApellido());

            notificationService.sendEmail(vet.getEmail(), vetSubject, vetContent);
        } catch (Exception e) {
            //log.error("Error sending new appointment notifications: {}", e.getMessage());
        }
    }
    public PaginatedResponse<DailyAppointmentDTO> getDailyAppointmentsForReceptionist(
            Date date,
            PaginationRequest request) {
        return receptionistAppointmentService.getDailyAppointmentsForReceptionist(date, request);
    }
}
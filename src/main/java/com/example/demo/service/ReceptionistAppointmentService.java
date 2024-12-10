package com.example.demo.service;

import com.example.demo.dto.DailyAppointmentDTO;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Appointment;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.ZoneOffset;

@Service
public class ReceptionistAppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(ReceptionistAppointmentService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    /**
     * Obtiene las citas para un día específico con paginación
     * @param date fecha para la cual obtener las citas
     * @param request parámetros de paginación
     * @return lista paginada de citas
     */
    public PaginatedResponse<DailyAppointmentDTO> getDailyAppointmentsForReceptionist(Date date, PaginationRequest request) {
        try {
            // Convertir Date a LocalDate en UTC para evitar problemas de zona horaria
            LocalDate localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();

            // Obtener inicio y fin del día en UTC
            Date startOfDay = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            Date endOfDay = Date.from(localDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

            // Construir query base
            CollectionReference appointmentsRef = firestore.collection("appointments");
            Query query = appointmentsRef
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay);

            // Aplicar ordenamiento
            Query.Direction direction = request.getSortDirection() != null &&
                    request.getSortDirection().equalsIgnoreCase("DESC") ?
                    Query.Direction.DESCENDING : Query.Direction.ASCENDING;

            query = query.orderBy("appointmentDate", direction);

            // Aplicar paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<DailyAppointmentDTO> appointments = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                if (appointment != null) {
                    appointments.add(enrichAppointmentData(appointment));
                }
            }

            // Obtener el total de elementos para este día
            long totalElements = getTotalAppointmentsForDay(appointmentsRef, startOfDay, endOfDay);

            // Crear y retornar respuesta paginada
            return new PaginatedResponse<>(
                    appointments,
                    request.getPage(),
                    request.getSize(),
                    totalElements,
                    (int) Math.ceil((double) totalElements / request.getSize())
            );

        } catch (Exception e) {
            logger.error("Error getting daily appointments: ", e);
            throw new CustomExceptions.ProcessingException("Error fetching appointments: " + e.getMessage());
        }
    }

    /**
     * Enriquece los datos de la cita con información adicional
     * @param appointment cita base
     * @return DTO con información completa
     */
    private DailyAppointmentDTO enrichAppointmentData(Appointment appointment) {
        try {
            // Obtener información del cliente
            var client = userService.getUserById(appointment.getClientId());

            // Obtener información de la mascota
            var pet = petService.getPetById(appointment.getPetId());

            // Obtener información del veterinario
            var veterinarian = userService.getUserById(appointment.getVeterinarianId());

            // Construir DTO
            return DailyAppointmentDTO.builder()
                    .id(appointment.getId())
                    .appointmentTime(appointment.getAppointmentDate())
                    // Información del cliente
                    .clientId(client.getUid())
                    .clientName(client.getNombre() + " " + client.getApellido())
                    .clientEmail(client.getEmail())
                    .clientPhone(client.getTelefono())
                    // Información de la mascota
                    .petId(pet.getId())
                    .petName(pet.getName())
                    .petSpecies(pet.getSpecies())
                    .petBreed(pet.getBreed())
                    // Información del veterinario
                    .veterinarianId(veterinarian.getUid())
                    .veterinarianName(veterinarian.getNombre() + " " + veterinarian.getApellido())
                    // Estado y razón
                    .status(appointment.getStatus())
                    .reason(appointment.getReason())
                    // Permisos
                    .canCancel(canCancelAppointment(appointment))
                    .canReschedule(canRescheduleAppointment(appointment))
                    .build();

        } catch (Exception e) {
            logger.error("Error enriching appointment data: ", e);
            throw new CustomExceptions.ProcessingException("Error processing appointment data: " + e.getMessage());
        }
    }

    /**
     * Obtiene el total de citas para un día específico
     */
    private long getTotalAppointmentsForDay(CollectionReference appointmentsRef,
                                            Date startOfDay,
                                            Date endOfDay) throws Exception {
        return appointmentsRef
                .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                .whereLessThan("appointmentDate", endOfDay)
                .get()
                .get()
                .size();
    }

    /**
     * Verifica si una cita puede ser cancelada
     */
    private boolean canCancelAppointment(Appointment appointment) {
        // Verificar que la cita no esté ya cancelada o completada
        if (appointment.getStatus().equals("CANCELLED") ||
                appointment.getStatus().equals("COMPLETED")) {
            return false;
        }

        // Verificar que la cita sea en el futuro
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime now = LocalDateTime.now();

        // Permitir cancelaciones hasta 24 horas antes
        return appointmentDateTime.minusHours(24).isAfter(now);
    }

    /**
     * Verifica si una cita puede ser reprogramada
     */
    private boolean canRescheduleAppointment(Appointment appointment) {
        // Aplicar las mismas reglas que para cancelación
        return canCancelAppointment(appointment);
    }
}
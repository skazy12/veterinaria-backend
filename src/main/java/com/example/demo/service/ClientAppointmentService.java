package com.example.demo.service;

import com.example.demo.dto.AppointmentDTOs;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.UserDTOs;
import com.example.demo.dto.PetDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Appointment;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class ClientAppointmentService {

    private static final int MINIMUM_HOURS_BEFORE_CANCEL = 24;
    private static final int MINIMUM_HOURS_BEFORE_RESCHEDULE = 24;

    @Autowired
    private Firestore firestore;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    /**
     * Obtiene todas las citas del cliente actual
     */
    public PaginatedResponse<AppointmentDTOs.ClientPetAppointmentDTO> getClientAppointments(PaginationRequest request) {
        String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Crear referencia a la colección de citas
            CollectionReference appointmentsRef = firestore.collection("appointments");

            // Construir query base
            Query query = appointmentsRef
                    .whereEqualTo("clientId", clientId)
                    .whereGreaterThanOrEqualTo("appointmentDate", new Date());

            // Aplicar ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;

            query = query.orderBy("appointmentDate", direction);

            // Aplicar paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<AppointmentDTOs.ClientPetAppointmentDTO> appointments = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                if (appointment != null) {
                    appointments.add(convertToDTO(appointment));
                }
            }

            // Obtener total de elementos
            long totalElements = appointmentsRef
                    .whereEqualTo("clientId", clientId)
                    .whereGreaterThanOrEqualTo("appointmentDate", new Date())
                    .get().get().size();

            return PaginatedResponse.of(appointments, request, totalElements);

        } catch (Exception e) {
            log.error("Error fetching client appointments: {}", e.getMessage());
            throw new CustomExceptions.ProcessingException("Error fetching appointments");
        }
    }

    /**
     * Cancela una cita verificando las políticas establecidas
     */
    public AppointmentDTOs.ClientPetAppointmentDTO cancelAppointment(String appointmentId) {
        String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Obtener la cita
            DocumentSnapshot appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar que la cita pertenece al cliente
            if (!appointment.getClientId().equals(clientId)) {
                throw new CustomExceptions.UnauthorizedException("Unauthorized to cancel this appointment");
            }

            // Verificar tiempo mínimo para cancelación
            if (!canCancel(appointment.getAppointmentDate())) {
                throw new CustomExceptions.ProcessingException(
                        "Appointments can only be cancelled " + MINIMUM_HOURS_BEFORE_CANCEL + " hours in advance");
            }

            // Actualizar estado
            appointment.setStatus("CANCELLED");
            appointmentDoc.getReference().set(appointment).get();

            // Enviar notificaciones
            sendCancellationNotifications(appointment);

            return convertToDTO(appointment);

        } catch (Exception e) {
            log.error("Error cancelling appointment: {}", e.getMessage());
            throw new CustomExceptions.ProcessingException("Error cancelling appointment");
        }
    }

    /**
     * Reprograma una cita verificando las políticas establecidas
     */
    public AppointmentDTOs.ClientPetAppointmentDTO rescheduleAppointment(String appointmentId,
                                                                         AppointmentDTOs.RescheduleRequest request) {
        String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Verificar que la nueva fecha es futura
            if (request.getNewDate().before(new Date())) {
                throw new CustomExceptions.ProcessingException("New date must be in the future");
            }

            // Obtener la cita
            DocumentSnapshot appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar que la cita pertenece al cliente
            if (!appointment.getClientId().equals(clientId)) {
                throw new CustomExceptions.UnauthorizedException(
                        "Unauthorized to reschedule this appointment");
            }

            // Verificar tiempo mínimo para reprogramación
            if (!canReschedule(appointment.getAppointmentDate())) {
                throw new CustomExceptions.ProcessingException(
                        "Appointments can only be rescheduled " +
                                MINIMUM_HOURS_BEFORE_RESCHEDULE + " hours in advance");
            }

            // Guardar fecha anterior para notificación
            Date oldDate = appointment.getAppointmentDate();

            // Actualizar fecha y motivo
            appointment.setAppointmentDate(request.getNewDate());
            appointment.setReason(request.getReason()); // Aquí se asigna el motivo
            appointmentDoc.getReference().set(appointment).get();


            // Enviar notificaciones
            sendRescheduleNotifications(appointment, oldDate);

            return convertToDTO(appointment);

        } catch (Exception e) {
            log.error("Error rescheduling appointment: {}", e.getMessage());
            throw new CustomExceptions.ProcessingException("Error rescheduling appointment");
        }
    }


    private AppointmentDTOs.ClientPetAppointmentDTO convertToDTO(Appointment appointment)
            throws ExecutionException, InterruptedException {
        // Obtener información del veterinario
        UserDTOs.UserResponse vet = userService.getUserById(appointment.getVeterinarianId());

        return AppointmentDTOs.ClientPetAppointmentDTO.builder()
                .id(appointment.getId())
                .appointmentDate(appointment.getAppointmentDate())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .veterinarianName(vet.getNombre() + " " + vet.getApellido())
                .pet(petService.getPetById(appointment.getPetId()))
                .canCancel(canCancel(appointment.getAppointmentDate()))
                .canReschedule(canReschedule(appointment.getAppointmentDate()))
                .build();
    }

    private boolean canCancel(Date appointmentDate) {
        long hoursDifference = getHoursDifference(appointmentDate);
        return hoursDifference >= MINIMUM_HOURS_BEFORE_CANCEL;
    }

    private boolean canReschedule(Date appointmentDate) {
        long hoursDifference = getHoursDifference(appointmentDate);
        return hoursDifference >= MINIMUM_HOURS_BEFORE_RESCHEDULE;
    }

    private long getHoursDifference(Date appointmentDate) {
        long diffInMillies = appointmentDate.getTime() - new Date().getTime();
        return diffInMillies / (60 * 60 * 1000);
    }

    private void sendCancellationNotifications(Appointment appointment)
            throws ExecutionException, InterruptedException {
        // Obtener datos necesarios
        UserDTOs.UserResponse client = userService.getUserById(appointment.getClientId());
        UserDTOs.UserResponse vet = userService.getUserById(appointment.getVeterinarianId());
        PetDTOs.PetResponse pet = petService.getPetById(appointment.getPetId());

        // Notificar al cliente
        String clientSubject = "Cita Cancelada - " + pet.getName();
        String clientContent = generateCancellationEmailContent(
                pet.getName(),
                appointment.getAppointmentDate(),
                vet.getNombre() + " " + vet.getApellido()
        );
        notificationService.sendEmail(client.getEmail(), clientSubject, clientContent);

        // Notificar al veterinario
        String vetSubject = "Cita Cancelada por Cliente";
        String vetContent = generateVetCancellationEmailContent(
                pet.getName(),
                client.getNombre() + " " + client.getApellido(),
                appointment.getAppointmentDate()
        );
        notificationService.sendEmail(vet.getEmail(), vetSubject, vetContent);
    }

    private void sendRescheduleNotifications(Appointment appointment, Date oldDate)
            throws ExecutionException, InterruptedException {
        // Obtener datos necesarios
        UserDTOs.UserResponse client = userService.getUserById(appointment.getClientId());
        UserDTOs.UserResponse vet = userService.getUserById(appointment.getVeterinarianId());
        PetDTOs.PetResponse pet = petService.getPetById(appointment.getPetId());

        // Notificar al cliente
        String clientSubject = "Cita Reprogramada - " + pet.getName();
        String clientContent = generateRescheduleEmailContent(
                pet.getName(),
                oldDate,
                appointment.getAppointmentDate(),
                vet.getNombre() + " " + vet.getApellido()
        );
        notificationService.sendEmail(client.getEmail(), clientSubject, clientContent);

        // Notificar al veterinario
        String vetSubject = "Cita Reprogramada por Cliente";
        String vetContent = generateVetRescheduleEmailContent(
                pet.getName(),
                client.getNombre() + " " + client.getApellido(),
                oldDate,
                appointment.getAppointmentDate()
        );
        notificationService.sendEmail(vet.getEmail(), vetSubject, vetContent);
    }

    // Templates para los correos
    private String generateVetCancellationEmailContent(String petName, String clientName, Date date) {
        return String.format("""
        <html>
        <body>
            <h2>Notificación de Cancelación de Cita</h2>
            <p>Una cita ha sido cancelada por el cliente.</p>
            <p>Detalles de la cita cancelada:</p>
            <ul>
                <li><strong>Mascota:</strong> %s</li>
                <li><strong>Cliente:</strong> %s</li>
                <li><strong>Fecha cancelada:</strong> %s</li>
            </ul>
            <p>El espacio en su agenda ha sido liberado y está disponible para otras citas.</p>
            <hr>
            <p style="color: #666; font-size: 0.9em;">
                Este es un mensaje automático del sistema de gestión de citas veterinarias.
            </p>
        </body>
        </html>
        """,
                petName,
                clientName,
                formatDate(date)
        );
    }

    private String generateVetRescheduleEmailContent(String petName, String clientName,
                                                     Date oldDate, Date newDate) {
        return String.format("""
        <html>
        <body>
            <h2>Notificación de Reprogramación de Cita</h2>
            <p>Una cita ha sido reprogramada por el cliente.</p>
            <p>Detalles de la reprogramación:</p>
            <ul>
                <li><strong>Mascota:</strong> %s</li>
                <li><strong>Cliente:</strong> %s</li>
                <li><strong>Fecha anterior:</strong> %s</li>
                <li><strong>Nueva fecha:</strong> %s</li>
            </ul>
            <p>Por favor, verifique que la nueva fecha se ajuste a su disponibilidad.</p>
            <p style="color: #E74C3C;">
                Si existe algún inconveniente con la nueva fecha, 
                por favor contacte al cliente lo antes posible.
            </p>
            <hr>
            <p style="color: #666; font-size: 0.9em;">
                Este es un mensaje automático del sistema de gestión de citas veterinarias.
            </p>
        </body>
        </html>
        """,
                petName,
                clientName,
                formatDate(oldDate),
                formatDate(newDate)
        );
    }
    // Templates para los correos
    private String generateCancellationEmailContent(String petName, Date date, String vetName) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Cancelada</h2>
                <p>Tu cita para %s ha sido cancelada.</p>
                <p>Detalles de la cita:</p>
                <ul>
                    <li><strong>Fecha:</strong> %s</li>
                    <li><strong>Veterinario:</strong> Dr. %s</li>
                </ul>
                <p>Si deseas programar una nueva cita, por favor contáctanos.</p>
            </body>
            </html>
            """,
                petName,
                formatDate(date),
                vetName
        );
    }

    private String generateRescheduleEmailContent(String petName, Date oldDate,
                                                  Date newDate, String vetName) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Reprogramada</h2>
                <p>Tu cita para %s ha sido reprogramada.</p>
                <p>Detalles de la cita:</p>
                <ul>
                    <li><strong>Fecha anterior:</strong> %s</li>
                    <li><strong>Nueva fecha:</strong> %s</li>
                    <li><strong>Veterinario:</strong> Dr. %s</li>
                </ul>
                <p>Si necesitas hacer algún cambio adicional, por favor contáctanos.</p>
            </body>
            </html>
            """,
                petName,
                formatDate(oldDate),
                formatDate(newDate),
                vetName
        );
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
}
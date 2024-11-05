package com.example.demo.service;
import com.example.demo.model.*;
import com.example.demo.dto.*;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReminderService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private NotificationService notificationService;

    private static final String DEFAULT_TEMPLATE = """
        <html>
        <body>
            <h2>Recordatorio de Cita Veterinaria</h2>
            <p>Estimado/a %s,</p>
            <p>Le recordamos su cita programada para:</p>
            <ul>
                <li><strong>Fecha:</strong> %s</li>
                <li><strong>Mascota:</strong> %s</li>
                <li><strong>Veterinario:</strong> Dr. %s</li>
            </ul>
            <p>Por favor, confirme su asistencia haciendo clic en el siguiente enlace:</p>
            <a href="%s" style="padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;">
                Confirmar Asistencia
            </a>
            <p>Si necesita reprogramar su cita, por favor contáctenos lo antes posible.</p>
        </body>
        </html>
    """;

    // Ejecutar cada hora para verificar próximas citas
    @Scheduled(fixedRate = 3600000)
    public void checkUpcomingAppointments() {
        try {
            AppointmentReminderConfig config = getConfig();
            if (!config.isEnabled()) {
                return;
            }

            // Calcular el rango de tiempo para las citas que necesitan recordatorio
            Date now = new Date();
            Date reminderTime = new Date(now.getTime() + (config.getReminderHoursBefore() * 3600000));

            // Obtener citas próximas que aún no tienen recordatorio enviado
            var appointments = firestore.collection("appointments")
                    .whereEqualTo("status", "SCHEDULED")
                    .whereEqualTo("reminderSent", false)
                    .whereLessThan("appointmentDate", reminderTime)
                    .get()
                    .get();

            for (var doc : appointments.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                sendReminder(appointment, config);

                // Marcar recordatorio como enviado
                doc.getReference().update("reminderSent", true);
            }

        } catch (Exception e) {
            logger.error("Error checking upcoming appointments: {}", e.getMessage());
        }
    }

    public void sendReminder(Appointment appointment, AppointmentReminderConfig config) {
        try {
            // Obtener información necesaria
            var clientDoc = firestore.collection("users").document(appointment.getClientId()).get().get();
            var petDoc = firestore.collection("pets").document(appointment.getPetId()).get().get();
            var vetDoc = firestore.collection("users").document(appointment.getVeterinarianId()).get().get();

            User client = clientDoc.toObject(User.class);
            Pet pet = petDoc.toObject(Pet.class);
            User vet = vetDoc.toObject(User.class);

            // Generar enlace de confirmación único
            String confirmationLink = generateConfirmationLink(appointment.getId());

            // Generar contenido del correo
            String emailContent = String.format(
                    config.getEmailTemplate() != null ? config.getEmailTemplate() : DEFAULT_TEMPLATE,
                    client.getNombre(),
                    formatDate(appointment.getAppointmentDate()),
                    pet.getName(),
                    vet.getNombre() + " " + vet.getApellido(),
                    confirmationLink
            );

            // Enviar correo
            notificationService.sendEmail(
                    client.getEmail(),
                    "Recordatorio de Cita Veterinaria - " + pet.getName(),
                    emailContent
            );

            logger.info("Reminder sent for appointment ID: {}", appointment.getId());
        } catch (Exception e) {
            logger.error("Error sending reminder for appointment {}: {}",
                    appointment.getId(), e.getMessage());
        }
    }

    private String generateConfirmationLink(String appointmentId) {
        // Generar un token seguro para la confirmación
        String token = UUID.randomUUID().toString();
        String baseUrl = "http://tu-dominio.com/api/appointments/confirm";
        return String.format("%s?id=%s&token=%s", baseUrl, appointmentId, token);
    }

    public void updateReminderConfig(ReminderDTO config) {
        try {
            AppointmentReminderConfig reminderConfig = AppointmentReminderConfig.builder()
                    .id("default")
                    .reminderHoursBefore(config.getReminderHoursBefore())
                    .isEnabled(config.isEnabled())
                    .emailTemplate(config.getEmailTemplate())
                    .build();

            firestore.collection("configurations")
                    .document("reminderConfig")
                    .set(reminderConfig)
                    .get();

            logger.info("Reminder configuration updated successfully");
        } catch (Exception e) {
            logger.error("Error updating reminder configuration: {}", e.getMessage());
            throw new RuntimeException("Error updating reminder configuration");
        }
    }

    public void confirmAppointment(String appointmentId, String token) {
        try {
            // Verificar token y actualizar estado de la cita
            var appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .get();

            if (!appointmentDoc.exists()) {
                throw new RuntimeException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);
            appointment.setStatus(AppointmentStatus.CONFIRMED.toString());

            firestore.collection("appointments")
                    .document(appointmentId)
                    .set(appointment)
                    .get();

            logger.info("Appointment {} confirmed successfully", appointmentId);
        } catch (Exception e) {
            logger.error("Error confirming appointment {}: {}", appointmentId, e.getMessage());
            throw new RuntimeException("Error confirming appointment");
        }
    }

    private AppointmentReminderConfig getConfig() throws ExecutionException, InterruptedException {
        var configDoc = firestore.collection("configurations")
                .document("reminderConfig")
                .get()
                .get();

        if (!configDoc.exists()) {
            // Crear configuración por defecto
            AppointmentReminderConfig defaultConfig = AppointmentReminderConfig.builder()
                    .id("default")
                    .reminderHoursBefore(24) // 24 horas por defecto
                    .isEnabled(true)
                    .emailTemplate(DEFAULT_TEMPLATE)
                    .build();

            configDoc.getReference().set(defaultConfig).get();
            return defaultConfig;
        }

        return configDoc.toObject(AppointmentReminderConfig.class);
    }

    private String formatDate(Date date) {
        return date.toString(); // Implementar formato deseado
    }
}

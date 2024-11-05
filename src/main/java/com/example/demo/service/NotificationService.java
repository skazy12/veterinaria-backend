package com.example.demo.service;


import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs;
import com.example.demo.model.AlertStatus;
import com.example.demo.model.LowStockAlert;
import com.example.demo.model.Role;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private Firestore firestore;

    @Autowired
    private PetService petService;

    @Autowired
    private UserService userService;

    public void sendLowStockAlert(LowStockAlert alert) {
        try {
            // Obtener emails de recepcionistas
            List<String> recipientEmails = getUserEmailsByRole(Role.RECEPCIONISTA);

            String subject = "Alerta de Stock Bajo: " + alert.getProductName();
            String content = generateLowStockEmailContent(alert);

            // Enviar email a cada recepcionista
            for (String email : recipientEmails) {
                sendEmail(email, subject, content);
            }
        } catch (Exception e) {
            log.error("Error sending low stock alert: {}", e.getMessage());
        }
    }

    void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            log.info("Enviando correo...");
            emailSender.send(message);
            log.info("Alert email sent to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage());
        }
    }

    private String generateLowStockEmailContent(LowStockAlert alert) {
        return """
            <html>
            <body>
                <h2>Alerta de Stock Bajo</h2>
                <div style="background-color: %s; padding: 20px; border-radius: 5px;">
                    <h3>Producto: %s</h3>
                    <p>Stock Actual: %d</p>
                    <p>Stock Mínimo: %d</p>
                    <p>Estado: %s</p>
                </div>
                <p>Se requiere realizar un nuevo pedido.</p>
                <a href="http://tu-aplicacion.com/inventory/restock/%s">Crear Orden de Reabastecimiento</a>
            </body>
            </html>
            """.formatted(
                alert.getStatus() == AlertStatus.CRITICAL ? "#ffebee" : "#fff3e0",
                alert.getProductName(),
                alert.getCurrentStock(),
                alert.getMinThreshold(),
                alert.getStatus(),
                alert.getProductId()
        );
    }
    /**
     * Obtiene los emails de los usuarios que tienen un rol específico
     */
    public List<String> getUserEmailsByRole(Role role) {
        try {
            // Consultar todos los usuarios con el rol especificado
            QuerySnapshot querySnapshot = firestore.collection("users")
                    .whereArrayContains("roles", role)
                    .get()
                    .get();

            // Extraer y retornar los emails
            return querySnapshot.getDocuments().stream()
                    .map(doc -> doc.getString("email"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error obteniendo emails para el rol {}: {}", role, e.getMessage());
            return new ArrayList<>(); // Retornar lista vacía en caso de error
        }
    }
    //PARTE DE CITAS
    public void sendAppointmentRescheduledNotification(String clientId, String petId, Date oldDate, Date newDate) {
        try {
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            UserDTOs.UserResponse client = userService.getUserById(clientId);

            String subject = "Cita reprogramada para " + pet.getName();
            String content = generateRescheduledAppointmentEmail(pet.getName(), oldDate, newDate);

            sendEmail(client.getEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error sending appointment rescheduled notification: {}", e.getMessage());
        }
    }

    public void sendVeterinarianAppointmentRescheduledNotification(String veterinarianId, String petId, Date oldDate, Date newDate) {
        try {
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            UserDTOs.UserResponse vet = userService.getUserById(veterinarianId);

            String subject = "Cita reprogramada - " + pet.getName();
            String content = generateVetRescheduledAppointmentEmail(pet.getName(), oldDate, newDate);

            sendEmail(vet.getEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error sending vet appointment rescheduled notification: {}", e.getMessage());
        }
    }

    public void sendAppointmentCancelledNotification(String clientId, String petId, Date date) {
        try {
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            UserDTOs.UserResponse client = userService.getUserById(clientId);

            String subject = "Cita cancelada - " + pet.getName();
            String content = generateCancelledAppointmentEmail(pet.getName(), date);

            sendEmail(client.getEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error sending appointment cancelled notification: {}", e.getMessage());
        }
    }

    public void sendVeterinarianAppointmentCancelledNotification(String veterinarianId, String petId, Date date) {
        try {
            PetDTOs.PetResponse pet = petService.getPetById(petId);
            UserDTOs.UserResponse vet = userService.getUserById(veterinarianId);

            String subject = "Cita cancelada - " + pet.getName();
            String content = generateVetCancelledAppointmentEmail(pet.getName(), date);

            sendEmail(vet.getEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error sending vet appointment cancelled notification: {}", e.getMessage());
        }
    }

    private String generateRescheduledAppointmentEmail(String petName, Date oldDate, Date newDate) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Reprogramada</h2>
                <p>La cita para %s ha sido reprogramada:</p>
                <ul>
                    <li><strong>Fecha anterior:</strong> %s</li>
                    <li><strong>Nueva fecha:</strong> %s</li>
                </ul>
                <p>Si necesita hacer algún cambio adicional, por favor contáctenos.</p>
            </body>
            </html>
            """,
                petName,
                formatDate(oldDate),
                formatDate(newDate)
        );
    }

    private String generateVetRescheduledAppointmentEmail(String petName, Date oldDate, Date newDate) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Reprogramada - Actualización</h2>
                <p>Una cita ha sido reprogramada:</p>
                <ul>
                    <li><strong>Paciente:</strong> %s</li>
                    <li><strong>Fecha anterior:</strong> %s</li>
                    <li><strong>Nueva fecha:</strong> %s</li>
                </ul>
            </body>
            </html>
            """,
                petName,
                formatDate(oldDate),
                formatDate(newDate)
        );
    }

    private String generateCancelledAppointmentEmail(String petName, Date date) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Cancelada</h2>
                <p>La cita para %s programada para el %s ha sido cancelada.</p>
                <p>Si desea programar una nueva cita, por favor contáctenos.</p>
            </body>
            </html>
            """,
                petName,
                formatDate(date)
        );
    }

    private String generateVetCancelledAppointmentEmail(String petName, Date date) {
        return String.format("""
            <html>
            <body>
                <h2>Cita Cancelada - Notificación</h2>
                <p>La siguiente cita ha sido cancelada:</p>
                <ul>
                    <li><strong>Paciente:</strong> %s</li>
                    <li><strong>Fecha:</strong> %s</li>
                </ul>
            </body>
            </html>
            """,
                petName,
                formatDate(date)
        );
    }
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    String generateNewAppointmentEmail(String petName, Date appointmentDate, String vetName) {
        return String.format("""
        <html>
        <body>
            <h2>Nueva Cita Programada</h2>
            <p>Se ha programado una nueva cita para %s:</p>
            <ul>
                <li><strong>Fecha:</strong> %s</li>
                <li><strong>Veterinario:</strong> Dr. %s</li>
            </ul>
            <p>Por favor, llegue 10 minutos antes de la hora programada.</p>
        </body>
        </html>
        """,
                petName,
                formatDate(appointmentDate),
                vetName
        );
    }

    String generateNewAppointmentVetEmail(String petName, Date appointmentDate, String clientName) {
        return String.format("""
        <html>
        <body>
            <h2>Nueva Cita Programada</h2>
            <p>Se ha programado una nueva cita:</p>
            <ul>
                <li><strong>Paciente:</strong> %s</li>
                <li><strong>Dueño:</strong> %s</li>
                <li><strong>Fecha:</strong> %s</li>
            </ul>
        </body>
        </html>
        """,
                petName,
                clientName,
                formatDate(appointmentDate)
        );
    }
}
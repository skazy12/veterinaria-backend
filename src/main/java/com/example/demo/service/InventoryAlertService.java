package com.example.demo.service;

import com.example.demo.dto.InventoryDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.*;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryAlertService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private NotificationService notificationService;

    /**
     * Verifica periódicamente los niveles de stock
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void checkInventoryLevels() {
        try {
            QuerySnapshot snapshot = firestore.collection("inventory").get().get();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                InventoryItem item = doc.toObject(InventoryItem.class);
                if (item != null && isLowStock(item)) {
                    createStockAlert(item);
                }
            }
        } catch (Exception e) {
            log.error("Error checking inventory levels: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todos los productos con stock bajo
     */
    public List<InventoryDTOs.LowStockAlertDTO> getLowStockAlerts() {
        try {
            List<InventoryDTOs.LowStockAlertDTO> alerts = new ArrayList<>();
            QuerySnapshot snapshot = firestore.collection("inventory")
                    .get().get();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                InventoryItem item = doc.toObject(InventoryItem.class);
                if (item != null && isLowStock(item)) {
                    alerts.add(createAlertDTO(item));
                }
            }
            return alerts;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching low stock alerts");
        }
    }

    /**
     * Actualiza el umbral mínimo de un producto
     */
    public InventoryDTOs.InventoryItemResponse updateThreshold(String productId, int newThreshold) {
        try {
            if (newThreshold < 0) {
                throw new IllegalArgumentException("Threshold cannot be negative");
            }

            DocumentReference docRef = firestore.collection("inventory")
                    .document(productId);
            InventoryItem item = docRef.get().get().toObject(InventoryItem.class);

            if (item == null) {
                throw new CustomExceptions.NotFoundException("Product not found");
            }

            item.setMinThreshold(newThreshold);
            docRef.set(item).get();

            // Verificar si necesita generar alerta con el nuevo umbral
            if (isLowStock(item)) {
                createStockAlert(item);
            }

            return convertToResponse(item);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error updating threshold");
        }
    }

    /**
     * Crea una orden de reabastecimiento
     */
    public InventoryDTOs.RestockOrderDTO createRestockOrder(InventoryDTOs.CreateRestockOrderRequest request) {
        try {
            String currentUserId = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            RestockOrder order = RestockOrder.builder()
                    .id(UUID.randomUUID().toString())
                    .productId(request.getProductId())
                    .quantityToOrder(request.getQuantity())
                    .status(InventoryDTOs.OrderStatus.PENDING)
                    .orderDate(new Date())
                    .requestedBy(currentUserId)
                    .notes(request.getNotes())
                    .build();

            firestore.collection("restockOrders")
                    .document(order.getId())
                    .set(order)
                    .get();

            notifyRestockOrderCreated(order);
            return convertToRestockDTO(order);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error creating restock order");
        }
    }

    private boolean isLowStock(InventoryItem item) {
        return item.getQuantity() <= item.getMinThreshold();
    }

    private void createStockAlert(InventoryItem item) {
        try {
            LowStockAlert alert = LowStockAlert.builder()
                    .id(UUID.randomUUID().toString())
                    .productId(item.getId())
                    .productName(item.getName())
                    .currentStock(item.getQuantity())
                    .minThreshold(item.getMinThreshold())
                    .status(calculateAlertStatus(item))
                    .createdAt(new Date())
                    .isAcknowledged(false)
                    .build();

            // Guardar alerta
            firestore.collection("lowStockAlerts")
                    .document(alert.getId())
                    .set(alert)
                    .get();

            // Enviar notificación
            notificationService.sendLowStockAlert(alert);
        } catch (Exception e) {
            log.error("Error creating stock alert: {}", e.getMessage());
        }
    }

    private AlertStatus calculateAlertStatus(InventoryItem item) {
        double ratio = (double) item.getQuantity() / item.getMinThreshold();
        return ratio <= 0.5 ? AlertStatus.CRITICAL : AlertStatus.WARNING;
    }
    private InventoryDTOs.LowStockAlertDTO createAlertDTO(InventoryItem item) {
        return InventoryDTOs.LowStockAlertDTO.builder()
                .id(UUID.randomUUID().toString())
                .productId(item.getId())
                .productName(item.getName())
                .currentStock(item.getQuantity())
                .minThreshold(item.getMinThreshold())
                .status(calculateAlertStatus(item))
                .createdAt(new Date())
                .isAcknowledged(false)
                .build();
    }

    /**
     * Convierte un InventoryItem a InventoryItemResponse
     */
    private InventoryDTOs.InventoryItemResponse convertToResponse(InventoryItem item) {
        return InventoryDTOs.InventoryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .minThreshold(item.getMinThreshold())
                .recommendedOrderQuantity(calculateRecommendedOrderQuantity(item))
                .dateAdded(item.getDateAdded())
                .lastUpdated(new Date())
                .status(calculateAlertStatus(item).toString())
                .needsReorder(isLowStock(item))
                .build();
    }

    /**
     * Convierte RestockOrder a RestockOrderDTO
     */
    private InventoryDTOs.RestockOrderDTO convertToRestockDTO(RestockOrder order) {
        return InventoryDTOs.RestockOrderDTO.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .currentStock(order.getCurrentStock())
                .quantityToOrder(order.getQuantityToOrder())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .requestedBy(order.getRequestedBy())
                .notes(order.getNotes())
                .build();
    }

    /**
     * Notifica la creación de una nueva orden de reabastecimiento
     */
    private void notifyRestockOrderCreated(RestockOrder order) {
        try {
            // Obtener el producto
            InventoryItem item = firestore.collection("inventory")
                    .document(order.getProductId())
                    .get()
                    .get()
                    .toObject(InventoryItem.class);

            if (item == null) {
                log.error("Producto no encontrado para notificación de reorden: {}",
                        order.getProductId());
                return;
            }

            String subject = "Nueva Orden de Reabastecimiento Creada";
            String content = generateRestockOrderEmailContent(order, item);

            // Enviar notificación a veterinarios
            List<String> veterinarianEmails = getUserEmailsByRole(Role.VETERINARIO);
            for (String email : veterinarianEmails) {
                notificationService.sendEmail(email, subject, content);
            }

            log.info("Notificación de orden de reabastecimiento enviada para producto: {}",
                    item.getName());
        } catch (Exception e) {
            log.error("Error enviando notificación de reabastecimiento: {}", e.getMessage());
        }
    }

    /**
     * Calcula la cantidad recomendada de reorden
     */
    private int calculateRecommendedOrderQuantity(InventoryItem item) {
        // Fórmula básica: Doble del umbral mínimo menos el stock actual
        int recommended = (item.getMinThreshold() * 2) - item.getQuantity();
        return Math.max(recommended, 1); // Mínimo 1 unidad
    }

    /**
     * Genera el contenido del email para órdenes de reabastecimiento
     */
    private String generateRestockOrderEmailContent(RestockOrder order, InventoryItem item) {
        return String.format("""
            <html>
            <body>
                <h2>Nueva Orden de Reabastecimiento</h2>
                <div style="background-color: #e3f2fd; padding: 20px; border-radius: 5px;">
                    <h3>Producto: %s</h3>
                    <p><strong>Stock Actual:</strong> %d unidades</p>
                    <p><strong>Cantidad Solicitada:</strong> %d unidades</p>
                    <p><strong>Solicitado por:</strong> %s</p>
                    <p><strong>Notas:</strong> %s</p>
                </div>
                <p>Por favor, revisa y aprueba esta orden lo antes posible.</p>
                <a href="http://tu-aplicacion.com/inventory/restock-orders/%s" 
                   style="background-color: #4CAF50; color: white; padding: 10px 20px; 
                          text-decoration: none; border-radius: 5px;">
                    Ver Orden
                </a>
            </body>
            </html>
            """,
                item.getName(),
                order.getCurrentStock(),
                order.getQuantityToOrder(),
                order.getRequestedBy(),
                order.getNotes(),
                order.getId()
        );
    }

    private List<String> getUserEmailsByRole(Role role) {
        try {
            return firestore.collection("users")
                    .whereArrayContains("roles", role)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.getString("email"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error obteniendo emails por rol: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
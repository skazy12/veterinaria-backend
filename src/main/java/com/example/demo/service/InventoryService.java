package com.example.demo.service;

import com.example.demo.dto.InventoryDTOs;
import com.example.demo.dto.InventoryDTOs.*;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.AlertStatus;
import com.example.demo.model.InventoryItem;
import com.example.demo.model.RestockOrder;
import com.example.demo.util.FirestorePaginationUtils;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    private Firestore firestore;

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public PaginatedResponse<InventoryItemResponse> getAllItems(PaginationRequest request) {
        try {
            CollectionReference inventoryRef = firestore.collection("inventory");
            Query query = inventoryRef;

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

            List<InventoryItemResponse> items = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        InventoryItem item = doc.toObject(InventoryItem.class);
                        return convertToInventoryItemResponse(item);
                    })
                    .collect(Collectors.toList());

            long totalElements = FirestorePaginationUtils.getTotalElements(inventoryRef);

            return PaginatedResponse.of(items, request, totalElements);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching inventory items: " + e.getMessage());
        }
    }


    public InventoryItemResponse addItem(AddInventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setMinThreshold(request.getMinThreshold());
        item.setDateAdded(new Date());
        item.setPrice(request.getPrice());

        try {
            getFirestore().collection("inventory").document(item.getId()).set(item).get();
            return convertToInventoryItemResponse(item);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding inventory item: " + e.getMessage());
        }
    }

    public InventoryItemResponse updateItem(String id, UpdateInventoryItemRequest request) {
        try {
            InventoryItem item = getFirestore().collection("inventory").document(id).get().get().toObject(InventoryItem.class);
            if (item == null) {
                throw new CustomExceptions.NotFoundException("Inventory item not found with id: " + id);
            }
            item.setQuantity(request.getQuantity());
            item.setPrice(request.getPrice());
            item.setMinThreshold(request.getMinThreshold());

            getFirestore().collection("inventory").document(id).set(item).get();
            return convertToInventoryItemResponse(item);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating inventory item: " + e.getMessage());
        }
    }

    public PaginatedResponse<LowStockAlertDTO> getLowStockItems(PaginationRequest request) {
        try {
            CollectionReference inventoryRef = firestore.collection("inventory");

            // Construir query base
            Query query = inventoryRef;

            // Agregar condición de bajo stock
            query = query.whereGreaterThan("minThreshold", 0)  // Solo items con umbral definido
                    .whereLessThanOrEqualTo("quantity", "minThreshold");

            // Aplicar filtros adicionales
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
            }

            // Ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;

            // Asegurarse de que el campo de ordenamiento existe
            String sortBy = request.getSortBy().equals("id") ? "quantity" : request.getSortBy();
            query = query.orderBy(sortBy, direction);

            // Paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<LowStockAlertDTO> alerts = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        InventoryItem item = doc.toObject(InventoryItem.class);
                        // Asegurarse de establecer el ID del documento
                        if (item != null) {
                            item.setId(doc.getId());
                        }
                        return createAlertDTO(item);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Contar total de elementos (en una consulta separada)
            long totalElements = getTotalLowStockItems(inventoryRef);

            return PaginatedResponse.of(alerts, request, totalElements);

        } catch (Exception e) {
            log.error("Error fetching low stock items: ", e);
            throw new CustomExceptions.ProcessingException(
                    "Error fetching low stock items: " + e.getMessage());
        }
    }
    /**
     * Obtiene el total de items con bajo stock
     */
    private long getTotalLowStockItems(CollectionReference inventoryRef) throws ExecutionException, InterruptedException {
        return inventoryRef
                .whereGreaterThan("minThreshold", 0)
                .whereLessThanOrEqualTo("quantity", "minThreshold")
                .get()
                .get()
                .size();
    }



    private InventoryItemResponse convertToInventoryItemResponse(InventoryItem item) {
        InventoryItemResponse response = new InventoryItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setMinThreshold(item.getMinThreshold());
        response.setDateAdded(item.getDateAdded());
        response.setPrice(item.getPrice());
        return response;
    }
    public LowStockAlertDTO createAlertDTO(InventoryItem item) {
        if (item == null) {
            return null;
        }

        AlertStatus status = calculateAlertStatus(item);

        return LowStockAlertDTO.builder()
                .id(UUID.randomUUID().toString()) // Generar nuevo ID para la alerta
                .productId(item.getId())
                .productName(item.getName())
                .currentStock(item.getQuantity())
                .minThreshold(item.getMinThreshold())
                .status(status)
                .createdAt(new Date())
                .isAcknowledged(false)
                .build();
    }
    private AlertStatus calculateAlertStatus(InventoryItem item) {
        if (item.getQuantity() <= 0) {
            return AlertStatus.CRITICAL;
        } else if (item.getQuantity() <= item.getMinThreshold()) {
            double ratio = (double) item.getQuantity() / item.getMinThreshold();
            return ratio <= 0.5 ? AlertStatus.CRITICAL : AlertStatus.WARNING;
        }
        return AlertStatus.OK;
    }
    public InventoryItemResponse updateThreshold(String productId, int newThreshold) {
        try {
            DocumentReference docRef = firestore.collection("inventory").document(productId);
            DocumentSnapshot doc = docRef.get().get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Product not found");
            }

            InventoryItem item = doc.toObject(InventoryItem.class);
            if (item != null) {
                item.setMinThreshold(newThreshold);
                docRef.set(item).get();
                return convertToInventoryItemResponse(item);
            }

            throw new CustomExceptions.ProcessingException("Error updating threshold");
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error updating threshold: " + e.getMessage());
        }
    }
    public RestockOrderDTO createRestockOrder(CreateRestockOrderRequest request) {
        try {
            String currentUserId = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            // Verificar que el producto existe
            DocumentSnapshot productDoc = firestore.collection("inventory")
                    .document(request.getProductId())
                    .get().get();

            if (!productDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Product not found");
            }

            InventoryItem product = productDoc.toObject(InventoryItem.class);

            RestockOrder order = RestockOrder.builder()
                    .id(UUID.randomUUID().toString())
                    .productId(request.getProductId())
                    .productName(product.getName())
                    .currentStock(product.getQuantity())
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

            return convertToRestockDTO(order);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error creating restock order: " + e.getMessage());
        }
    }
    public InventoryItemResponse getItemById(String id) {
        try {
            // Obtener el documento del inventario
            DocumentSnapshot doc = firestore.collection("inventory")
                    .document(id)
                    .get()
                    .get();

            if (!doc.exists()) {
                throw new CustomExceptions.NotFoundException("Item not found with id: " + id);
            }

            InventoryItem item = doc.toObject(InventoryItem.class);

            // Verificar si el item es favorito para el usuario actual
            boolean isFavorite = checkIfFavorite(id);

            return convertToResponse(item, isFavorite);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching inventory item: " + e.getMessage());
        }
    }
    private InventoryItemResponse convertToResponse(InventoryItem item, boolean isFavorite) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .isFavorite(isFavorite)
                .build();
    }
    private RestockOrderDTO convertToRestockDTO(RestockOrder order) {
        if (order == null) return null;

        return RestockOrderDTO.builder()
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
    private boolean checkIfFavorite(String itemId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            // Buscar en la colección de favoritos
            QuerySnapshot snapshot = firestore.collection("favorites")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("itemId", itemId)
                    .whereEqualTo("itemType", "PRODUCT")
                    .get()
                    .get();

            return !snapshot.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }



}
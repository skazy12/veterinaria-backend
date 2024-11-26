package com.example.demo.service;

import com.example.demo.dto.FavoriteDTOs.*;
import com.example.demo.dto.ServiceVeterinaryDTOs;
import com.example.demo.dto.InventoryDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Favorite;
import com.example.demo.model.InventoryItem;
import com.example.demo.model.ItemType;
import com.example.demo.model.ServiceVeterinary;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class FavoriteService {
    @Autowired
    private Firestore firestore;

    @Autowired
    private ServiceVeterinaryService serviceService;

    @Autowired
    private InventoryService inventoryService;

    /**
     * Obtiene todos los favoritos del veterinario actual
     */
    public List<FavoriteResponse> getCurrentUserFavorites() {
        String userId = getCurrentUserUid();
        try {
            // Obtener todos los favoritos del usuario
            QuerySnapshot snapshot = firestore.collection("favorites")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .get();

            List<FavoriteResponse> favorites = new ArrayList<>();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Favorite favorite = doc.toObject(Favorite.class);
                if (favorite != null) {
                    // Obtener información adicional según el tipo de item
                    FavoriteResponse response = enrichFavoriteResponse(favorite);
                    if (response != null) {
                        favorites.add(response);
                    }
                }
            }

            return favorites;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error fetching favorites: " + e.getMessage());
        }
    }

    /**
     * Alterna el estado de favorito de un item
     */
    public FavoriteResponse toggleFavorite(String itemId, ItemType itemType) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Primero verificar que el item existe en la colección correspondiente
            boolean itemExists = checkItemExists(itemId, itemType);
            if (!itemExists) {
                throw new CustomExceptions.NotFoundException("Item not found");
            }

            // Buscar si ya existe el favorito
            QuerySnapshot existingFavorite = firestore.collection("favorites")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("itemId", itemId)
                    .whereEqualTo("itemType", itemType.name())
                    .get()
                    .get();

            // Si existe, eliminarlo
            if (!existingFavorite.isEmpty()) {
                existingFavorite.getDocuments().get(0).getReference().delete().get();
                return null; // Indicar que se eliminó el favorito
            }

            // Si no existe, crearlo
            Favorite favorite = Favorite.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .itemId(itemId)
                    .itemType(itemType.name())
                    .createdAt(new Date())
                    .build();

            // Guardar en Firestore
            firestore.collection("favorites")
                    .document(favorite.getId())
                    .set(favorite)
                    .get();

            // Retornar respuesta enriquecida
            return enrichFavoriteResponse(favorite);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error toggling favorite: " + e.getMessage());
        }
    }

    /**
     * Verifica si un item está marcado como favorito
     */
    public boolean isFavorite(String itemId, String itemType) {
        String userId = getCurrentUserUid();
        try {
            QuerySnapshot snapshot = firestore.collection("favorites")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("itemId", itemId)
                    .whereEqualTo("itemType", itemType)
                    .get()
                    .get();

            return !snapshot.isEmpty();
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error checking favorite status: " + e.getMessage());
        }
    }

    /**
     * Enriquece la respuesta del favorito con información del item
     */
    private FavoriteResponse enrichFavoriteResponse(Favorite favorite) {
        try {
            // Usamos el enum ItemType para mayor seguridad
            if (ItemType.VETERINARY_SERVICE.name().equals(favorite.getItemType())) {
                // Obtener de la colección veterinary_services
                DocumentSnapshot serviceDoc = firestore.collection("veterinary_services")
                        .document(favorite.getItemId())
                        .get()
                        .get();

                if (serviceDoc.exists()) {
                    ServiceVeterinary service = serviceDoc.toObject(ServiceVeterinary.class);
                    return FavoriteResponse.builder()
                            .id(favorite.getId())
                            .itemId(favorite.getItemId())
                            .itemType(favorite.getItemType())
                            .itemName(service.getName())
                            .price(service.getPrice())
                            .createdAt(favorite.getCreatedAt())
                            .build();
                }

            } else if (ItemType.INVENTORY.name().equals(favorite.getItemType())) {
                // Obtener de la colección inventory
                DocumentSnapshot inventoryDoc = firestore.collection("inventory")
                        .document(favorite.getItemId())
                        .get()
                        .get();

                if (inventoryDoc.exists()) {
                    InventoryItem item = inventoryDoc.toObject(InventoryItem.class);
                    return FavoriteResponse.builder()
                            .id(favorite.getId())
                            .itemId(favorite.getItemId())
                            .itemType(favorite.getItemType())
                            .itemName(item.getName())
                            .price(item.getPrice())
                            .createdAt(favorite.getCreatedAt())
                            // Campos adicionales específicos de inventario
                            .quantity(item.getQuantity())
                            .minThreshold(item.getMinThreshold())
                            .status(calculateStatus(item))
                            .build();
                }
            }

            return null;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException(
                    "Error enriching favorite response: " + e.getMessage());
        }
    }


    private String getCurrentUserUid() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    private boolean checkItemExists(String itemId, ItemType itemType) throws Exception {
        DocumentSnapshot doc;

        if (itemType == ItemType.VETERINARY_SERVICE) {
            doc = firestore.collection("veterinary_services")
                    .document(itemId)
                    .get()
                    .get();
        } else {
            doc = firestore.collection("inventory")
                    .document(itemId)
                    .get()
                    .get();
        }

        return doc.exists();
    }
    private String calculateStatus(InventoryItem item) {
        if (item.getQuantity() <= 0) {
            return "CRITICAL";
        } else if (item.getQuantity() <= item.getMinThreshold()) {
            return "WARNING";
        }
        return "OK";
    }
}
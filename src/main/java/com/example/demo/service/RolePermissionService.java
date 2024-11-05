package com.example.demo.service;

import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.security.CustomUserDetails;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {
    @Autowired
    private Firestore firestore;
    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    private final Map<Role, List<String>> rolePermissions;

    public RolePermissionService() {
        rolePermissions = new HashMap<>();
        rolePermissions.put(Role.VETERINARIO, List.of("VER_USUARIOS", "GESTIONAR_USUARIOS", "GESTIONAR_ROLES"));
        rolePermissions.put(Role.RECEPCIONISTA, List.of("VER_USUARIOS"));
        rolePermissions.put(Role.CLIENTE, List.of("VER_PERFIL_PROPIO", "EDITAR_PERFIL_PROPIO"));
    }


    public boolean hasPermission(String uid, String permission) {
        try {
            List<Role> userRoles = getUserRoles(uid);
            for (Role role : userRoles) {
                List<String> rolePermissions = getRolePermissions(role.name());
                if (rolePermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error checking user permission: " + e.getMessage());
        }
    }
    public UserDTOs.RolePermissionDTO updateRolePermissions(String roleName, List<String> newPermissions) {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection("roles")
                    .whereEqualTo("name", roleName)
                    .get().get().getDocuments();
            if (!documents.isEmpty()) {
                String docId = documents.get(0).getId();
                firestore.collection("roles").document(docId).update("permissions", newPermissions).get();
                return new UserDTOs.RolePermissionDTO(roleName, newPermissions);
            } else {
                throw new RuntimeException("Role not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating role permissions", e);
        }
    }
    public List<UserDTOs.RolePermissionDTO> getAllRolePermissions() {
        try {
            List<UserDTOs.RolePermissionDTO> rolePermissions = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = getFirestore().collection("roles").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                UserDTOs.RolePermissionDTO dto = new UserDTOs.RolePermissionDTO();

                // Usar el campo 'name' como el nombre del rol en lugar del ID del documento
                String roleName = document.getString("name");
                dto.setRole(roleName != null ? roleName : document.getId());

                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) document.get("permissions");
                dto.setPermissions(permissions);

                rolePermissions.add(dto);
            }

            return rolePermissions;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching role permissions: " + e.getMessage());
        }
    }
    private List<Role> getUserRoles(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = getFirestore().collection("users").document(uid).get().get();
        if (document.exists()) {
            @SuppressWarnings("unchecked")
            List<String> roleStrings = (List<String>) document.get("roles");
            return roleStrings.stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getRolePermissions(String roleName) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = getFirestore().collection("roles").whereEqualTo("name", roleName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (!documents.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) documents.get(0).get("permissions");
            return permissions != null ? permissions : new ArrayList<>();
        }
        return new ArrayList<>();
    }
    // Obtener permisos de un rol por nombre
    public List<String> getRolePermissionsByName(String roleName) {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection("roles")
                    .whereEqualTo("name", roleName)
                    .get().get().getDocuments();
            if (!documents.isEmpty()) {
                return (List<String>) documents.get(0).get("permissions");
            } else {
                throw new RuntimeException("Role not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching role permissions", e);
        }
    }
}

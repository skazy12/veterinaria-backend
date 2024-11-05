package com.example.demo.service;

import com.example.demo.dto.RoleDTOs;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleService {
    @Autowired
    private Firestore firestore;

    public List<RoleDTOs.RoleResponse> getAllRoles() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("roles").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.stream()
                    .map(this::convertToDTO)
                    .map(RoleDTOs.RoleResponse::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching roles", e);
        }
    }
/*
    public RoleDTOs.RoleDTO createRole(RoleDTOs.CreateRoleRequest request) {
        DocumentReference docRef = firestore.collection("roles").document();
        Map<String, Object> data = new HashMap<>();
        data.put("name", request.getName());
        data.put("permissions", request.getPermissions());

        try {
            docRef.set(data).get();
            return new RoleDTOs.RoleDTO(docRef.getId(), request.getName(), request.getPermissions());
        } catch (Exception e) {
            throw new RuntimeException("Error creating role", e);
        }
    }
*/
    public RoleDTOs.RoleResponse createRole(RoleDTOs.CreateRoleRequest request) {
        DocumentReference docRef = firestore.collection("roles").document();
        Map<String, Object> data = new HashMap<>();
        data.put("name", request.getName());
        Map<String, String> permissions = new HashMap<>();
        for (int i = 0; i < request.getPermissions().size(); i++) {
            permissions.put(String.valueOf(i), request.getPermissions().get(i));
        }
        data.put("permissions", permissions);

        try {
            docRef.set(data).get();
            RoleDTOs.RoleDTO roleDTO = new RoleDTOs.RoleDTO(docRef.getId(), request.getName(), request.getPermissions());
            return new RoleDTOs.RoleResponse(roleDTO);
        } catch (Exception e) {
            throw new RuntimeException("Error creating role", e);
        }
    }
//    public RoleDTOs.RoleDTO updateRole(String roleId, RoleDTOs.UpdateRoleRequest request) {
//        DocumentReference docRef = firestore.collection("roles").document(roleId);
//        Map<String, Object> updates = new HashMap<>();
//        updates.put("name", request.getName());
//
//        try {
//            docRef.update(updates).get();
//            DocumentSnapshot updatedDoc = docRef.get().get();
//            return convertToDTO(updatedDoc);
//        } catch (Exception e) {
//            throw new RuntimeException("Error updating role", e);
//        }
//    }
    public RoleDTOs.RoleResponse updateRole(String roleId, RoleDTOs.UpdateRoleRequest request) {
        DocumentReference docRef = firestore.collection("roles").document(roleId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", request.getName());
        Map<String, String> permissions = new HashMap<>();
        for (int i = 0; i < request.getPermissions().size(); i++) {
            permissions.put(String.valueOf(i), request.getPermissions().get(i));
        }
        updates.put("permissions", permissions);

        try {
            docRef.update(updates).get();
            DocumentSnapshot updatedDoc = docRef.get().get();
            return new RoleDTOs.RoleResponse(convertToDTO(updatedDoc));
        } catch (Exception e) {
            throw new RuntimeException("Error updating role", e);
        }
    }

    public void deleteRole(String roleId) {
        try {
            firestore.collection("roles").document(roleId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting role", e);
        }
    }

    public RoleDTOs.RoleDTO updateRolePermissions(String roleId, List<String> permissions) {
        DocumentReference docRef = firestore.collection("roles").document(roleId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("permissions", permissions);

        try {
            docRef.update(updates).get();
            DocumentSnapshot updatedDoc = docRef.get().get();
            return convertToDTO(updatedDoc);
        } catch (Exception e) {
            throw new RuntimeException("Error updating role permissions", e);
        }
    }

    private RoleDTOs.RoleDTO convertToDTO(DocumentSnapshot document) {
        String id = document.getId();
        String name = document.getString("name");
        Map<String, String> permissionsMap = (Map<String, String>) document.get("permissions");
        List<String> permissions = new ArrayList<>(permissionsMap.values());
        return new RoleDTOs.RoleDTO(id, name, permissions);
    }
}

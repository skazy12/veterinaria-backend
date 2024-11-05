package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserDTOs;
import com.example.demo.model.Role;
import com.example.demo.service.RolePermissionService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTOs.RolePermissionDTO>>> getAllRolePermissions() {
        return ResponseEntity.ok(ApiResponse.success(rolePermissionService.getAllRolePermissions()));
    }
    @PutMapping("/{roleName}/permissions")
    @PreAuthorize("hasPermission('', 'GESTIONAR_ROLES')")
    public ResponseEntity<ApiResponse<UserDTOs.RolePermissionDTO>> updateRolePermissions(
            @PathVariable String roleName,
            @RequestBody List<String> permissions
    ) {
        UserDTOs.RolePermissionDTO updatedRole = rolePermissionService.updateRolePermissions(roleName, permissions);
        return ResponseEntity.ok(ApiResponse.success(updatedRole));
    }

    // Obtener los permisos de un rol específico
    @GetMapping("/{roleName}/permissions")
    @PreAuthorize("hasPermission('', 'VER_ROLES')")
    public ResponseEntity<ApiResponse<List<String>>> getRolePermissions(@PathVariable String roleName) {
        List<String> permissions = rolePermissionService.getRolePermissionsByName(roleName);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasPermission('', 'GESTIONAR_USUARIOS')")
    public ResponseEntity<ApiResponse<UserDTOs.UserResponse>> updateUserRoles(@PathVariable String userId, @RequestBody List<Role> roles) {
        UserDTOs.UserResponse updatedUser = userService.updateUserRoles(userId, roles);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }
}
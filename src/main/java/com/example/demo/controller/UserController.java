package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // En UserController.java
    // En UserController.java
    @PreAuthorize("hasPermission(null, 'VER_USUARIOS')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponse>>> getAllUsers(
            @ModelAttribute PaginationRequest paginationRequest,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String role) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    userService.getAllUsers(paginationRequest, isActive, role)));
        } catch (CustomExceptions.ProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }
    @GetMapping("/veterinarians")
    @PreAuthorize("hasPermission('', 'VER_VETERINARIOS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponse>>> getVeterinarians(
            @ModelAttribute PaginationRequest paginationRequest) {
        try {
            // Configurar valores por defecto si no se proporcionan
            if (paginationRequest.getSortBy() == null) {
                paginationRequest.setSortBy("nombre");
            }
            if (paginationRequest.getSortDirection() == null) {
                paginationRequest.setSortDirection("ASC");
            }
            if (paginationRequest.getSize() == 0) {
                paginationRequest.setSize(10);
            }

            // Obtener veterinarios paginados
            PaginatedResponse<UserResponse> veterinarians = userService.getVeterinarians(paginationRequest);
            return ResponseEntity.ok(ApiResponse.success(veterinarians));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'VER_USUARIOS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponse>>> searchUsers(
            @RequestParam String searchTerm,
            @ModelAttribute PaginationRequest paginationRequest) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    userService.searchUsers(searchTerm, paginationRequest)));
        } catch (Exception e) {
            log.error("Error searching users:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }



 /*
    @PreAuthorize("hasPermission(null, 'VER_USUARIOS')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String role) {
        List<UserResponse> users = userService.getAllUsers(isActive, role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }


  */

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('', 'VER_USUARIOS')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping
    //@PreAuthorize("hasPermission('', 'CREAR_USUARIO')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody AuthDTOs.RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
    }

    @PostMapping("/create-with-roles")
    @PreAuthorize("hasPermission('', 'CREAR_USUARIO')")
    public ResponseEntity<ApiResponse<UserResponse>> createUserWithRoles(@RequestBody AuthDTOs.RegisterRequest request) {
        try {
            // Delegate user creation to UserService
            UserResponse newUser = userService.createUserWithRoles(request);
            return ResponseEntity.ok(ApiResponse.success(newUser));
        } catch (CustomExceptions.ProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('', 'EDITAR_USUARIO')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('', 'ELIMINAR_USUARIO')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("hasPermission('', 'VER_PERFIL_PROPIO')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (CustomExceptions.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
        } catch (CustomExceptions.InvalidPasswordException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PASSWORD", e.getMessage()));
        } catch (CustomExceptions.ProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasPermission('', 'EDITAR_USUARIO')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable String id, @RequestBody ToggleUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserStatus(id, request)));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasPermission('', 'EDITAR_USUARIO')")
    public ResponseEntity<ApiResponse<Void>> addNote(@PathVariable String id, @RequestBody AddNoteRequest request) {
        userService.addNote(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasPermission('', 'VER_USUARIOS')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserNotes(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserNotes(id)));
    }
    @GetMapping("/me")
    @PreAuthorize("hasPermission('', 'VER_PERFIL_PROPIO')")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasPermission('', 'EDITAR_PERFIL_PROPIO')")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateCurrentUserProfile(request)));
    }
    @GetMapping("/me/pets")
    @PreAuthorize("hasPermission('', 'VER_PERFIL_PROPIO')")
    public ResponseEntity<ApiResponse<List<PetDTOs.PetResponse>>> getCurrentUserPets() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserPets()));
    }
    @GetMapping("/{id}/pets")
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA') or @userService.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<List<PetDTOs.PetResponse>>> getUserPets(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserPets(id)));
    }
}
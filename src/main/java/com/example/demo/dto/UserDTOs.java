package com.example.demo.dto;

import com.example.demo.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class UserDTOs {

    @Data
    public static class UserResponse {
        private String uid;
        private String email;
        private String nombre;
        private String apellido;
        private String telefono;
        private String direccion;
        private List<Role> roles;
        private boolean isEnabled;
        private boolean active;
    }

    @Data
    public static class UpdateUserRequest {

        private String email;
        private String nombre;
        private String apellido;
        private String telefono;
        private String direccion;
        private List<Role> roles;
        private boolean isEnabled;
        private boolean active;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "La contraseña actual es obligatoria")
        private String currentPassword;

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
        private String newPassword;
    }

    @Data
    public static class ToggleUserStatusRequest {
        private boolean isActive;
    }

    @Data
    public static class AddNoteRequest {
        @NotBlank(message = "El contenido de la nota es obligatorio")
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionDTO {
        private String role;
        private List<String> permissions;
    }

    @Data
    public static class UpdateProfileRequest {
        private String nombre;
        private String apellido;
        private String telefono;
        private String direccion;
    }
    @Data
    @AllArgsConstructor
    public static class ClientSearchCriteria {
        private String clientName;
        private String petName;
        private LocalDate consultationDate;
    }
    @Data
    public static class ClientWithPetsDTO {
        private String uid;
        private String nombre;
        private String apellido;
        private String email;
        private String telefono;
        private String direccion;
        private List<PetDTOs.PetWithHistoryDTO> mascotas;
    }



}
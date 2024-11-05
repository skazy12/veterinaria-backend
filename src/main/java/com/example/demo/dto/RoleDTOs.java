package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


public class RoleDTOs {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoleDTO {
        private String id;
        private String name;
        private List<String> permissions;

        // Constructor, getters, and setters
    }

    @Data
    public static class CreateRoleRequest {
        private String name;
        private List<String> permissions;
    }

    @Data
    public static class UpdateRoleRequest {
        private String name;
        private List<String> permissions;
    }

    @Data
    public static class RoleResponse {
        private String id;
        private String name;
        private List<String> permissions;

        public RoleResponse(RoleDTO roleDTO) {
            this.id = roleDTO.getId();
            this.name = roleDTO.getName();
            this.permissions = roleDTO.getPermissions();
        }
    }

}

package com.example.demo.model;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class User {
    private String uid; // Firebase UID
    private String email;
    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private List<Role> roles;
    private boolean isEnabled;
    private boolean active;

    // Constructor para crear un User a partir de un Map (útil cuando se recupera datos de Firebase)
    public User(Map<String, Object> data) {
        this.uid = (String) data.get("uid");
        this.email = (String) data.get("email");
        this.nombre = (String) data.get("nombre");
        this.apellido = (String) data.get("apellido");
        this.telefono = (String) data.get("telefono");
        this.direccion = (String) data.get("direccion");
        this.roles = (List<Role>) data.get("roles");
        this.isEnabled = (Boolean) data.get("isEnabled");
    }

    // Método para convertir el User a un Map (útil cuando se guarda en Firebase)
    public Map<String, Object> toMap() {
        return Map.of(
                "uid", uid,
                "email", email,
                "nombre", nombre,
                "apellido", apellido,
                "telefono", telefono,
                "direccion", direccion,
                "roles", roles,
                "isEnabled", isEnabled
        );
    }
}
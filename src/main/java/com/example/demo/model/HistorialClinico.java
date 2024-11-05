package com.example.demo.model;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialClinico {
    private String id;
    private String petId;         // ID de la mascota
    private String veterinarianId;// ID del veterinario
    private Date fechaVisita;
    private String motivoConsulta;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private Date fechaCreacion;
    private Date fechaActualizacion;
    private String estado;        // ACTIVO, ARCHIVADO
}

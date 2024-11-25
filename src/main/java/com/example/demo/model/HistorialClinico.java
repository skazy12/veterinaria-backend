package com.example.demo.model;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialClinico {
    private String id;
    private String petId;
    private String veterinarianId;
    private Date fechaVisita;
    private String motivoConsulta;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private Date fechaCreacion;
    private Date fechaActualizacion;
    private String estado;

    // Nuevos campos
    @Builder.Default
    private List<ServicioRealizado> serviciosRealizados = new ArrayList<>();
    @Builder.Default
    private List<ServicioAdicional> serviciosAdicionales = new ArrayList<>();
    private double precioTotal;
}


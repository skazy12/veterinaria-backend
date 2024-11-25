package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioRealizado {
    private String serviceId;
    private String serviceName;
    private double precioBase;
    private Double precioPersonalizado; // null si se usa el precio base
    private String notas;
}

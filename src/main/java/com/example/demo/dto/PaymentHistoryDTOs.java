package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

public class PaymentHistoryDTOs {

    /**
     * DTO para la respuesta del historial de pagos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentHistoryResponse {
        private String id;
        private Date fecha;
        private String petId;
        private String petName;
        private double montoTotal;
        private List<ServicioDetalleDTO> serviciosRealizados;
        private List<ServicioAdicionalDTO> serviciosAdicionales;
        private String veterinarioNombre;
        private String razon;  // motivo de consulta
    }

    /**
     * DTO para mostrar detalles de servicios regulares
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioDetalleDTO {
        private String servicioId;
        private String nombre;
        private double precioBase;
        private Double precioPersonalizado;  // Puede ser null si se usa el precio base
        private String notas;
    }

    /**
     * DTO para mostrar detalles de servicios adicionales
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioAdicionalDTO {
        private String descripcion;
        private double precio;
        private String notas;
    }

    /**
     * DTO para filtrar el historial de pagos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentHistoryFilterRequest {
        private Date fechaInicio;
        private Date fechaFin;
        private String petId;  // opcional, para filtrar por mascota
        private Double montoMinimo;
        private Double montoMaximo;
    }
}
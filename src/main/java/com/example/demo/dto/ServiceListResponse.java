package com.example.demo.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceListResponse {
    private Map<String, List<ServiceDTO>> servicesByCategory;
    private long totalServices;
}

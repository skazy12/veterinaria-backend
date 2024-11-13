package com.example.demo.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSearchRequest {
    private String searchTerm;
    private String category;
    private Boolean onlyActive;
}
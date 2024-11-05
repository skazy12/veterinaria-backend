package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {
    private String id;
    private Date date;
    private String diagnosis;
    private String treatment;
    private String notes;
    private String veterinarianId;

    @Builder.Default
    private List<String> attachments = new ArrayList<>();
}
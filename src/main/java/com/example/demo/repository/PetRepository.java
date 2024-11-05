package com.example.demo.repository;

import com.example.demo.model.Pet;
import com.example.demo.exception.CustomExceptions;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class PetRepository {

    private final Firestore firestore;

    @Autowired
    public PetRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    // Método para obtener todas las mascotas
    public List<Pet> findAll() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("pets").get(); // Obtener todas las mascotas
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Pet.class)) // Convertir documentos Firestore a objetos Pet
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error al obtener las mascotas desde Firebase: " + e.getMessage());
        }
    }
}

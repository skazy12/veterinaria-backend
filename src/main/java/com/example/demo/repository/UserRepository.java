package com.example.demo.repository;

import com.example.demo.exception.CustomExceptions;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private final Firestore firestore;

    @Autowired
    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<User> findByEmail(String email) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users").whereEqualTo("email", email).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (!documents.isEmpty()) {
                return Optional.of(new User(documents.get(0).getData()));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user by email from Firebase: " + e.getMessage());
        }
        return Optional.empty();
    }


    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public User save(User user) {
        firestore.collection("users").document(user.getUid()).set(user.toMap());
        return user;
    }

    public List<User> findAll() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.stream()
                    .map(doc -> new User(doc.getData()))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching all users from Firebase: " + e.getMessage());
        }
    }


    public Optional<User> findById(String id) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users").whereEqualTo("uid", id).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (!documents.isEmpty()) {
                return Optional.of(new User(documents.get(0).getData()));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user by id from Firebase: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void deleteById(String id) {
        firestore.collection("users").document(id).delete();
    }
}
package com.example.demo.util;

import com.google.cloud.firestore.*;
import com.example.demo.dto.PaginationRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FirestorePaginationUtils {

    public static <T> QuerySnapshot getPaginatedData(
            CollectionReference collection,
            PaginationRequest request,
            Query.Direction sortDirection) throws ExecutionException, InterruptedException {

        // Construir query base
        Query query = collection;

        // Aplicar filtros si existen
        if (request.getFilterBy() != null && request.getFilterValue() != null) {
            query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
        }

        // Aplicar ordenamiento
        query = query.orderBy(request.getSortBy(), sortDirection);

        // Aplicar paginación
        query = query.offset(request.getPage() * request.getSize())
                .limit(request.getSize());

        // Ejecutar query
        return query.get().get();
    }

    public static <T> List<T> convertToList(QuerySnapshot snapshot, Class<T> clazz) {
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(clazz))
                .collect(Collectors.toList());
    }

    public static long getTotalElements(CollectionReference collection)
            throws ExecutionException, InterruptedException {
        return collection.get().get().size();
    }
}
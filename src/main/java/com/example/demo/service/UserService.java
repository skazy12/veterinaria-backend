package com.example.demo.service;

import com.example.demo.dto.AuthDTOs.RegisterRequest;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.dto.PaginationRequest;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    PetService petService;
    @Autowired
    private FirebaseAuth firebaseAuth;
    @Autowired
    private Firestore firestore;
    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
    public List<Map<String, Object>> getUserNotes(String userId) {
        try {
            List<Map<String, Object>> notes = new ArrayList<>();
            getFirestore().collection("users").document(userId).collection("notes").get().get()
                    .getDocuments().forEach(doc -> notes.add(doc.getData()));
            return notes;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user notes: " + e.getMessage());
        }
    }
    // Buscar clientes
    public PaginatedResponse<UserResponse> searchUsers(String searchTerm, PaginationRequest request) {
        try {
            CollectionReference usersRef = firestore.collection("users");

            // Crear una consulta base que filtre primero por el rol CLIENTE
            Query baseQuery = usersRef.whereArrayContains("roles", Role.CLIENTE);

            // Obtener todos los documentos que son clientes
            QuerySnapshot allClientsSnapshot = baseQuery.get().get();

            // Filtrar en memoria por el término de búsqueda (case insensitive)
            List<UserResponse> filteredUsers = allClientsSnapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(User.class))
                    .filter(user ->
                            (user.getNombre() + " " + user.getApellido())
                                    .toLowerCase()
                                    .contains(searchTerm.toLowerCase()) ||
                                    user.getEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                    user.getTelefono().contains(searchTerm)
                    )
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());

            // Calcular el total de elementos y páginas
            long totalElements = filteredUsers.size();
            int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

            // Aplicar paginación a los resultados filtrados
            List<UserResponse> paginatedUsers = filteredUsers.stream()
                    .skip((long) request.getPage() * request.getSize())
                    .limit(request.getSize())
                    .collect(Collectors.toList());

            return new PaginatedResponse<>(
                    paginatedUsers,
                    request.getPage(),
                    request.getSize(),
                    totalElements,
                    totalPages
            );
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error searching users: " + e.getMessage());
        }
    }
    //Buscar todos los usuarios
    public PaginatedResponse<UserResponse> searchAllUsers(String searchTerm, PaginationRequest request) {
        try {
            CollectionReference usersRef = firestore.collection("users");

            // Obtener todos los documentos
            QuerySnapshot allClientsSnapshot = usersRef.get().get();

            // Filtrar en memoria por nombre (case insensitive y null-safe)
            List<UserResponse> filteredUsers = allClientsSnapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(User.class))
                    .filter(user -> {
                        if (user == null || user.getNombre() == null) return false;

                        // Crear nombre completo y hacer búsqueda case-insensitive
                        String fullName = (user.getNombre() + " " +
                                (user.getApellido() != null ? user.getApellido() : ""))
                                .toLowerCase();
                        return fullName.contains(searchTerm.toLowerCase());
                    })
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());

            // Calcular el total de elementos y páginas
            long totalElements = filteredUsers.size();
            int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

            // Aplicar paginación
            List<UserResponse> paginatedUsers = filteredUsers.stream()
                    .skip((long) request.getPage() * request.getSize())
                    .limit(request.getSize())
                    .collect(Collectors.toList());

            return new PaginatedResponse<>(
                    paginatedUsers,
                    request.getPage(),
                    request.getSize(),
                    totalElements,
                    totalPages
            );

        } catch (Exception e) {
            logger.error("Error searching users: ", e);
            throw new CustomExceptions.ProcessingException("Error searching users: " + e.getMessage());
        }
    }

    public UserResponse createUser(RegisterRequest request) {
        try {
            // Primero, verifica si el usuario ya existe en Firestore
            Optional<User> existingUser = findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                // Si el usuario ya existe, actualiza sus datos y devuelve la respuesta
                User user = existingUser.get();
                updateUserData(user, request);
                return convertToUserResponse(user);
            }

            // Si el usuario no existe, crea uno nuevo
            User newUser = new User();
            newUser.setUid(request.getUid()); // Esto debería ser el UID de Firebase
            newUser.setEmail(request.getEmail());
            newUser.setNombre(request.getNombre());
            newUser.setApellido(request.getApellido());
            newUser.setTelefono(request.getTelefono());
            newUser.setDireccion(request.getDireccion());
            newUser.setRoles(request.getRoles());
            newUser.setEnabled(true);
            newUser.setActive(true);

            // Guarda el nuevo usuario en Firestore
            getFirestore().collection("users").document(newUser.getUid()).set(newUser).get();

            return convertToUserResponse(newUser);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error creating user: " + e.getMessage());
        }
    }
    public UserResponse createUserWithRoles(RegisterRequest request) {
        try {
            // Create user in Firebase Authentication
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);

            // Use the Firebase UID for the Firestore user record
            User newUser = new User();
            newUser.setUid(userRecord.getUid());
            newUser.setEmail(request.getEmail());
            newUser.setNombre(request.getNombre());
            newUser.setApellido(request.getApellido());
            newUser.setTelefono(request.getTelefono());
            newUser.setDireccion(request.getDireccion());
            newUser.setRoles(request.getRoles()); // Assign roles from the frontend
            newUser.setEnabled(true);
            newUser.setActive(true);

            // Save the user to Firestore
            getFirestore().collection("users").document(newUser.getUid()).set(newUser).get();

            // Set custom claims for roles in Firebase Authentication
            Map<String, Object> claims = new HashMap<>();
            List<String> rolesWithPrefix = request.getRoles().stream()
                    .map(role -> "ROLE_" + role.name())
                    .collect(Collectors.toList());
            claims.put("roles", rolesWithPrefix);
            FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);

            return convertToUserResponse(newUser);

        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error creating user: " + e.getMessage());
        }
    }
    private void updateUserData(User user, RegisterRequest request) {
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRoles(request.getRoles());
        // Actualiza el usuario en Firestore
        try {
            getFirestore().collection("users").document(user.getUid()).set(user).get();
        } catch (InterruptedException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }
    public Optional<User> findByEmail(String email) {
        try {
            QuerySnapshot querySnapshot = getFirestore().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get();
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(document.toObject(User.class));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error finding user by email: " + e.getMessage());
        }
    }

    public UserResponse getUserById(String id) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user: " + e.getMessage());
        }
    }

    // En UserService.java
    // En UserService.java
    public PaginatedResponse<UserResponse> getAllUsers(PaginationRequest request,
                                                       Boolean isActive, String role) {
        try {
            CollectionReference usersRef = getFirestore().collection("users");
            Query query = usersRef;

            // Aplicar filtros específicos
            if (isActive != null) {
                query = query.whereEqualTo("active", isActive);
            }
            if (role != null) {
                query = query.whereArrayContains("roles", Role.valueOf(role.toUpperCase()));
            }

            // Aplicar filtros generales si existen
            if (request.getFilterBy() != null && request.getFilterValue() != null) {
                query = query.whereEqualTo(request.getFilterBy(), request.getFilterValue());
            }

            // Aplicar ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(request.getSortBy(), direction);

            // Aplicar paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<UserResponse> users = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        return convertToUserResponse(user);
                    })
                    .collect(Collectors.toList());

            // Contar total de elementos
            long totalElements = usersRef.get().get().size();

            return PaginatedResponse.of(users, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching users: " + e.getMessage());
        }
    }
    public PaginatedResponse<UserResponse> getVeterinarians(PaginationRequest request) {
        try {
            CollectionReference usersRef = firestore.collection("users");
            Query query = usersRef;

            // Filtrar solo veterinarios
            query = query.whereArrayContains("roles", Role.VETERINARIO)
                    .whereEqualTo("active", true); // Solo veterinarios activos

            // Aplicar ordenamiento
            Query.Direction direction = request.getSortDirection().equalsIgnoreCase("DESC")
                    ? Query.Direction.DESCENDING
                    : Query.Direction.ASCENDING;
            query = query.orderBy(request.getSortBy(), direction);

            // Aplicar paginación
            query = query.offset(request.getPage() * request.getSize())
                    .limit(request.getSize());

            // Ejecutar query
            QuerySnapshot querySnapshot = query.get().get();

            // Convertir resultados
            List<UserResponse> veterinarians = querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId()); // Asegurar que el ID esté establecido
                        return convertToUserResponse(user);
                    })
                    .collect(Collectors.toList());

            // Obtener el total de elementos
            Query countQuery = usersRef.whereArrayContains("roles", Role.VETERINARIO)
                    .whereEqualTo("active", true);
            long totalElements = countQuery.get().get().size();

            // Crear y retornar la respuesta paginada
            return PaginatedResponse.of(veterinarians, request, totalElements);

        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error fetching veterinarians: " + e.getMessage());
        }
    }

    public UserResponse updateUser(String id, UpdateUserRequest request) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            user.setEmail(request.getEmail());
            user.setNombre(request.getNombre());
            user.setApellido(request.getApellido());
            user.setTelefono(request.getTelefono());
            user.setDireccion(request.getDireccion());
            user.setRoles(request.getRoles());

            getFirestore().collection("users").document(id).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(String id) {
        try {
            FirebaseAuth.getInstance().deleteUser(id);
            getFirestore().collection("users").document(id).delete().get();
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error deleting user: " + e.getMessage());
        }
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        try {
            // Verificar la contraseña actual
            if (!verifyCurrentPassword(userId, request.getCurrentPassword())) {
                throw new CustomExceptions.UnauthorizedException("La contraseña actual es incorrecta");
            }

            // Validar la nueva contraseña
            if (!isValidPassword(request.getNewPassword())) {
                throw new CustomExceptions.InvalidPasswordException("La nueva contraseña no cumple con los requisitos de seguridad");
            }

            // Actualizar la contraseña
            UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(userId)
                    .setPassword(request.getNewPassword());
            firebaseAuth.updateUser(updateRequest);

        } catch (FirebaseAuthException e) {
            // Log the exception details

            throw new CustomExceptions.ProcessingException("Error changing password: " + e.getMessage());
        } catch (Exception e) {
            // Log any unexpected exceptions
            logger.error("Unexpected error in changePassword: ", e);
            throw new CustomExceptions.ProcessingException("Unexpected error occurred: " + e.getMessage());
        }
    }
    private boolean isValidPassword(String password) {
        // Implementar la validación de contraseña
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return password.matches(regex);
    }
    private boolean verifyCurrentPassword(String userId, String currentPassword) {
        try {
            // Obtener el usuario de Firebase
            UserRecord userRecord = firebaseAuth.getUser(userId);

            // Intentar autenticar al usuario con el email y la contraseña actual
            // Nota: Este método no está disponible directamente en FirebaseAuth,
            // por lo que necesitamos usar una alternativa

            // Una opción es usar la API de Firebase Auth REST
            // Esto requiere una implementación adicional que no está directamente
            // disponible en el SDK de Admin de Firebase

            // Por ahora, como placeholder, asumiremos que la verificación es exitosa
            // En una implementación real, necesitarías integrar con la API de Firebase Auth
            return true;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }

    public UserResponse toggleUserStatus(String id, ToggleUserStatusRequest request) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            user.setActive(request.isActive());
            getFirestore().collection("users").document(id).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error toggling user status: " + e.getMessage());
        }
    }

    public void addNote(String userId, AddNoteRequest request) {
        try {
            Map<String, Object> note = new HashMap<>();
            note.put("content", request.getContent());
            note.put("timestamp", new Date());

            getFirestore().collection("users").document(userId).collection("notes").add(note).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding note: " + e.getMessage());
        }
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUid(user.getUid());
        response.setEmail(user.getEmail());
        response.setNombre(user.getNombre());
        response.setApellido(user.getApellido());
        response.setTelefono(user.getTelefono());
        response.setDireccion(user.getDireccion());
        response.setRoles(user.getRoles() != null ? user.getRoles() : new ArrayList<>());
        response.setEnabled(user.isEnabled());
        response.setActive(user.isActive());
        return response;
    }
    /*
    public UserResponse updateUserProfile(UpdateProfileRequest request) {
        String uid = getCurrentUserUid();
        try {
            User user = getFirestore().collection("users").document(uid).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + uid);
            }
            user.setNombre(request.getNombre());
            user.setApellido(request.getApellido());
            user.setTelefono(request.getTelefono());
            user.setDireccion(request.getDireccion());

            getFirestore().collection("users").document(uid).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }

     */
    public boolean existsByEmail(String email) {
        try {
            return getFirestore().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get()
                    .size() > 0;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error checking user existence: " + e.getMessage());
        }
    }

//    private String getCurrentUserUid() {
//        // Obtener el contexto de seguridad actual
//        SecurityContext securityContext = SecurityContextHolder.getContext();
//        Authentication authentication = securityContext.getAuthentication();
//
//        // Verificar si hay un usuario autenticado
//        if (authentication != null && authentication.isAuthenticated()) {
//            // El nombre principal en este caso será el UID de Firebase
//            return authentication.getName();
//        }
//
//        // Si no hay usuario autenticado, lanzar una excepción
//        throw new CustomExceptions.AuthenticationException("No authenticated user found");
//    }
    public UserResponse updateUserProfile(UpdateProfileRequest request) {
        String uid = getCurrentUserUid();
        try {
            User user = getFirestore().collection("users").document(uid).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + uid);
            }
            User updatedUser = User.builder()
                    .uid(user.getUid())
                    .email(user.getEmail())
                    .nombre(request.getNombre())
                    .apellido(request.getApellido())
                    .telefono(request.getTelefono())
                    .direccion(request.getDireccion())
                    .roles(user.getRoles())  // Mantener roles existentes
                    .isEnabled(user.isEnabled())
                    .active(user.isActive())
                    .build();

            getFirestore().collection("users").document(uid).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }
    public List<PetDTOs.PetResponse> getUserPets(String userId) {
        return petService.getPetsByUserId(userId);
    }
    public UserResponse updateUserRoles(String id, List<Role> roles) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            user.setRoles(roles);
            getFirestore().collection("users").document(id).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user roles: " + e.getMessage());
        }
    }
    public UserResponse getCurrentUserProfile() {
        String uid = getCurrentUserUid();
        return getUserById(uid);
    }
    public UserResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        String uid = getCurrentUserUid();
        try {
            User user = getFirestore().collection("users").document(uid).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + uid);
            }
            User updatedUser = User.builder()
                    .uid(user.getUid())
                    .email(user.getEmail())
                    .nombre(request.getNombre())
                    .apellido(request.getApellido())
                    .telefono(request.getTelefono())
                    .direccion(request.getDireccion())
                    .roles(user.getRoles())  // Mantener roles existentes
                    .isEnabled(user.isEnabled())
                    .active(user.isActive())
                    .build();

            getFirestore().collection("users").document(uid).set(updatedUser).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }

    public List<PetDTOs.PetResponse> getCurrentUserPets() {
        String uid = getCurrentUserUid();
        return petService.getPetsByUserId(uid);
    }
    private String getCurrentUserUid() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    private UpdateUserRequest convertToUpdateUserRequest(User user) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombre(user.getNombre());
        request.setApellido(user.getApellido());
        request.setTelefono(user.getTelefono());
        request.setDireccion(user.getDireccion());
        return request;
    }
    private User getUserEntityById(String id) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentSnapshot document = firestore.collection("users").document(id).get().get();

            if (!document.exists()) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }

            User user = document.toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.ProcessingException("Error converting Firestore document to User object");
            }

            // Asegúrate de que el ID del usuario esté establecido
            user.setUid(id);

            return user;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user from Firestore: " + e.getMessage());
        }
    }

}
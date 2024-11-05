package com.example.demo.service;

import com.example.demo.dto.AuthDTOs.*;
import com.example.demo.dto.UserDTOs.UserResponse;
import com.example.demo.exception.CustomExceptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserService userService;
    @Autowired
    private FirebaseAuth firebaseAuth;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;

    private final WebClient webClient;

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://identitytoolkit.googleapis.com/v1").build();
    }

//    public AuthResponse registerUser(RegisterRequest request) {
//        try {
//            UserRecord userRecord;
//            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
//                    .setEmail(request.getEmail())
//                    .setPassword(request.getPassword())
//                    .setDisplayName(request.getNombre() + " " + request.getApellido());
//
//            try {
//                // Intentar crear el usuario
//                userRecord = FirebaseAuth.getInstance().createUser(createRequest);
//            } catch (FirebaseAuthException e) {
//                if ("auth/email-already-exists".equals(e.getErrorCode())) {
//                    // Si el email existe, intentamos obtener el usuario
//                    userRecord = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
//                    // Si el usuario existe en Auth pero no en Firestore, lo eliminamos y creamos uno nuevo
//                    if (!userService.existsByEmail(request.getEmail())) {
//                        FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
//                        userRecord = FirebaseAuth.getInstance().createUser(createRequest);
//                    } else {
//                        throw new CustomExceptions.EmailAlreadyExistsException("Email already in use");
//                    }
//                } else {
//                    throw e;
//                }
//            }
//
//            UserResponse user = userService.createUser(request);
//
//            List<String> rolesWithPrefix = user.getRoles().stream()
//                    .map(role -> "ROLE_" + role.name())
//                    .collect(Collectors.toList());
//
//            Map<String, Object> claims = new HashMap<>();
//            claims.put("roles", rolesWithPrefix);
//            FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);
//
//            String customToken = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
//            String idToken = exchangeCustomTokenForIdToken(customToken);
//
//            return new AuthResponse(idToken, user);
//        } catch (FirebaseAuthException e) {
//            throw new CustomExceptions.AuthenticationException("Error during user registration: " + e.getMessage());
//        }
//    }
    public AuthResponse registerUser(RegisterRequest request) {
        try {
            System.out.println("Attempting to create user with email: " + request.getEmail());
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            System.out.println("Sending create user request to Firebase");
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
            System.out.println("User created successfully in Firebase with UID: " + userRecord.getUid());

            // Usa el UID generado por Firebase
            request.setUid(userRecord.getUid());
            UserResponse user = userService.createUser(request);

            List<String> rolesWithPrefix = user.getRoles().stream()
                    .map(role -> "ROLE_" + role.name())
                    .collect(Collectors.toList());

            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", rolesWithPrefix);
            FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);

            String customToken = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
            String idToken = exchangeCustomTokenForIdToken(customToken);

            return new AuthResponse(idToken, user);
        } catch (FirebaseAuthException e) {
            if (e.getErrorCode().equals("auth/email-already-exists")) {
                System.err.println("FirebaseAuthException occurred: " + e.getErrorCode() + " - " + e.getMessage());

                throw new CustomExceptions.EmailAlreadyExistsException("The user with the provided email already exists");
            }
            throw new CustomExceptions.AuthenticationException("Error during user registration: " + e.getMessage());
        }
    }
    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Obtener el usuario por correo electrónico
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
            UserResponse user = userService.getUserById(userRecord.getUid());

            // Validar la contraseña
            WebClient webClient = WebClient.builder().baseUrl("https://identitytoolkit.googleapis.com/v1").build();
            Map<String, Object> loginData = Map.of(
                    "email", request.getEmail(),
                    "password", request.getPassword(),
                    "returnSecureToken", true
            );

            Map<String, Object> response = webClient.post()
                    .uri("/accounts:signInWithPassword?key=" + firebaseApiKey)
                    .bodyValue(loginData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.containsKey("error")) {
                throw new CustomExceptions.InvalidCredentialsException("Invalid email or password.");
            }

            // Si la contraseña es válida, generar el token personalizado
            String customToken = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
            String idToken = exchangeCustomTokenForIdToken(customToken);

            return new AuthResponse(idToken, user);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Invalid email or password.");
        } catch (CustomExceptions.InvalidCredentialsException e) {
            throw new CustomExceptions.InvalidCredentialsException(e.getMessage());
        } catch (Exception e) {
            // Captura cualquier excepción inesperada para evitar el error 500 sin contexto
            //aqui esta el error cuando no se cumple correo y contraseña correctos
            throw new CustomExceptions.AuthenticationException("Invalid email or password." );
        }
    }



    private String exchangeCustomTokenForIdToken(String customToken) {
        return webClient.post()
                .uri("/accounts:signInWithCustomToken?key=" + firebaseApiKey)
                .bodyValue(Map.of("token", customToken, "returnSecureToken", true))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("idToken"))
                .block();
    }



    public void logout() {
        // Obtener el token actual del contexto de seguridad
        String token =  SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

        try {
            // Verificar y obtener el UID del token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
            String uid = decodedToken.getUid();

            // Revocar todos los tokens del usuario
            firebaseAuth.revokeRefreshTokens(uid);

            // Limpiar el contexto de seguridad
            SecurityContextHolder.clearContext();

        } catch (FirebaseAuthException e) {
            // Manejar cualquier error de autenticación
            throw new RuntimeException("Error al cerrar sesión", e);
        }
    }

    public AuthResponse refreshToken(String uid) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            UserResponse user = userService.getUserById(uid);

            // Generar nuevo token personalizado
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);

            return new AuthResponse(customToken, user);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error refreshing token: " + e.getMessage());
        }
    }

    public void resetPassword(String email) {
        try {
            FirebaseAuth.getInstance().generatePasswordResetLink(email);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error generating password reset link: " + e.getMessage());
        }
    }
}
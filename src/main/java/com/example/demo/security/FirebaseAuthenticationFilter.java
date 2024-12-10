package com.example.demo.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (isPublicEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String idToken = extractToken(request);
            if (idToken == null) {
                handleAuthenticationFailure(response, "No token provided");
                return;
            }

            FirebaseToken decodedToken = verifyToken(idToken);
            if (decodedToken == null) {
                handleAuthenticationFailure(response, "Invalid token");
                return;
            }
            if (isTokenExpired(decodedToken)) {
                handleAuthenticationFailure(response, "Token expired");
                return;
            }

            List<SimpleGrantedAuthority> authorities = extractAuthorities(decodedToken);
            setAuthenticationInContext(decodedToken, authorities);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            handleAuthenticationFailure(response, "Authentication failed");
        }
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/api/auth/login") ||
                path.contains("/api/auth/register") ||
                path.contains("/v3/api-docs") ||
                path.contains("/swagger-ui/") ||
                path.contains("/swagger-resources") ||
                path.contains("/configuration/") ||
                path.contains("/webjars/") ||
                path.contains("/swagger-ui.html") ||
                path.contains("favicon.ico");
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            logger.error("Error verifying Firebase token", e);
            return null;
        }
    }

    private List<SimpleGrantedAuthority> extractAuthorities(FirebaseToken decodedToken) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Object rolesObject = decodedToken.getClaims().get("roles");
        if (rolesObject instanceof List) {
            List<?> roles = (List<?>) rolesObject;
            for (Object role : roles) {
                if (role instanceof String) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()));
                }
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        }
        return authorities;
    }

    private void setAuthenticationInContext(FirebaseToken decodedToken, List<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                decodedToken.getUid(), null, authorities);
       // logger.info((Object) "Roles assigned to SecurityContext: {}. User ID: {}", (Throwable) authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

    }

    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String jsonResponse = String.format(
                "{\"success\":false,\"error\":{\"code\":\"AUTH_ERROR\",\"message\":\"%s\"}}",
                message
        );

        response.getWriter().write(jsonResponse);
    }
    private boolean isTokenExpired(FirebaseToken token) {
        try {
            // Obtener el tiempo de expiración desde los claims del token
            Long exp = (Long) token.getClaims().get("exp");
            if (exp == null) {
                return true; // Si no hay claim de expiración, consideramos el token como expirado
            }

            // Convertir a milisegundos (exp está en segundos)
            long expirationTimeMs = exp * 1000L;
            long currentTimeMs = System.currentTimeMillis();

            // Considerar el token expirado si está a 5 segundos o menos de expirar
            return (expirationTimeMs - currentTimeMs) <= 5000;
        } catch (Exception e) {
            logger.error("Error checking token expiration", e);
            return true; // En caso de error, consideramos el token como expirado
        }
    }

}
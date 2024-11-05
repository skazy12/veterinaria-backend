package com.example.demo.config;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;


@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

    @Autowired
    private Firestore firestore;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        //logger.info("ENTRA A LA FUNCION erronea");
        return hasPermission(authentication, null, null, permission);
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        logger.info("ENTRA A LA FUNCION");
        if (auth == null || !(permission instanceof String)) {
            return false;
        }
        String username = auth.getName();
        String permissionStr = (String) permission;

        try {
            // Obtener el documento del usuario
            DocumentSnapshot userDoc = firestore.collection("users").document(username).get().get();
            if (!userDoc.exists()) {
                logger.debug("User document not found for username: {}", username);
                return false;
            }

            // Obtener roles del usuario
            List<String> userRoles = (List<String>) userDoc.get("roles");
            logger.debug("Roles for user {}: {}", username, userRoles);
            if (userRoles == null || userRoles.isEmpty()) {
                logger.debug("No roles found for user: {}", username);
                return false;
            }

            // Iterar sobre los roles del usuario
            for (String roleName : userRoles) {
                logger.debug("Checking role by name: {}", roleName);

                // Buscar el rol basado en el campo "name" en lugar del ID del documento
                List<QueryDocumentSnapshot> roleDocs = firestore.collection("roles")
                        .whereEqualTo("name", roleName)
                        .get().get().getDocuments();

                if (!roleDocs.isEmpty()) {
                    DocumentSnapshot roleDoc = roleDocs.get(0);  // Tomar el primer documento si existe
                    List<String> permissions = (List<String>) roleDoc.get("permissions");
                    logger.debug("Permissions for role '{}': {}", roleName, permissions);
                    if (permissions != null && permissions.contains(permissionStr)) {
                        logger.info("Permission '{}' granted for user '{}' with role '{}'", permissionStr, username, roleName);
                        return true;
                    }
                } else {
                    logger.warn("Role not found for role name: {}", roleName);
                }
            }

            logger.info("Permission '{}' denied for user '{}'", permissionStr, username);
            return false;
        } catch (Exception e) {
            logger.error("Error checking permission: '{}' for user: '{}'", permissionStr, username, e);
            return false;
        }
    }

}
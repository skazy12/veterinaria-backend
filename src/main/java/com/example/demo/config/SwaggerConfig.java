package com.example.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Sistema Veterinario",
                version = "1.0",
                description = "API para la gestión de sistema veterinario",
                contact = @Contact(
                        name = "Soporte",
                        email = "soporte@veterinaria.com"
                )
        ),
        security = {
                @SecurityRequirement(name = "Bearer Auth")
        },
        servers = {
                @Server(
                        description = "Local ENV",
                        url = "http://localhost:8080"
                )
        }
)
@SecurityScheme(
        name = "Bearer Auth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {


}
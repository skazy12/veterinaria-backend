# Primera etapa: build
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Segunda etapa: imagen final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar las capas de la aplicación
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./

# Crear directorio para recursos
RUN mkdir -p /app/BOOT-INF/classes
COPY src/main/resources/firebase-service-account.json /app/BOOT-INF/classes/

# Dar permisos apropiados
RUN chmod 644 /app/BOOT-INF/classes/firebase-service-account.json

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
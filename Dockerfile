# spec-09: empaquetado multi-stage con Podman.
# Etapa 1: JDK 21 + Gradle wrapper -> fat JAR (shadowJar).
# Etapa 2: solo JRE 21, usuario no root y el JAR; sin herramientas de build.

FROM docker.io/library/eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Primero solo los ficheros de configuracion: esta capa se reutiliza mientras
# no cambien el build ni el wrapper (la distribucion Gradle se descarga aqui).
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties openapi.properties ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew --no-daemon --console=plain shadowJar

FROM docker.io/library/eclipse-temurin:21-jre
WORKDIR /app

# Usuario del sistema sin shell ni home escribible fuera de /app: no root.
RUN groupadd --system bankapi && \
    useradd --system --gid bankapi --home-dir /app --no-create-home bankapi

COPY --from=build /workspace/build/libs/bankaccount-0.1-all.jar /app/bankaccount.jar
RUN chown bankapi:bankapi /app/bankaccount.jar

USER bankapi

# micronaut.server.port=${SERVER_PORT:8080} en application.properties:
# permite publicar otro puerto dentro del contenedor sin reconstruir la imagen.
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/bankaccount.jar"]

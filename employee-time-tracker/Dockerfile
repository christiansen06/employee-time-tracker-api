# --- Etapa 1: build del jar con Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachea dependencias: primero el pom, luego el codigo.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

# --- Etapa 2: imagen liviana solo con el JRE para correr ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render inyecta PORT; la app ya lo lee desde application.properties.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

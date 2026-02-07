# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar pom y descargar deps
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar código
COPY src ./src

# Construir jar
RUN mvn -B package -DskipTests

# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiar jar del stage build
COPY --from=build /app/target/*-boot.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

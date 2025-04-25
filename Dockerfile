FROM gradle:8.13.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/RMP-product-service-all.jar RMP-product-service-all.jar
EXPOSE 8080

CMD ["java", "-jar", "RMP-product-service-all.jar"]

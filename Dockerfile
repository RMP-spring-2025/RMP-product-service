FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/RMP-product-service-all.jar RMP-product-service-all.jar

EXPOSE 8080

CMD ["java", "-jar", "RMP-product-service-all.jar"]
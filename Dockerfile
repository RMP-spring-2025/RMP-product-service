FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/RMP-product-service-all.jar RMP-product-service-all.jar
EXPOSE 8082

CMD ["java", "-jar", "RMP-product-service-all.jar"]

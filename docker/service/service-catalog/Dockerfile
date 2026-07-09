FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY service/service-catalog/target/service-catalog-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
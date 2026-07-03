FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY server/server-gateway/target/server-gateway-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
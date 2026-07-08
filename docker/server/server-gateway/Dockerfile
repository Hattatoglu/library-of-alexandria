FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY server/server-gateway/target/server-gateway-1.0.0.jar app.jar
EXPOSE 8080
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
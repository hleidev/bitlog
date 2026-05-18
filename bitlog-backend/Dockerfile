FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY bitlog-server/target/bitlog-server-*.jar app.jar
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

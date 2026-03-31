# Stage 1: 从 fat JAR 提取分层
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY target/bitlog-gateway-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: 最终运行镜像
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

USER spring

EXPOSE 9000

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]

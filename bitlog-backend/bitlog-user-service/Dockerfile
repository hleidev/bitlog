# Stage 1: 从 fat JAR 提取分层（依赖层 / 框架层 / 应用层分离）
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY target/bitlog-user-service-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: 最终运行镜像
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 非 root 用户运行（安全最佳实践）
RUN addgroup -S spring && adduser -S spring -G spring

# 按变更频率从低到高复制各层，充分利用 Docker 层缓存
# dependencies（第三方依赖）最少变动，最先 COPY
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
# application（业务代码）最常变动，最后 COPY
COPY --from=builder --chown=spring:spring /app/application/ ./

USER spring

EXPOSE 9001

# JVM 容器参数说明：
# UseContainerSupport：让 JVM 感知容器内存限制（而非宿主机内存）
# MaxRAMPercentage：最多使用容器内存的 75%
# java.security.egd：加速随机数生成，避免 SecureRandom 阻塞启动
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]

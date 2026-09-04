FROM eclipse-temurin:21-jre-alpine
# 关联 GHCR 镜像包与本仓库；GitHub 依据该标签确定包的 Repository source
LABEL org.opencontainers.image.source="https://github.com/hleidev/bitlog"
WORKDIR /app
COPY bitlog-server/target/bitlog-server-*.jar app.jar
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

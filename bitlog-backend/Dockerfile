FROM eclipse-temurin:21-jre-alpine
# 关联 GHCR 镜像包与本仓库；GitHub 依据该标签确定包的 Repository source
LABEL org.opencontainers.image.source="https://github.com/hleidev/bitlog"
# UID 写死 1000：宿主机 logs 目录的属主按数字匹配，容器里的用户名对不上
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
WORKDIR /app
COPY --chown=1000:1000 bitlog-server/target/bitlog-server-*.jar app.jar
USER 1000:1000
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

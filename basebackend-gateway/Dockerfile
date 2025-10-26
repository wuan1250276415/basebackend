# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /build

# 复制父POM和所有模块的POM文件（利用Docker缓存）
COPY pom.xml .
COPY basebackend-common/pom.xml basebackend-common/
COPY basebackend-jwt/pom.xml basebackend-jwt/
COPY basebackend-database/pom.xml basebackend-database/
COPY basebackend-cache/pom.xml basebackend-cache/
COPY basebackend-logging/pom.xml basebackend-logging/
COPY basebackend-security/pom.xml basebackend-security/
COPY basebackend-observability/pom.xml basebackend-observability/
COPY basebackend-messaging/pom.xml basebackend-messaging/
COPY basebackend-file-service/pom.xml basebackend-file-service/
COPY basebackend-backup/pom.xml basebackend-backup/
COPY basebackend-nacos/pom.xml basebackend-nacos/
COPY basebackend-gateway/pom.xml basebackend-gateway/
COPY basebackend-demo-api/pom.xml basebackend-demo-api/
COPY basebackend-gateway/pom.xml basebackend-gateway/

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B -DskipTests

# 复制源代码
COPY basebackend-common/src basebackend-common/src
COPY basebackend-jwt/src basebackend-jwt/src
COPY basebackend-database/src basebackend-database/src
COPY basebackend-cache/src basebackend-cache/src
COPY basebackend-logging/src basebackend-logging/src
COPY basebackend-security/src basebackend-security/src
COPY basebackend-observability/src basebackend-observability/src
COPY basebackend-messaging/src basebackend-messaging/src
COPY basebackend-file-service/src basebackend-file-service/src
COPY basebackend-backup/src basebackend-backup/src
COPY basebackend-nacos/src basebackend-nacos/src
COPY basebackend-gateway/src basebackend-gateway/src

# 构建项目（跳过测试以加快构建速度）
RUN mvn clean package -pl basebackend-gateway -am -DskipTests -B

# 解压jar文件以利用Spring Boot分层特性
WORKDIR /build/basebackend-gateway/target
RUN java -Djarmode=layertools -jar *.jar extract

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

# 元数据标签
LABEL maintainer="basebackend-team"
LABEL service="gateway"
LABEL version="1.0.0"

# 安装必要工具和字体（支持中文）
RUN apk add --no-cache \
    curl \
    tzdata \
    ttf-dejavu \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 创建非root用户
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# 创建应用目录
WORKDIR /app

# 从builder阶段复制分层的jar内容（优化镜像层缓存）
COPY --from=builder --chown=appuser:appuser /build/basebackend-gateway/target/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/basebackend-gateway/target/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /build/basebackend-gateway/target/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/basebackend-gateway/target/application/ ./

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM参数配置
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heap-dump.hprof"

# Spring Boot配置
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

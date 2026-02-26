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
COPY basebackend-web/pom.xml basebackend-web/
COPY basebackend-transaction/pom.xml basebackend-transaction/
COPY basebackend-security-starter/pom.xml basebackend-security-starter/
COPY basebackend-feign-api/pom.xml basebackend-feign-api/
COPY basebackend-feature-toggle/pom.xml basebackend-feature-toggle/
COPY basebackend-scheduler-parent/pom.xml basebackend-scheduler-parent/
COPY basebackend-code-generator/pom.xml basebackend-code-generator/
COPY basebackend-user-api/pom.xml basebackend-user-api/
COPY basebackend-system-api/pom.xml basebackend-system-api/
COPY basebackend-notification-service/pom.xml basebackend-notification-service/
COPY basebackend-observability-service/pom.xml basebackend-observability-service/
COPY basebackend-common/basebackend-common-core/pom.xml basebackend-common/basebackend-common-core/
COPY basebackend-common/basebackend-common-dto/pom.xml basebackend-common/basebackend-common-dto/
COPY basebackend-common/basebackend-common-util/pom.xml basebackend-common/basebackend-common-util/
COPY basebackend-common/basebackend-common-context/pom.xml basebackend-common/basebackend-common-context/
COPY basebackend-common/basebackend-common-security/pom.xml basebackend-common/basebackend-common-security/
COPY basebackend-common/basebackend-common-starter/pom.xml basebackend-common/basebackend-common-starter/
COPY basebackend-common/basebackend-common-storage/pom.xml basebackend-common/basebackend-common-storage/
COPY basebackend-scheduler-parent/scheduler-core/pom.xml basebackend-scheduler-parent/scheduler-core/
COPY basebackend-scheduler-parent/scheduler-workflow/pom.xml basebackend-scheduler-parent/scheduler-workflow/
COPY basebackend-scheduler-parent/scheduler-processor/pom.xml basebackend-scheduler-parent/scheduler-processor/
COPY basebackend-scheduler-parent/scheduler-metrics/pom.xml basebackend-scheduler-parent/scheduler-metrics/
COPY basebackend-scheduler-parent/scheduler-integration/pom.xml basebackend-scheduler-parent/scheduler-integration/

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B -DskipTests

# 复制源代码
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
COPY basebackend-web/src basebackend-web/src
COPY basebackend-transaction/src basebackend-transaction/src
COPY basebackend-security-starter/src basebackend-security-starter/src
COPY basebackend-feign-api/src basebackend-feign-api/src
COPY basebackend-feature-toggle/src basebackend-feature-toggle/src
COPY basebackend-code-generator/src basebackend-code-generator/src
COPY basebackend-user-api/src basebackend-user-api/src
COPY basebackend-system-api/src basebackend-system-api/src
COPY basebackend-notification-service/src basebackend-notification-service/src
COPY basebackend-observability-service/src basebackend-observability-service/src
COPY basebackend-common/basebackend-common-core/src basebackend-common/basebackend-common-core/src
COPY basebackend-common/basebackend-common-dto/src basebackend-common/basebackend-common-dto/src
COPY basebackend-common/basebackend-common-util/src basebackend-common/basebackend-common-util/src
COPY basebackend-common/basebackend-common-context/src basebackend-common/basebackend-common-context/src
COPY basebackend-common/basebackend-common-security/src basebackend-common/basebackend-common-security/src
COPY basebackend-common/basebackend-common-starter/src basebackend-common/basebackend-common-starter/src
COPY basebackend-common/basebackend-common-storage/src basebackend-common/basebackend-common-storage/src
COPY basebackend-scheduler-parent/scheduler-core/src basebackend-scheduler-parent/scheduler-core/src
COPY basebackend-scheduler-parent/scheduler-workflow/src basebackend-scheduler-parent/scheduler-workflow/src
COPY basebackend-scheduler-parent/scheduler-processor/src basebackend-scheduler-parent/scheduler-processor/src
COPY basebackend-scheduler-parent/scheduler-metrics/src basebackend-scheduler-parent/scheduler-metrics/src
COPY basebackend-scheduler-parent/scheduler-integration/src basebackend-scheduler-parent/scheduler-integration/src

# 构建项目（跳过测试）
# 使用全仓库打包以确保生成可执行的 Spring Boot fat jar
RUN mvn -Dmaven.test.skip=true clean package -B

# 打包完成后直接保留 jar 文件供运行镜像复制使用（不使用 layertools 提取）

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:25-jre-alpine

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
# Ensure logs directory exists for Logback file appenders
# Logback config writes to ./logs/*.log, so create and set ownership here
RUN mkdir -p /app/logs \
    && chown -R 1000:1000 /app/logs
# 从 builder 阶段复制打包好的 jar 到运行镜像
COPY --from=builder --chown=appuser:appuser /build/basebackend-gateway/target/*.jar /app/app.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM参数配置
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heap-dump.hprof"

# Spring Boot配置
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令（直接运行 jar）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

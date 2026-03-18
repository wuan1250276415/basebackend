# syntax=docker/dockerfile:1.7

# ==========================================
# BaseBackend Chat API — 多阶段构建 Dockerfile
# Java 25 + Spring Boot 4.0.3
# ==========================================

# 阶段 1: 构建
FROM maven:3.9.11-eclipse-temurin-25 AS builder
WORKDIR /build

# 复制所有 pom.xml（利用 Docker 缓存层）
COPY pom.xml .
COPY basebackend-common/pom.xml basebackend-common/
COPY basebackend-common/basebackend-common-core/pom.xml basebackend-common/basebackend-common-core/
COPY basebackend-common/basebackend-common-util/pom.xml basebackend-common/basebackend-common-util/
COPY basebackend-common/basebackend-common-context/pom.xml basebackend-common/basebackend-common-context/
COPY basebackend-common/basebackend-common-security/pom.xml basebackend-common/basebackend-common-security/
COPY basebackend-common/basebackend-common-starter/pom.xml basebackend-common/basebackend-common-starter/
COPY basebackend-common/basebackend-common-storage/pom.xml basebackend-common/basebackend-common-storage/
COPY basebackend-common/basebackend-common-lock/pom.xml basebackend-common/basebackend-common-lock/
COPY basebackend-common/basebackend-common-idempotent/pom.xml basebackend-common/basebackend-common-idempotent/
COPY basebackend-common/basebackend-common-datascope/pom.xml basebackend-common/basebackend-common-datascope/
COPY basebackend-common/basebackend-common-ratelimit/pom.xml basebackend-common/basebackend-common-ratelimit/
COPY basebackend-common/basebackend-common-export/pom.xml basebackend-common/basebackend-common-export/
COPY basebackend-common/basebackend-common-event/pom.xml basebackend-common/basebackend-common-event/
COPY basebackend-common/basebackend-common-masking/pom.xml basebackend-common/basebackend-common-masking/
COPY basebackend-common/basebackend-common-audit/pom.xml basebackend-common/basebackend-common-audit/
COPY basebackend-common/basebackend-common-tree/pom.xml basebackend-common/basebackend-common-tree/
COPY basebackend-jwt/pom.xml basebackend-jwt/
COPY basebackend-database/pom.xml basebackend-database/
COPY basebackend-database/database-core/pom.xml basebackend-database/database-core/
COPY basebackend-database/database-security/pom.xml basebackend-database/database-security/
COPY basebackend-database/database-multitenant/pom.xml basebackend-database/database-multitenant/
COPY basebackend-database/database-failover/pom.xml basebackend-database/database-failover/
COPY basebackend-database/database-migration/pom.xml basebackend-database/database-migration/
COPY basebackend-cache/pom.xml basebackend-cache/
COPY basebackend-cache/basebackend-cache-core/pom.xml basebackend-cache/basebackend-cache-core/
COPY basebackend-cache/basebackend-cache-advanced/pom.xml basebackend-cache/basebackend-cache-advanced/
COPY basebackend-cache/basebackend-cache-admin/pom.xml basebackend-cache/basebackend-cache-admin/
COPY basebackend-logging/pom.xml basebackend-logging/
COPY basebackend-logging/basebackend-logging-core/pom.xml basebackend-logging/basebackend-logging-core/
COPY basebackend-logging/basebackend-logging-monitoring/pom.xml basebackend-logging/basebackend-logging-monitoring/
COPY basebackend-logging/basebackend-logging-advanced/pom.xml basebackend-logging/basebackend-logging-advanced/
COPY basebackend-logging/basebackend-logging-audit/pom.xml basebackend-logging/basebackend-logging-audit/
COPY basebackend-security/pom.xml basebackend-security/
COPY basebackend-observability/pom.xml basebackend-observability/
COPY basebackend-observability/observability-core/pom.xml basebackend-observability/observability-core/
COPY basebackend-observability/observability-metrics/pom.xml basebackend-observability/observability-metrics/
COPY basebackend-observability/observability-alert/pom.xml basebackend-observability/observability-alert/
COPY basebackend-observability/observability-slo/pom.xml basebackend-observability/observability-slo/
COPY basebackend-observability/observability-logging/pom.xml basebackend-observability/observability-logging/
COPY basebackend-messaging/pom.xml basebackend-messaging/
COPY basebackend-nacos/pom.xml basebackend-nacos/
COPY basebackend-api-model/pom.xml basebackend-api-model/
COPY basebackend-service-client/pom.xml basebackend-service-client/
COPY basebackend-ai/pom.xml basebackend-ai/
COPY basebackend-search/pom.xml basebackend-search/
COPY basebackend-websocket/pom.xml basebackend-websocket/
COPY basebackend-workflow/pom.xml basebackend-workflow/
COPY basebackend-backup/pom.xml basebackend-backup/
COPY basebackend-code-generator/pom.xml basebackend-code-generator/
COPY basebackend-scheduler-parent/pom.xml basebackend-scheduler-parent/
COPY basebackend-scheduler-parent/scheduler-camunda/pom.xml basebackend-scheduler-parent/scheduler-camunda/
COPY basebackend-file-service/pom.xml basebackend-file-service/
COPY basebackend-chat-api/pom.xml basebackend-chat-api/
COPY basebackend-gateway/pom.xml basebackend-gateway/
COPY basebackend-user-api/pom.xml basebackend-user-api/
COPY basebackend-system-api/pom.xml basebackend-system-api/
COPY basebackend-notification-service/pom.xml basebackend-notification-service/
COPY basebackend-observability-service/pom.xml basebackend-observability-service/

# 下载依赖
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -DskipTests 2>/dev/null || true

# 复制源代码并构建
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl basebackend-chat-api -am -DskipTests -Dmaven.test.skip=true -B

# ==========================================
# 阶段 2: 运行
# ==========================================
FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="basebackend-team"
LABEL service="chat-api"

# 创建非 root 用户
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

WORKDIR /app
RUN mkdir -p /app/logs && chown -R 1000:1000 /app/logs

# 复制构建产物
COPY --from=builder --chown=appuser:appuser /build/basebackend-chat-api/target/*.jar /app/app.jar

USER appuser
EXPOSE 8086

# JVM 参数：ZGC 分代模式 + 内存限制
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Duser.timezone=Asia/Shanghai -XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heap-dump.hprof"
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8086

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

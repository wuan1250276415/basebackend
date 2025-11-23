# =====================================================
# BaseBackend Scheduler - Docker 构建文件
# 版本: 1.0
# 描述: 构建 basebackend-scheduler 微服务镜像
# 技术栈: Java 17, Spring Boot 3.1.5, Camunda 7.21
# =====================================================

# 第一阶段：构建应用
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=latest

# 设置元数据标签
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/basebackend/basebackend"
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.title="BaseBackend Scheduler Service"
LABEL org.opencontainers.image.description="BaseBackend Scheduler微服务 - 基于Camunda的工作流引擎和任务调度"
LABEL org.opencontainers.image.vendor="BaseBackend Team"

# 设置工作目录
WORKDIR /build

# 复制 Maven 配置文件
COPY pom.xml .

# 复制 Maven 配置目录（如果存在）
COPY settings.xml .

# 下载依赖（利用 Docker 层缓存）
# 由于 Maven 的复杂依赖管理，先下载依赖到本地
RUN mvn dependency:go-offline -B -q

# 复制源代码
COPY src ./src

# 构建应用
# -DskipTests=true: 跳过测试（生产环境）
# -Dmaven.test.skip=true: 跳过测试编译
# -Dcheckstyle.skip=true: 跳过代码规范检查
# -Dmaven.javadoc.skip=true: 跳过文档生成
RUN mvn clean package -DskipTests=true -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -B -q

# 验证 JAR 文件是否存在
RUN test -f /build/target/*.jar || (echo "JAR file not found in target directory" && exit 1)

# 第二阶段：运行环境
FROM eclipse-temurin:17-jre-alpine

# 设置构建参数和元数据
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=latest

LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/basebackend/basebackend"
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.title="BaseBackend Scheduler Service"
LABEL org.opencontainers.image.description="BaseBackend Scheduler微服务 - 基于Camunda的工作流引擎和任务调度"
LABEL org.opencontainers.image.vendor="BaseBackend Team"

# 安装必要的工具和依赖
# tzdata: 时区数据库
# curl: 用于健康检查和调试
# bash: 提供更好的 shell 环境
# ca-certificates: HTTPS 证书
RUN apk update && apk upgrade && \
    apk add --no-cache \
        tzdata \
        curl \
        bash \
        ca-certificates && \
    rm -rf /var/cache/apk/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户和组
# 使用非 root 用户运行应用，提高安全性
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 创建应用目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/target/*.jar app.jar

# 验证 JAR 文件
RUN test -f app.jar && echo "JAR file found: $(ls -lh app.jar)" || (echo "JAR file not found!" && exit 1)

# 创建日志目录
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 切换到应用用户
USER appuser

# 暴露端口
# 8080: Spring Boot 应用端口
# 8090: Camunda Web UI 端口
EXPOSE 8080 8090

# 设置 JVM 参数
# -Xms: 初始堆大小（生产环境建议设置）
# -Xmx: 最大堆大小（生产环境建议设置）
# -XX:+UseG1GC: 使用 G1 垃圾收集器（适合大堆内存）
# -XX:+UseContainerSupport: 支持容器环境
# -XX:MaxRAMPercentage: 基于容器内存限制的百分比
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 健康检查
# 检查应用是否正常运行
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
# 使用 exec 形式启动，可以正确处理信号
# 优雅关闭：Spring Boot 会自动处理 SIGTERM 信号
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 启动说明
# 默认启动：docker run -p 8080:8080 -p 8090:8090 basebackend-scheduler
# 自定义配置：docker run -p 8080:8080 -p 8090:8090 -e SPRING_PROFILES_ACTIVE=prod basebackend-scheduler
# 自定义 JVM：docker run -p 8080:8080 -p 8090:8090 -e JAVA_OPTS='-Xms1024m -Xmx2048m' basebackend-scheduler

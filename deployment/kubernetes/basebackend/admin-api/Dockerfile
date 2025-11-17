# 多阶段构建 - BaseBackend Admin API
# 阶段1: 构建阶段
FROM maven:3.9-openjdk-17-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY basebackend-admin-api/pom.xml .
COPY basebackend-admin-api/settings.xml ./settings.xml

# 下载依赖 (利用Docker缓存)
RUN mvn dependency:go-offline -B -s ./settings.xml

# 复制源代码
COPY basebackend-admin-api/src ./src

# 构建应用
RUN mvn clean package -DskipTests -B -s ./settings.xml

# 阶段2: 运行环境
FROM openjdk:17-jre-slim

# 创建应用用户和组
RUN groupadd --system --gid 1000 spring && \
    useradd --system --uid 1000 --gid spring --shell /bin/false spring

# 安装必要工具
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 切换到非root用户
USER spring:spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Duser.timezone=Asia/Shanghai", \
  "-Dfile.encoding=UTF-8", \
  "-Dspring.profiles.active=kubernetes", \
  "-XX:+UseG1GC", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UnlockExperimentalVMOptions", \
  "-XX:+UseCGroupMemoryLimitForHeap", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/heap_dump.hprof", \
  "-Xss512k", \
  "-jar", "/app.jar"]

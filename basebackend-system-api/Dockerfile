# 多阶段构建 - 构建阶段
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 复制父POM和依赖模块
COPY pom.xml .
COPY basebackend-common ./basebackend-common
COPY basebackend-database ./basebackend-database
COPY basebackend-cache ./basebackend-cache
COPY basebackend-web ./basebackend-web
COPY basebackend-logging ./basebackend-logging
COPY basebackend-system-api ./basebackend-system-api

# 构建项目
RUN mvn clean package -pl basebackend-system-api -am -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 添加非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 复制构建产物
COPY --from=builder /app/basebackend-system-api/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

EXPOSE 8082

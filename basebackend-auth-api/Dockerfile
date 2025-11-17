# 多阶段构建 - 构建阶段
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 复制父POM和依赖模块
COPY pom.xml .
COPY basebackend-common ./basebackend-common
COPY basebackend-cache ./basebackend-cache
COPY basebackend-security ./basebackend-security
COPY basebackend-jwt ./basebackend-jwt
COPY basebackend-web ./basebackend-web
COPY basebackend-auth-api ./basebackend-auth-api

# 构建项目
RUN mvn clean package -pl basebackend-auth-api -am -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 添加非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 复制构建产物
COPY --from=builder /app/basebackend-auth-api/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

EXPOSE 8083

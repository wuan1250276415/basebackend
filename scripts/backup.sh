#!/bin/bash
# Production Backup Script
# 使用方法: ./backup.sh [backup_type]
# backup_type: full (默认) | incremental

set -euo pipefail

BACKUP_TYPE=${1:-full}
BACKUP_DIR="/backup/$(date +%Y%m%d_%H%M%S)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "==========================================="
echo "开始执行生产环境备份 - $TIMESTAMP"
echo "备份类型: $BACKUP_TYPE"
echo "备份目录: $BACKUP_DIR"
echo "==========================================="

# 创建备份目录
mkdir -p "$BACKUP_DIR"
cd "$BACKUP_DIR"

# 1. 数据库备份
echo "[1/6] 备份 PostgreSQL 数据库..."
docker exec basebackend-postgres-prod-primary pg_dump \
  -U basebackend \
  -d basebackend_prod \
  --verbose \
  --clean \
  --no-owner \
  --no-privileges \
  > "database_${TIMESTAMP}.sql"

if [ $? -eq 0 ]; then
  echo "✓ 数据库备份成功"
else
  echo "✗ 数据库备份失败"
  exit 1
fi

# 2. Redis 数据备份
echo "[2/6] 备份 Redis 数据..."
docker exec basebackend-redis-prod redis-cli --rdb - > "redis_${TIMESTAMP}.rdb"
if [ $? -eq 0 ]; then
  echo "✓ Redis 备份成功"
else
  echo "✗ Redis 备份失败"
  exit 1
fi

# 3. 配置文件备份
echo "[3/6] 备份配置文件..."
mkdir -p config
tar -czf "config_${TIMESTAMP}.tar.gz" \
  ../config \
  ../nginx/nginx.production.conf \
  ../docker-compose.production.yml \
  2>/dev/null || true

# 4. 日志文件备份
echo "[4/6] 备份重要日志..."
mkdir -p logs
tar -czf "logs_${TIMESTAMP}.tar.gz" \
  ../logs/scheduler \
  ../logs/nginx \
  2>/dev/null || true

# 5. Docker Compose 状态
echo "[5/6] 备份 Docker Compose 状态..."
docker-compose -f ../docker-compose.production.yml ps > "container_status_${TIMESTAMP}.txt"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" > "running_containers_${TIMESTAMP}.txt"

# 6. 健康检查状态
echo "[6/6] 记录服务健康状态..."
cat > "health_check_${TIMESTAMP}.txt" <<EOF
# 服务健康状态检查
# 时间: $TIMESTAMP

## 数据库连接状态
$(docker exec basebackend-postgres-prod-primary pg_isready -U basebackend)

## Redis 连接状态
$(docker exec basebackend-redis-prod redis-cli ping 2>/dev/null || echo "Redis 连接失败")

## 应用服务状态
$(curl -s http://localhost:8080/actuator/health || echo "服务未响应")

## 磁盘使用情况
$(df -h /)

## 内存使用情况
$(free -h)

## Docker 容器资源使用
$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" || true)
EOF

# 7. 生成备份清单
echo "[7/7] 生成备份清单..."
cat > "BACKUP_MANIFEST_${TIMESTAMP}.txt" <<EOF
备份时间: $TIMESTAMP
备份类型: $BACKUP_TYPE
主机名: $(hostname)
备份目录: $(pwd)
备份内容:
  - database_${TIMESTAMP}.sql (PostgreSQL 数据库)
  - redis_${TIMESTAMP}.rdb (Redis 数据)
  - config_${TIMESTAMP}.tar.gz (配置文件)
  - logs_${TIMESTAMP}.tar.gz (日志文件)
  - container_status_${TIMESTAMP}.txt (容器状态)
  - running_containers_${TIMESTAMP}.txt (运行容器)
  - health_check_${TIMESTAMP}.txt (健康状态)

验证命令:
  - 数据库恢复: docker exec -i basebackend-postgres-prod-primary psql -U basebackend -d basebackend_prod < database_${TIMESTAMP}.sql
  - Redis 恢复: docker cp redis_${TIMESTAMP}.rdb basebackend-redis-prod:/data/dump.rdb

注意事项:
  1. 备份完成后请立即验证备份文件完整性
  2. 建议将备份文件复制到异地存储
  3. 恢复前请确保服务已停止
  4. 定期清理旧备份文件

备份人: CI/CD 自动化
EOF

# 8. 压缩整个备份
echo "[8/8] 压缩备份文件..."
tar -czf "/backup/basebackend_backup_${TIMESTAMP}.tar.gz" .
rm -rf "$BACKUP_DIR"

# 9. 计算校验和
cd /backup
sha256sum "basebackend_backup_${TIMESTAMP}.tar.gz" > "basebackend_backup_${TIMESTAMP}.sha256"

# 10. 清理旧备份（保留最近7天）
echo "[9/9] 清理旧备份文件..."
find /backup -name "basebackend_backup_*.tar.gz" -mtime +7 -delete

# 11. 生成备份摘要
echo "==========================================="
echo "备份完成!"
echo "==========================================="
echo "备份文件: basebackend_backup_${TIMESTAMP}.tar.gz"
echo "校验和文件: basebackend_backup_${TIMESTAMP}.sha256"
echo "文件大小: $(ls -lh basebackend_backup_${TIMESTAMP}.tar.gz | awk '{print $5}')"
echo "备份位置: /backup/"
echo "==========================================="

# 12. 发送通知（可选）
if command -v curl &> /dev/null; then
  curl -X POST "${SLACK_WEBHOOK_URL:-}" \
    -H 'Content-type: application/json' \
    --data "{\"text\":\"✅ 生产环境备份完成: basebackend_backup_${TIMESTAMP}.tar.gz\"}" \
    2>/dev/null || true
fi

exit 0

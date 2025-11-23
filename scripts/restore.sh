#!/bin/bash
# Production Restore Script
# 使用方法: ./restore.sh <backup_file> [service]
# backup_file: 备份文件名 (例如: basebackend_backup_20241124_120000.tar.gz)
# service: all (默认) | database | redis | config

set -euo pipefail

BACKUP_FILE=${1:-}
SERVICE=${2:-all}
BACKUP_DIR="/tmp/restore_$(date +%Y%m%d_%H%M%S)"

if [ -z "$BACKUP_FILE" ]; then
  echo "错误: 请指定备份文件"
  echo "使用方法: $0 <backup_file> [service]"
  echo "可用的服务: all, database, redis, config"
  exit 1
fi

echo "==========================================="
echo "开始执行生产环境恢复 - $(date)"
echo "备份文件: $BACKUP_FILE"
echo "恢复服务: $SERVICE"
echo "==========================================="

# 1. 验证备份文件
if [ ! -f "/backup/$BACKUP_FILE" ]; then
  echo "错误: 备份文件不存在: /backup/$BACKUP_FILE"
  exit 1
fi

# 验证校验和
if [ -f "/backup/${BACKUP_FILE}.sha256" ]; then
  echo "验证备份文件完整性..."
  cd /backup
  if sha256sum -c "${BACKUP_FILE}.sha256" --status; then
    echo "✓ 备份文件完整性验证通过"
  else
    echo "✗ 备份文件完整性验证失败"
    exit 1
  fi
  cd - > /dev/null
fi

# 2. 提取备份文件
echo "[1/5] 提取备份文件..."
mkdir -p "$BACKUP_DIR"
tar -xzf "/backup/$BACKUP_FILE" -C "$BACKUP_DIR"

# 3. 根据服务类型执行恢复
case "$SERVICE" in
  "database"|"all")
    echo "[2/5] 恢复 PostgreSQL 数据库..."
    SQL_FILE=$(find "$BACKUP_DIR" -name "database_*.sql" | head -n 1)
    if [ -n "$SQL_FILE" ]; then
      echo "停止应用服务..."
      docker-compose -f ../docker-compose.production.yml stop scheduler scheduler-secondary admin-api gateway

      echo "恢复数据库..."
      docker exec -i basebackend-postgres-prod-primary psql -U basebackend -d basebackend_prod < "$SQL_FILE"

      echo "✓ 数据库恢复完成"
    else
      echo "警告: 未找到数据库备份文件"
    fi
    ;;

  "redis"|"all")
    echo "[3/5] 恢复 Redis 数据..."
    RDB_FILE=$(find "$BACKUP_DIR" -name "redis_*.rdb" | head -n 1)
    if [ -n "$RDB_FILE" ]; then
      echo "停止 Redis 服务..."
      docker-compose -f ../docker-compose.production.yml stop redis

      echo "恢复 Redis 数据..."
      docker cp "$RDB_FILE" basebackend-redis-prod:/data/dump.rdb
      docker start basebackend-redis-prod

      echo "等待 Redis 启动..."
      sleep 10

      echo "✓ Redis 恢复完成"
    else
      echo "警告: 未找到 Redis 备份文件"
    fi
    ;;

  "config"|"all")
    echo "[4/5] 恢复配置文件..."
    CONFIG_TAR=$(find "$BACKUP_DIR" -name "config_*.tar.gz" | head -n 1)
    if [ -n "$CONFIG_TAR" ]; then
      tar -xzf "$CONFIG_TAR" -C ../
      echo "✓ 配置文件恢复完成"
    else
      echo "警告: 未找到配置文件备份"
    fi
    ;;
esac

# 4. 重新启动所有服务
if [ "$SERVICE" == "all" ]; then
  echo "[5/5] 重新启动所有服务..."
  docker-compose -f ../docker-compose.production.yml up -d

  echo "等待服务启动..."
  for i in {1..60}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
      echo "✓ 服务启动成功"
      break
    fi
    echo "等待服务启动... ($i/60)"
    sleep 10
  done
fi

# 5. 健康检查
echo "[6/6] 执行健康检查..."
echo "检查数据库连接..."
docker exec basebackend-postgres-prod-primary pg_isready -U basebackend

echo "检查 Redis 连接..."
docker exec basebackend-redis-prod redis-cli ping

echo "检查应用服务..."
curl -s http://localhost:8080/actuator/health | jq '.' || echo "服务健康检查完成"

# 6. 生成恢复报告
cat > "/backup/restore_report_$(date +%Y%m%d_%H%M%S).txt" <<EOF
恢复报告
========
时间: $(date)
备份文件: $BACKUP_FILE
恢复服务: $SERVICE
操作人: 手动恢复

恢复项目:
$(if [[ "$SERVICE" == "database" || "$SERVICE" == "all" ]]; then echo "  ✓ PostgreSQL 数据库"; fi)
$(if [[ "$SERVICE" == "redis" || "$SERVICE" == "all" ]]; then echo "  ✓ Redis 数据"; fi)
$(if [[ "$SERVICE" == "config" || "$SERVICE" == "all" ]]; then echo "  ✓ 配置文件"; fi)

服务状态:
EOF

docker-compose -f ../docker-compose.production.yml ps >> "/backup/restore_report_$(date +%Y%m%d_%H%M%S).txt"

# 清理临时文件
echo "清理临时文件..."
rm -rf "$BACKUP_DIR"

echo "==========================================="
echo "恢复完成!"
echo "==========================================="
echo "请检查服务是否正常运行:"
echo "  - 数据库: docker exec basebackend-postgres-prod-primary pg_isready -U basebackend"
echo "  - Redis: docker exec basebackend-redis-prod redis-cli ping"
echo "  - 应用: curl http://localhost:8080/actuator/health"
echo "==========================================="

exit 0

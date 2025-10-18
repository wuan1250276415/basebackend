#!/bin/bash

# Nacos 配置上传脚本

NACOS_SERVER="localhost:8848"
NACOS_NAMESPACE="public"
NACOS_GROUP="DEFAULT_GROUP"

echo "=== 上传 Nacos 配置 ==="

# 检查 Nacos 是否运行
if ! curl -s http://$NACOS_SERVER/nacos/v1/ns/operator/servers > /dev/null; then
    echo "❌ Nacos 服务未启动，请先启动 Nacos"
    exit 1
fi

# 上传通用配置
echo "上传通用配置..."
curl -X POST "http://$NACOS_SERVER/nacos/v1/cs/configs" \
  -d "dataId=common-config.yml" \
  -d "group=$NACOS_GROUP" \
  -d "namespaceId=$NACOS_NAMESPACE" \
  -d "content=$(cat nacos-configs/common-config.yml | sed 's/"/\\"/g' | tr '\n' '\\n')" \
  -d "type=yaml"

# 上传 Gateway 配置
echo "上传 Gateway 配置..."
curl -X POST "http://$NACOS_SERVER/nacos/v1/cs/configs" \
  -d "dataId=gateway-config.yml" \
  -d "group=$NACOS_GROUP" \
  -d "namespaceId=$NACOS_NAMESPACE" \
  -d "content=$(cat nacos-configs/gateway-config.yml | sed 's/"/\\"/g' | tr '\n' '\\n')" \
  -d "type=yaml"

# 上传 Demo-API 配置
echo "上传 Demo-API 配置..."
curl -X POST "http://$NACOS_SERVER/nacos/v1/cs/configs" \
  -d "dataId=demo-api-config.yml" \
  -d "group=$NACOS_GROUP" \
  -d "namespaceId=$NACOS_NAMESPACE" \
  -d "content=$(cat nacos-configs/demo-api-config.yml | sed 's/"/\\"/g' | tr '\n' '\\n')" \
  -d "type=yaml"

echo "✅ 配置上传完成！"
echo "访问 Nacos 控制台: http://$NACOS_SERVER/nacos"

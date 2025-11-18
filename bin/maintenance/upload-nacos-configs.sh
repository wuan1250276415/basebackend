#!/bin/bash
# ============================================================================
# Nacos配置上传脚本 (Linux/Mac)
# ============================================================================
# 功能：将config/nacos目录下的配置文件上传到Nacos配置中心
# 使用：./upload-nacos-configs.sh [nacos-server-url]
# ============================================================================

set -e

# 设置Nacos服务器地址
NACOS_SERVER=${1:-"http://localhost:8848"}
NACOS_USERNAME="nacos"
NACOS_PASSWORD="nacos"
GROUP="DEFAULT_GROUP"
CONFIG_DIR="config/nacos"

echo ""
echo "========================================"
echo "  Nacos配置上传脚本"
echo "========================================"
echo ""
echo "Nacos服务器: $NACOS_SERVER"
echo "配置目录: $CONFIG_DIR"
echo ""

# 检查配置目录
if [ ! -d "$CONFIG_DIR" ]; then
    echo "[错误] 配置目录不存在: $CONFIG_DIR"
    exit 1
fi

# 检查curl是否可用
if ! command -v curl &> /dev/null; then
    echo "[错误] curl未安装"
    exit 1
fi

# 上传配置文件
count=0
for file in "$CONFIG_DIR"/*.yml; do
    if [ -f "$file" ]; then
        count=$((count + 1))
        filename=$(basename "$file")
        echo "[$count] 上传配置: $filename"
        
        # 读取文件内容
        content=$(cat "$file")
        
        # 上传到Nacos
        response=$(curl -s -w "\n%{http_code}" -X POST "$NACOS_SERVER/nacos/v1/cs/configs" \
            -d "dataId=$filename" \
            -d "group=$GROUP" \
            --data-urlencode "content=$content" \
            -d "type=yaml" \
            -d "username=$NACOS_USERNAME" \
            -d "password=$NACOS_PASSWORD")
        
        http_code=$(echo "$response" | tail -n1)
        
        if [ "$http_code" = "200" ]; then
            echo "   [✓] 上传成功"
        else
            echo "   [✗] 上传失败 (HTTP $http_code)"
        fi
    fi
done

echo ""
echo "========================================"
echo "  配置上传完成！共上传 $count 个文件"
echo "========================================"
echo ""
echo "访问Nacos控制台查看配置："
echo "$NACOS_SERVER/nacos"
echo ""

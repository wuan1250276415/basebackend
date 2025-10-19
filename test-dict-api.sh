#!/bin/bash

# 字典管理API测试脚本

BASE_URL="http://localhost:8082/api/admin"
TOKEN=""

echo "=== 字典管理API测试 ==="
echo ""

# 如果需要token，先登录获取
echo "1. 登录获取Token..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，无法获取Token"
  exit 1
fi

echo "✅ 登录成功，Token: ${TOKEN:0:20}..."
echo ""

# 2. 分页查询字典列表
echo "2. 分页查询字典列表..."
curl -s -X GET "${BASE_URL}/dicts?current=1&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.' || echo "请求失败"
echo ""

# 3. 创建字典
echo "3. 创建测试字典..."
CREATE_DICT_RESPONSE=$(curl -s -X POST "${BASE_URL}/dicts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dictName": "测试状态",
    "dictType": "test_status",
    "status": 1,
    "remark": "测试用字典"
  }')

echo $CREATE_DICT_RESPONSE | jq '.' || echo "请求失败"
echo ""

# 4. 创建字典数据
echo "4. 创建字典数据..."
curl -s -X POST "${BASE_URL}/dicts/data" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dictType": "test_status",
    "dictLabel": "启用",
    "dictValue": "1",
    "dictSort": 1,
    "status": 1,
    "isDefault": 1
  }' | jq '.' || echo "请求失败"
echo ""

curl -s -X POST "${BASE_URL}/dicts/data" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "dictType": "test_status",
    "dictLabel": "禁用",
    "dictValue": "0",
    "dictSort": 2,
    "status": 1,
    "isDefault": 0
  }' | jq '.' || echo "请求失败"
echo ""

# 5. 根据类型查询字典数据（会从缓存获取）
echo "5. 根据类型查询字典数据（从Redis缓存）..."
curl -s -X GET "${BASE_URL}/dicts/data/type/test_status" \
  -H "Authorization: Bearer $TOKEN" | jq '.' || echo "请求失败"
echo ""

# 6. 刷新缓存
echo "6. 刷新字典缓存..."
curl -s -X POST "${BASE_URL}/dicts/refresh-cache" \
  -H "Authorization: Bearer $TOKEN" | jq '.' || echo "请求失败"
echo ""

echo "=== 测试完成 ==="
echo ""
echo "提示："
echo "1. 查看 Redis 缓存: redis-cli keys 'sys:dict:*'"
echo "2. 获取缓存数据: redis-cli get 'sys:dict:test_status'"
echo "3. 访问前端页面: http://localhost:3000/system/dict"

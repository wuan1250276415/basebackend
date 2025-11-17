#!/bin/bash

# 角色菜单权限测试脚本

BASE_URL="http://localhost:8082/api/admin"
TOKEN=""

echo "=== 角色菜单权限功能测试 ==="
echo ""

# 1. 管理员登录
echo "1. 管理员登录..."
ADMIN_LOGIN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

ADMIN_TOKEN=$(echo $ADMIN_LOGIN | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ 管理员登录失败"
  exit 1
fi

echo "✅ 管理员登录成功"
echo ""

# 2. 查询所有菜单（管理员）
echo "2. 查询管理员的菜单树..."
curl -s -X GET "${BASE_URL}/menus/current-user" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data | length'
echo "✅ 管理员可见菜单数量"
echo ""

# 3. 创建测试角色
echo "3. 创建测试角色..."
CREATE_ROLE=$(curl -s -X POST "${BASE_URL}/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roleName": "测试角色",
    "roleKey": "test_role",
    "roleSort": 10,
    "dataScope": 1,
    "status": 1,
    "remark": "用于测试权限的角色"
  }')

echo $CREATE_ROLE | jq '.'
echo ""

# 4. 查询角色列表，获取测试角色ID
echo "4. 查询角色列表..."
ROLES=$(curl -s -X GET "${BASE_URL}/roles?current=1&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo $ROLES | jq '.data.records[] | select(.roleKey=="test_role") | {id, roleName}'
TEST_ROLE_ID=$(echo $ROLES | jq -r '.data.records[] | select(.roleKey=="test_role") | .id')
echo "测试角色ID: $TEST_ROLE_ID"
echo ""

# 5. 查询菜单列表
echo "5. 查询菜单列表..."
MENUS=$(curl -s -X GET "${BASE_URL}/menus" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

DASHBOARD_MENU_ID=$(echo $MENUS | jq -r '.data[] | select(.path=="/dashboard") | .id')
echo "仪表盘菜单ID: $DASHBOARD_MENU_ID"
echo ""

# 6. 为测试角色分配菜单权限（只分配仪表盘）
echo "6. 为测试角色分配菜单权限（仅仪表盘）..."
if [ ! -z "$TEST_ROLE_ID" ] && [ ! -z "$DASHBOARD_MENU_ID" ]; then
  curl -s -X PUT "${BASE_URL}/roles/${TEST_ROLE_ID}/menus" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[\"${DASHBOARD_MENU_ID}\"]" | jq '.'
  echo ""
fi

# 7. 创建测试用户
echo "7. 创建测试用户..."
CREATE_USER=$(curl -s -X POST "${BASE_URL}/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123",
    "nickname": "测试用户",
    "email": "test@example.com",
    "status": 1
  }')

echo $CREATE_USER | jq '.'
echo ""

# 8. 查询用户列表，获取测试用户ID
echo "8. 查询测试用户ID..."
USERS=$(curl -s -X GET "${BASE_URL}/users?current=1&size=20&username=testuser" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

TEST_USER_ID=$(echo $USERS | jq -r '.data.records[] | select(.username=="testuser") | .id')
echo "测试用户ID: $TEST_USER_ID"
echo ""

# 9. 为测试用户分配角色
echo "9. 为测试用户分配角色..."
if [ ! -z "$TEST_USER_ID" ] && [ ! -z "$TEST_ROLE_ID" ]; then
  curl -s -X PUT "${BASE_URL}/users/${TEST_USER_ID}/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[\"${TEST_ROLE_ID}\"]" | jq '.'
  echo ""
fi

# 10. 使用测试用户登录
echo "10. 测试用户登录..."
TEST_LOGIN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123"
  }')

TEST_TOKEN=$(echo $TEST_LOGIN | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TEST_TOKEN" ]; then
  echo "❌ 测试用户登录失败"
else
  echo "✅ 测试用户登录成功"
fi
echo ""

# 11. 查询测试用户的菜单
echo "11. 查询测试用户的菜单树..."
TEST_MENUS=$(curl -s -X GET "${BASE_URL}/menus/current-user" \
  -H "Authorization: Bearer $TEST_TOKEN")

echo $TEST_MENUS | jq '.'
TEST_MENU_COUNT=$(echo $TEST_MENUS | jq '.data | length')
echo ""
echo "测试用户可见菜单数量: $TEST_MENU_COUNT"
echo ""

# 12. 对比结果
echo "=== 测试结果对比 ==="
echo "管理员菜单数量: 应该 > 5（有完整权限）"
echo "测试用户菜单数量: $TEST_MENU_COUNT（应该只有1个仪表盘）"
echo ""

if [ "$TEST_MENU_COUNT" -eq "1" ]; then
  echo "✅ 权限控制正常！测试用户只能看到被分配的菜单"
else
  echo "⚠️  测试用户菜单数量异常，请检查权限配置"
fi

echo ""
echo "=== 测试完成 ==="
echo ""
echo "提示："
echo "1. 前端登录测试用户: testuser / test123"
echo "2. 应该只能看到仪表盘菜单"
echo "3. 其他菜单不会显示"
echo "4. 清理测试数据（可选）:"
echo "   - 删除测试用户: DELETE ${BASE_URL}/users/${TEST_USER_ID}"
echo "   - 删除测试角色: DELETE ${BASE_URL}/roles/${TEST_ROLE_ID}"

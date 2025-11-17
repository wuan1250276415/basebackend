#!/bin/bash

echo "测试后台管理API登录接口..."

# 测试直接访问admin-api
echo "1. 测试直接访问admin-api (端口8082)..."
curl -X POST http://localhost:8082/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s

echo ""
echo "2. 测试通过网关访问admin-api (端口8180)..."
curl -X POST http://localhost:8180/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s

echo ""
echo "3. 测试Swagger文档访问..."
curl -I http://localhost:8082/doc.html 2>/dev/null | head -1
curl -I http://localhost:8180/api/admin/doc.html 2>/dev/null | head -1

echo ""
echo "测试完成！"

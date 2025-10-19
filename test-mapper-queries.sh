#!/bin/bash

echo "测试 Mapper 查询语句..."

# 测试数据库连接
echo "1. 测试数据库连接..."
mysql -h localhost -P 3306 -u root -p -e "USE basebackend_admin; SELECT COUNT(*) as user_count FROM sys_user;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ 数据库连接正常"
else
    echo "❌ 数据库连接失败，请检查数据库配置"
    exit 1
fi

# 测试用户查询
echo ""
echo "2. 测试用户查询..."
mysql -h localhost -P 3306 -u root -p basebackend_admin -e "
SELECT 
    id, username, nickname, email, phone, status, create_time
FROM sys_user 
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 5;
" 2>/dev/null

# 测试角色查询
echo ""
echo "3. 测试角色查询..."
mysql -h localhost -P 3306 -u root -p basebackend_admin -e "
SELECT 
    r.id, r.role_name, r.role_key, r.status, r.create_time
FROM sys_role r
WHERE r.deleted = 0
ORDER BY r.create_time DESC;
" 2>/dev/null

# 测试菜单查询
echo ""
echo "4. 测试菜单查询..."
mysql -h localhost -P 3306 -u root -p basebackend_admin -e "
SELECT 
    m.id, m.menu_name, m.parent_id, m.menu_type, m.status, m.order_num
FROM sys_menu m
WHERE m.deleted = 0
ORDER BY m.order_num;
" 2>/dev/null

# 测试权限查询
echo ""
echo "5. 测试权限查询..."
mysql -h localhost -P 3306 -u root -p basebackend_admin -e "
SELECT 
    p.id, p.permission_name, p.permission_key, p.permission_type, p.status
FROM sys_permission p
WHERE p.deleted = 0
ORDER BY p.create_time;
" 2>/dev/null

# 测试用户角色关联查询
echo ""
echo "6. 测试用户角色关联查询..."
mysql -h localhost -P 3306 -u root -p basebackend_admin -e "
SELECT 
    u.username, r.role_name, r.role_key
FROM sys_user u
INNER JOIN sys_user_role ur ON u.id = ur.user_id
INNER JOIN sys_role r ON ur.role_id = r.id
WHERE u.deleted = 0 AND ur.deleted = 0 AND r.deleted = 0
ORDER BY u.username;
" 2>/dev/null

echo ""
echo "测试完成！"

#!/bin/bash
# 修复Maven编译问题的脚本

echo "=== 开始修复Maven编译问题 ==="
echo ""

# 1. 修复根pom.xml - 添加缺失的依赖版本
echo "1. 修复根pom.xml中的依赖版本..."
cat > /tmp/springdoc_dependency.xml << 'EOF'

            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>2.2.0</version>
            </dependency>
EOF

# 在dependencyManagement中添加SpringDoc依赖
awk '
/<\/dependencyManagement>/ {
    print "            <!-- SpringDoc OpenAPI -->"
    print "            <dependency>"
    print "                <groupId>org.springdoc</groupId>"
    print "                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>"
    print "                <version>2.2.0</version>"
    print "            </dependency>"
    print ""
}
{print}
' pom.xml > pom.xml.tmp && mv pom.xml.tmp pom.xml

echo "   ✅ 已添加springdoc-openapi-starter-webmvc-ui版本: 2.2.0"
echo ""

# 2. 修复子模块的parent配置
echo "2. 修复子模块的parent配置..."

# 查找所有有问题的子模块
modules_to_fix=(
    "basebackend-user-service"
    "basebackend-auth-service"
    "basebackend-dict-service"
    "basebackend-dept-service"
    "basebackend-log-service"
    "basebackend-application-service"
    "basebackend-notification-service"
    "basebackend-menu-service"
    "basebackend-monitor-service"
    "basebackend-profile-service"
)

for module in "${modules_to_fix[@]}"; do
    pom_file="${module}/pom.xml"
    if [ -f "$pom_file" ]; then
        # 修复parent配置
        awk '
        /<parent>/, /<\/parent>/ {
            if ($0 ~ /<artifactId>basebackend<) {
                sub(/<artifactId>basebackend<, "<artifactId>basebackend-parent")
            }
            if ($0 ~ /<relativePath>/) {
                sub(/<relativePath>.*<\/relativePath>/, "<relativePath>../pom.xml</relativePath>")
            }
        }
        {print}
        ' "$pom_file" > "${pom_file}.tmp" && mv "${pom_file}.tmp" "$pom_file"

        # 添加springdoc版本（如果缺失）
        if grep -q 'springdoc-openapi-starter-webmvc-ui' "$pom_file"; then
            if ! grep -q 'springdoc-openapi-starter-webmvc-ui.*version' "$pom_file"; then
                awk '
                /<artifactId>springdoc-openapi-starter-webmvc-ui<\/artifactId>/ {
                    getline
                    if ($0 !~ /<version>/) {
                        print $0
                        print "                <version>2.2.0</version>"
                        next
                    }
                }
                {print}
                ' "$pom_file" > "${pom_file}.tmp" && mv "${pom_file}.tmp" "$pom_file"
            fi
        fi

        echo "   ✅ 已修复 $module"
    fi
done

echo ""
echo "3. 清理并重新编译..."
mvn clean install -DskipTests -q

echo ""
echo "=== 修复完成 ==="
echo ""
echo "修复内容:"
echo "1. ✅ 根pom.xml添加了springdoc-openapi-starter-webmvc-ui版本管理"
echo "2. ✅ 所有子模块的parent artifactId修复为 basebackend-parent"
echo "3. ✅ 添加了relativePath配置"
echo "4. ✅ 添加了缺失的依赖版本"
echo ""

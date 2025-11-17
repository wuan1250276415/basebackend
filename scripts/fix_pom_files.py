#!/usr/bin/env python3
"""
修复Maven POM文件的工具脚本
修复子模块的parent配置和依赖版本
"""

import os
import re
import glob
from pathlib import Path

def fix_parent_configuration(pom_path):
    """修复parent配置"""
    print(f"修复: {pom_path}")

    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 修复错误的artifactId
    content = content.replace('basebackend-parent/artifactId>', 'basebackend-parent</artifactId>')

    # 确保有relativePath
    if '<parent>' in content and '<relativePath>' not in content:
        content = re.sub(
            r'(<parent>\s*<groupId>com\.basebackend</groupId>\s*<artifactId>basebackend-parent</artifactId>\s*<version>.*?</version>)',
            r'\1\n        <relativePath>../pom.xml</relativePath>',
            content,
            flags=re.DOTALL
        )

    with open(pom_path, 'w', encoding='utf-8') as f:
        f.write(content)

def add_springdoc_version(pom_path):
    """添加SpringDoc版本"""
    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 检查是否有SpringDoc依赖但没有版本
    if 'springdoc-openapi-starter-webmvc-ui' in content:
        # 确保版本被正确添加
        content = re.sub(
            r'(<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>)(\s*)(?!<version>)',
            r'\1\n                <version>2.2.0</version>',
            content
        )

    with open(pom_path, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    print("=== 开始修复POM文件 ===\n")

    # 1. 修复所有子模块的parent配置
    pom_files = glob.glob('basebackend-*/pom.xml', recursive=False)

    for pom_file in pom_files:
        if os.path.exists(pom_file):
            fix_parent_configuration(pom_file)
            add_springdoc_version(pom_file)

    # 2. 验证修复结果
    print("\n=== 验证修复结果 ===\n")

    # 检查几个关键文件
    test_files = [
        'basebackend-user-service/pom.xml',
        'basebackend-auth-service/pom.xml',
        'basebackend-dict-service/pom.xml',
        'basebackend-dept-service/pom.xml'
    ]

    for test_file in test_files:
        if os.path.exists(test_file):
            with open(test_file, 'r') as f:
                content = f.read()

            if 'basebackend-parent</artifactId>' in content:
                print(f"✅ {test_file} - Parent配置正确")
            else:
                print(f"❌ {test_file} - Parent配置错误")

            if 'relativePath' in content:
                print(f"✅ {test_file} - 包含relativePath")
            else:
                print(f"❌ {test_file} - 缺少relativePath")

    print("\n=== 修复完成 ===\n")

if __name__ == '__main__':
    main()

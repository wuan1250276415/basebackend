#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
微服务集成测试脚本
自动测试所有微服务的启动状态和 API 调用
"""

import requests
import time
import json
import sys
from typing import Dict, List, Tuple

# 颜色定义
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color

# 测试结果
class TestResults:
    def __init__(self):
        self.total = 0
        self.passed = 0
        self.failed = 0
        self.failures = []

    def add_pass(self, name: str, message: str = ""):
        self.total += 1
        self.passed += 1
        print(f"{Colors.GREEN}[PASS]{Colors.NC} {name}")
        if message:
            print(f"  {message}")

    def add_fail(self, name: str, message: str = ""):
        self.total += 1
        self.failed += 1
        self.failures.append((name, message))
        print(f"{Colors.RED}[FAIL]{Colors.NC} {name}")
        if message:
            print(f"  {message}")

    def print_summary(self):
        print("\n" + "=" * 70)
        print("                     测试报告")
        print("=" * 70)
        print(f"总测试数: {self.total}")
        print(f"通过: {self.passed}")
        print(f"失败: {self.failed}")
        print(f"成功率: {(self.passed/self.total*100):.1f}%" if self.total > 0 else "0%")

        if self.failures:
            print(f"\n{Colors.RED}失败的测试:{Colors.NC}")
            for name, msg in self.failures:
                print(f"  - {name}")
                if msg:
                    print(f"    {msg}")

        return self.failed == 0

# HTTP 请求封装
def http_request(method: str, url: str, data: dict = None, headers: dict = None, timeout: int = 5) -> Tuple[int, dict]:
    """发送 HTTP 请求并返回状态码和响应体"""
    try:
        if headers is None:
            headers = {'Content-Type': 'application/json'}

        if method.upper() == 'GET':
            response = requests.get(url, headers=headers, timeout=timeout)
        elif method.upper() == 'POST':
            response = requests.post(url, json=data, headers=headers, timeout=timeout)
        elif method.upper() == 'PUT':
            response = requests.put(url, json=data, headers=headers, timeout=timeout)
        elif method.upper() == 'DELETE':
            response = requests.delete(url, headers=headers, timeout=timeout)
        else:
            raise ValueError(f"不支持的 HTTP 方法: {method}")

        try:
            body = response.json()
        except:
            body = response.text

        return response.status_code, body

    except requests.exceptions.RequestException as e:
        return -1, {"error": str(e)}

# 测试函数
def test_service_health(results: TestResults, name: str, port: int):
    """测试服务健康检查"""
    url = f"http://localhost:{port}/actuator/health"
    status_code, body = http_request('GET', url)

    if status_code == 200:
        results.add_pass(f"{name} 健康检查", f"响应: {body}")
    else:
        results.add_fail(f"{name} 健康检查", f"状态码: {status_code}, 响应: {body}")

def test_api_endpoint(results: TestResults, name: str, method: str, endpoint: str, data: dict = None, expected_code: int = 200):
    """测试 API 端点"""
    status_code, body = http_request(method, endpoint, data)

    if status_code == expected_code:
        results.add_pass(f"{name} API 调用", f"状态码: {status_code}")
    else:
        results.add_fail(f"{name} API 调用", f"期望: {expected_code}, 实际: {status_code}, 响应: {body}")

def test_gateway_route(results: TestResults, name: str, endpoint: str):
    """测试 Gateway 路由"""
    status_code, body = http_request('GET', endpoint)

    if status_code != -1:
        results.add_pass(f"Gateway 路由: {name}", f"状态码: {status_code}")
    else:
        results.add_fail(f"Gateway 路由: {name}", f"连接失败")

def test_database(results: TestResults, db_type: str, **kwargs):
    """测试数据库连接"""
    try:
        if db_type.lower() == 'mysql':
            import pymysql
            connection = pymysql.connect(
                host=kwargs.get('host', 'localhost'),
                port=kwargs.get('port', 3306),
                user=kwargs.get('user', 'root'),
                password=kwargs.get('password', '123456'),
                database=kwargs.get('database', 'mysql')
            )
            connection.close()
            results.add_pass(f"{db_type} 数据库连接")
        elif db_type.lower() == 'redis':
            import redis
            r = redis.Redis(
                host=kwargs.get('host', 'localhost'),
                port=kwargs.get('port', 6379),
                password=kwargs.get('password', ''),
                decode_responses=True
            )
            r.ping()
            results.add_pass(f"{db_type} 数据库连接")
    except Exception as e:
        results.add_fail(f"{db_type} 数据库连接", str(e))

def test_nacos(results: TestResults):
    """测试 Nacos 配置中心"""
    url = "http://localhost:8848/nacos/v1/console/health/readiness"
    status_code, body = http_request('GET', url, timeout=3)

    if status_code == 200:
        results.add_pass("Nacos 配置中心")
    else:
        results.add_fail("Nacos 配置中心", f"状态码: {status_code}")

# 主测试函数
def main():
    print("\n" + "=" * 70)
    print("              微服务集成测试开始")
    print("=" * 70 + "\n")

    results = TestResults()

    # 1. 服务健康检查
    print(f"\n{Colors.BLUE}{'=' * 70}{Colors.NC}")
    print(f"{Colors.BLUE}  1. 服务健康检查{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 70}{Colors.NC}\n")

    services = [
        ("Gateway", 8180),
        ("Nacos", 8848),
        ("User Service", 8081),
        ("Auth Service", 8082),
        ("Menu Service", 8088),
        ("Profile Service", 8090),
        ("Dept Service", 8083),
        ("Dict Service", 8084),
        ("Log Service", 8085),
        ("Monitor Service", 8086),
        ("Application Service", 8087),
        ("Notification Service", 8089),
    ]

    for name, port in services:
        test_service_health(results, name, port)
        time.sleep(0.1)  # 避免请求过快

    # 2. Gateway 路由测试
    print(f"\n{Colors.BLUE}{'=' * 70}{Colors.NC}")
    print(f"{Colors.BLUE}  2. Gateway 路由测试{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 70}{Colors.NC}\n")

    routes = [
        ("用户服务", "http://localhost:8180/api/users/test"),
        ("认证服务", "http://localhost:8180/api/auth/info"),
        ("菜单服务", "http://localhost:8180/api/menus/tree"),
        ("档案服务", "http://localhost:8180/api/profile/preference"),
    ]

    for name, endpoint in routes:
        test_gateway_route(results, name, endpoint)

    # 3. API 接口测试
    print(f"\n{Colors.BLUE}{'=' * 70}{Colors.NC}")
    print(f"{Colors.BLUE}  3. API 接口测试{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 70}{Colors.NC}\n")

    apis = [
        ("用户服务", "GET", "http://localhost:8180/api/users/by-username?username=admin", None, 200),
        ("认证服务", "GET", "http://localhost:8180/api/auth/info", None, 401),  # 需要认证
        ("菜单服务", "GET", "http://localhost:8180/api/menus/tree", None, 200),
        ("档案服务", "GET", "http://localhost:8180/api/profile/preference", None, 401),  # 需要认证
    ]

    for name, method, endpoint, data, expected_code in apis:
        test_api_endpoint(results, name, method, endpoint, data, expected_code)

    # 4. 数据库连接测试
    print(f"\n{Colors.BLUE}{'=' * 70}{Colors.NC}")
    print(f"{Colors.BLUE}  4. 数据库连接测试{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 70}{Colors.NC}\n")

    try:
        test_database(results, 'MySQL', host='localhost', user='root', password='123456', database='mysql')
    except ImportError:
        results.add_fail("MySQL 测试", "未安装 pymysql 库")
    except Exception as e:
        results.add_fail("MySQL 测试", str(e))

    try:
        test_database(results, 'Redis', host='1.117.67.222', password='redis_ycecQi')
    except ImportError:
        results.add_fail("Redis 测试", "未安装 redis 库")
    except Exception as e:
        results.add_fail("Redis 测试", str(e))

    # 5. Nacos 配置测试
    print(f"\n{Colors.BLUE}{'=' * 70}{Colors.NC}")
    print(f"{Colors.BLUE}  5. Nacos 配置测试{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 70}{Colors.NC}\n")

    test_nacos(results)

    # 生成测试报告
    print("\n" + "=" * 70)
    success = results.print_summary()
    print("=" * 70 + "\n")

    if success:
        print(f"{Colors.GREEN}✓ 所有测试通过！{Colors.NC}\n")
        sys.exit(0)
    else:
        print(f"{Colors.RED}✗ 有 {results.failed} 个测试失败{Colors.NC}\n")
        sys.exit(1)

if __name__ == "__main__":
    main()

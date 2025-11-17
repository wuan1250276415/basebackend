// K6负载测试脚本
// 测试BaseBackend各服务的性能和稳定性

import { check, sleep } from 'k6';
import http from 'k6/http';
import { Rate, Trend } from 'k6/metrics';

// 自定义指标
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

// 测试配置
export const options = {
  // 场景1: 负载测试 - 模拟正常流量
  scenarios: {
    // 场景1: 渐进式负载
    ramping_load: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '2m', target: 100 },   // 2分钟内上升到100用户
        { duration: '5m', target: 100 },   // 保持100用户5分钟
        { duration: '2m', target: 200 },   // 2分钟内上升到200用户
        { duration: '5m', target: 200 },   // 保持200用户5分钟
        { duration: '2m', target: 0 },     // 2分钟内降到0用户
      ],
      gracefulStop: '30s',
    },

    // 场景2: 峰值测试
    spike_load: {
      executor: 'spike-vus',
      stages: [
        { duration: '1m', target: 50 },    // 基础负载
        { duration: '30s', target: 500 },  // 突发峰值
        { duration: '1m', target: 500 },   // 保持峰值1分钟
        { duration: '30s', target: 50 },   // 回到基础负载
        { duration: '1m', target: 0 },     // 结束测试
      ],
      gracefulStop: '30s',
    },

    // 场景3: 压力测试
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '3m', target: 100 },   // 逐渐增加
        { duration: '5m', target: 500 },   // 持续压力
        { duration: '5m', target: 1000 },  // 极高压力
        { duration: '3m', target: 1000 },  // 保持极高压力
        { duration: '2m', target: 0 },     // 结束
      ],
      gracefulStop: '30s',
    },

    // 场景4: 稳定性测试
    soak_test: {
      executor: 'constant-vus',
      vus: 100,                           // 固定100用户
      duration: '30m',                    // 持续30分钟
      gracefulStop: '30s',
    },
  },

  // 阈值设置
  thresholds: {
    http_req_duration: ['p(95)<1000'],    // 95%的请求在1秒内完成
    http_req_failed: ['rate<0.05'],       // 错误率小于5%
    errors: ['rate<0.1'],                 // 自定义错误率小于10%
    response_time: ['p(90)<800'],         // 90%的响应时间在800ms内
    checks: ['rate>0.95'],                // 检查通过率大于95%
  },
};

// 测试数据
const BASE_URL = __ENV.BASE_URL || 'https://api-dev.basebackend.com';
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin123';

// 测试场景
export function setup() {
  // 执行登录获取令牌
  const loginResponse = http.post(`${BASE_URL}/auth/login`, {
    username: ADMIN_USERNAME,
    password: ADMIN_PASSWORD,
  });

  const loginResult = JSON.parse(loginResponse.body);

  return {
    authToken: loginResult.token,
    userId: loginResult.userId,
  };
}

export default function (data) {
  // 测试用户管理API
  testUserManagement(data.authToken);

  // 测试认证API
  testAuthAPI();

  // 测试网关API
  testGatewayAPI();

  // 等待1秒
  sleep(1);
}

// 用户管理API测试
function testUserManagement(authToken) {
  const headers = {
    'Authorization': `Bearer ${authToken}`,
    'Content-Type': 'application/json',
  };

  // 获取用户列表
  let response = http.get(`${BASE_URL}/admin/users`, { headers });

  check(response, {
    '用户列表 - 响应状态为200': (r) => r.status === 200,
    '用户列表 - 响应时间正常': (r) => r.timings.duration < 1000,
  }) || errorRate.add(1);

  responseTime.add(response.timings.duration);

  // 获取单个用户
  response = http.get(`${BASE_URL}/admin/users/1`, { headers });

  check(response, {
    '用户详情 - 响应状态为200': (r) => r.status === 200,
    '用户详情 - 响应时间正常': (r) => r.timings.duration < 800,
  }) || errorRate.add(1);

  responseTime.add(response.timings.duration);

  // 搜索用户
  response = http.get(`${BASE_URL}/admin/users/search?keyword=test`, { headers });

  check(response, {
    '搜索用户 - 响应状态为200': (r) => r.status === 200,
    '搜索用户 - 响应时间正常': (r) => r.timings.duration < 1200,
  }) || errorRate.add(1);

  responseTime.add(response.timings.duration);
}

// 认证API测试
function testAuthAPI() {
  // 获取当前用户信息（需要token）
  const tokenResponse = http.post(`${BASE_URL}/auth/login`, {
    username: ADMIN_USERNAME,
    password: ADMIN_PASSWORD,
  });

  check(tokenResponse, {
    '登录 - 响应状态为200': (r) => r.status === 200,
    '登录 - 响应时间正常': (r) => r.timings.duration < 1500,
    '登录 - 包含token': (r) => r.json('token') !== '',
  }) || errorRate.add(1);

  responseTime.add(tokenResponse.timings.duration);

  // 刷新token
  const token = JSON.parse(tokenResponse.body).token;
  const refreshResponse = http.post(`${BASE_URL}/auth/refresh`, {
    token: token,
  });

  check(refreshResponse, {
    '刷新token - 响应状态为200': (r) => r.status === 200,
    '刷新token - 响应时间正常': (r) => r.timings.duration < 800,
  }) || errorRate.add(1);

  responseTime.add(refreshResponse.timings.duration);
}

// 网关API测试
function testGatewayAPI() {
  const headers = {
    'Content-Type': 'application/json',
  };

  // 通过网关调用用户服务
  let response = http.get(`${BASE_URL}/gateway/user/info`, { headers });

  check(response, {
    '网关用户信息 - 响应状态为200': (r) => r.status === 200,
    '网关用户信息 - 响应时间正常': (r) => r.timings.duration < 1000,
  }) || errorRate.add(1);

  responseTime.add(response.timings.duration);

  // 通过网关调用认证服务
  response = http.get(`${BASE_URL}/gateway/auth/health`, { headers });

  check(response, {
    '网关认证健康检查 - 响应状态为200': (r) => r.status === 200,
    '网关认证健康检查 - 响应时间正常': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  responseTime.add(response.timings.duration);
}

// 测试完成后清理
export function teardown(data) {
  console.log('Test completed');
  console.log(`Auth token: ${data.authToken}`);
}

/**
 * 仪表盘首页
 * 展示系统关键指标统计卡片、7日趋势折线图、服务器系统信息面板
 * 数据来源：monitorApi.systemStats + monitorApi.serverInfo
 */
import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, Spin, Progress } from 'antd';
import {
  UserOutlined,
  MessageOutlined,
  ApiOutlined,
  TeamOutlined,
  DesktopOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
} from '@ant-design/icons';
import { Line } from '@ant-design/charts';
import { monitorApi } from '@/api/monitorApi';
import type { ServerInfoDTO } from '@/types';
import './index.css';

/** 系统统计数据结构 */
interface SystemStats {
  todayUserCount: number;
  todayMessageCount: number;
  todayRequestCount: number;
  onlineUserCount: number;
  /** 7日趋势数据 */
  trends: TrendItem[];
}

/** 趋势数据项 */
interface TrendItem {
  date: string;
  type: string;
  count: number;
}

/** 统计卡片配置 */
const statCards = [
  {
    title: '今日用户数',
    key: 'todayUserCount' as const,
    icon: <UserOutlined />,
    className: 'users',
  },
  {
    title: '今日消息数',
    key: 'todayMessageCount' as const,
    icon: <MessageOutlined />,
    className: 'messages',
  },
  {
    title: '今日请求数',
    key: 'todayRequestCount' as const,
    icon: <ApiOutlined />,
    className: 'requests',
  },
  {
    title: '在线用户数',
    key: 'onlineUserCount' as const,
    icon: <TeamOutlined />,
    className: 'online',
  },
];

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [serverInfo, setServerInfo] = useState<ServerInfoDTO | null>(null);

  /** 页面加载时并行获取统计数据和服务器信息 */
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [statsData, serverData] = await Promise.all([
          monitorApi.systemStats(),
          monitorApi.serverInfo(),
        ]);
        setStats(statsData as SystemStats);
        setServerInfo(serverData);
      } catch {
        // 错误已由全局拦截器处理
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  /** 7日趋势折线图配置 */
  const lineConfig = {
    data: stats?.trends ?? [],
    xField: 'date',
    yField: 'count',
    colorField: 'type',
    smooth: true,
    style: {
      lineWidth: 2,
    },
    point: {
      shapeField: 'circle',
      sizeField: 3,
    },
    axis: {
      x: { title: '日期' },
      y: { title: '数量' },
    },
    scale: {
      color: {
        range: ['#1677ff', '#52c41a', '#fa8c16'],
      },
    },
    height: 350,
  };

  /** 解析内存使用率百分比 */
  const parseMemoryUsage = (usage: string | undefined): number => {
    if (!usage) return 0;
    const num = parseFloat(usage.replace('%', ''));
    return isNaN(num) ? 0 : num;
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      {/* 统计卡片区域 */}
      <Row gutter={[16, 16]} className="dashboard-stat-row">
        {statCards.map((card) => (
          <Col xs={24} sm={12} lg={6} key={card.key}>
            <Card className="dashboard-stat-card" bordered={false}>
              <div className="stat-card-content">
                <div className={`stat-card-icon ${card.className}`}>
                  {card.icon}
                </div>
                <Statistic
                  title={card.title}
                  value={stats?.[card.key] ?? 0}
                />
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 7日趋势折线图 */}
      <Card
        className="dashboard-chart-card"
        title="近7日趋势"
        bordered={false}
      >
        <Line {...lineConfig} />
      </Card>

      {/* 系统信息面板 */}
      <Row gutter={[16, 16]}>
        {/* JVM 信息 */}
        <Col xs={24} lg={8}>
          <Card
            className="dashboard-info-card"
            title={
              <span>
                <DesktopOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                JVM 信息
              </span>
            }
            bordered={false}
          >
            <div className="server-info-item">
              <span className="server-info-label">Java 版本</span>
              <span className="server-info-value">
                {serverInfo?.javaVersion ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">JVM 名称</span>
              <span className="server-info-value">
                {serverInfo?.jvmName ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">JVM 版本</span>
              <span className="server-info-value">
                {serverInfo?.jvmVersion ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">运行时间</span>
              <span className="server-info-value">
                {serverInfo?.uptime ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">处理器数量</span>
              <span className="server-info-value">
                {serverInfo?.processorCount ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">系统负载</span>
              <span className="server-info-value">
                {serverInfo?.systemLoad ?? '-'}
              </span>
            </div>
          </Card>
        </Col>

        {/* 内存使用 */}
        <Col xs={24} lg={8}>
          <Card
            className="dashboard-info-card"
            title={
              <span>
                <CloudServerOutlined
                  style={{ marginRight: 8, color: '#52c41a' }}
                />
                内存使用
              </span>
            }
            bordered={false}
          >
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <Progress
                type="dashboard"
                percent={parseMemoryUsage(serverInfo?.memoryUsage)}
                strokeColor={{
                  '0%': '#1677ff',
                  '100%': '#52c41a',
                }}
                size={140}
              />
            </div>
            <div className="server-info-item">
              <span className="server-info-label">总内存</span>
              <span className="server-info-value">
                {serverInfo?.totalMemory ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">已用内存</span>
              <span className="server-info-value">
                {serverInfo?.usedMemory ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">空闲内存</span>
              <span className="server-info-value">
                {serverInfo?.freeMemory ?? '-'}
              </span>
            </div>
          </Card>
        </Col>

        {/* 服务器信息 */}
        <Col xs={24} lg={8}>
          <Card
            className="dashboard-info-card"
            title={
              <span>
                <DatabaseOutlined
                  style={{ marginRight: 8, color: '#fa8c16' }}
                />
                服务器信息
              </span>
            }
            bordered={false}
          >
            <div className="server-info-item">
              <span className="server-info-label">服务器名称</span>
              <span className="server-info-value">
                {serverInfo?.serverName ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">服务器 IP</span>
              <span className="server-info-value">
                {serverInfo?.serverIp ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">操作系统</span>
              <span className="server-info-value">
                {serverInfo?.osName ?? '-'} {serverInfo?.osArch ?? ''}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">系统版本</span>
              <span className="server-info-value">
                {serverInfo?.osVersion ?? '-'}
              </span>
            </div>
            <div className="server-info-item">
              <span className="server-info-label">Java 厂商</span>
              <span className="server-info-value">
                {serverInfo?.javaVendor ?? '-'}
              </span>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;

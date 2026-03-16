/**
 * 聊天统计页面
 * 展示聊天系统关键指标：今日消息数、活跃用户数、群组数
 * 以及近7日消息趋势折线图
 * 数据来源：chatApi.messageList + chatApi.groupList
 */
import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, Spin } from 'antd';
import {
  MessageOutlined,
  UserOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Line } from '@ant-design/charts';
import { chatApi } from '@/api/chatApi';
import './index.css';

/** 聊天统计数据结构 */
interface ChatStats {
  /** 今日消息数 */
  todayMessageCount: number;
  /** 活跃用户数（今日有发送消息的不同用户） */
  activeUserCount: number;
  /** 群组总数 */
  groupCount: number;
}

/** 7日趋势数据项 */
interface TrendItem {
  date: string;
  count: number;
}

/** 统计卡片配置 */
const statCards = [
  {
    title: '今日消息数',
    key: 'todayMessageCount' as const,
    icon: <MessageOutlined />,
    className: 'messages',
  },
  {
    title: '活跃用户数',
    key: 'activeUserCount' as const,
    icon: <UserOutlined />,
    className: 'active-users',
  },
  {
    title: '群组总数',
    key: 'groupCount' as const,
    icon: <TeamOutlined />,
    className: 'groups',
  },
];

/**
 * 获取最近N天的日期字符串数组（格式：MM-DD）
 */
const getRecentDates = (days: number): string[] => {
  const dates: string[] = [];
  const now = new Date();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    dates.push(`${month}-${day}`);
  }
  return dates;
};

/**
 * 获取某天的起止时间范围（ISO 格式）
 */
const getDayRange = (daysAgo: number): { beginTime: string; endTime: string } => {
  const now = new Date();
  const target = new Date(now);
  target.setDate(target.getDate() - daysAgo);
  target.setHours(0, 0, 0, 0);
  const end = new Date(target);
  end.setHours(23, 59, 59, 999);
  return {
    beginTime: target.toISOString(),
    endTime: end.toISOString(),
  };
};

const ChatStatsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<ChatStats>({
    todayMessageCount: 0,
    activeUserCount: 0,
    groupCount: 0,
  });
  const [trends, setTrends] = useState<TrendItem[]>([]);

  /** 页面加载时获取统计数据 */
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // 并行获取今日消息和群组数据
        const todayRange = getDayRange(0);
        const [messageResult, groupResult] = await Promise.all([
          chatApi.messageList({
            current: 1,
            size: 1,
            beginTime: todayRange.beginTime,
            endTime: todayRange.endTime,
          }),
          chatApi.groupList({ current: 1, size: 1 }),
        ]);

        // 获取今日消息的发送者数量（活跃用户估算）
        // 通过获取较多消息来统计不同发送者
        let activeUserCount = 0;
        if (messageResult.total > 0) {
          const detailResult = await chatApi.messageList({
            current: 1,
            size: 100,
            beginTime: todayRange.beginTime,
            endTime: todayRange.endTime,
          });
          const senderIds = new Set(detailResult.records.map((m) => m.senderId));
          activeUserCount = senderIds.size;
        }

        setStats({
          todayMessageCount: messageResult.total,
          activeUserCount,
          groupCount: groupResult.total,
        });

        // 获取近7日趋势数据
        const dates = getRecentDates(7);
        const trendPromises = Array.from({ length: 7 }, (_, i) => {
          const range = getDayRange(6 - i);
          return chatApi.messageList({
            current: 1,
            size: 1,
            beginTime: range.beginTime,
            endTime: range.endTime,
          });
        });
        const trendResults = await Promise.all(trendPromises);
        const trendData = dates.map((date, i) => ({
          date,
          count: trendResults[i].total,
        }));
        setTrends(trendData);
      } catch {
        // 错误已由全局拦截器处理
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  /** 7日消息趋势折线图配置 */
  const lineConfig = {
    data: trends,
    xField: 'date',
    yField: 'count',
    smooth: true,
    style: {
      lineWidth: 2,
      stroke: '#1677ff',
    },
    point: {
      shapeField: 'circle',
      sizeField: 3,
    },
    axis: {
      x: { title: '日期' },
      y: { title: '消息数' },
    },
    scale: {
      color: {
        range: ['#1677ff'],
      },
    },
    height: 350,
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div className="chat-stats-container">
      {/* 统计卡片区域 */}
      <Row gutter={[16, 16]} className="chat-stats-row">
        {statCards.map((card) => (
          <Col xs={24} sm={12} lg={8} key={card.key}>
            <Card className="chat-stat-card" bordered={false}>
              <div className="chat-stat-content">
                <div className={`chat-stat-icon ${card.className}`}>
                  {card.icon}
                </div>
                <Statistic
                  title={card.title}
                  value={stats[card.key]}
                />
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 近7日消息趋势折线图 */}
      <Card
        className="chat-chart-card"
        title="近7日消息趋势"
        bordered={false}
      >
        <Line {...lineConfig} />
      </Card>
    </div>
  );
};

export default ChatStatsPage;

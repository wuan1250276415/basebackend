import React from 'react'
import { Card, Space, Button, Typography } from 'antd'
import { ReloadOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/stores/auth'

const { Title, Text } = Typography

interface WelcomeHeaderProps {
  onRefresh: () => void
  loading?: boolean
}

export const WelcomeHeader: React.FC<WelcomeHeaderProps> = React.memo(
  ({ onRefresh, loading = false }) => {
    const { userInfo } = useAuthStore()

    // 获取问候语
    const getGreeting = () => {
      const hour = new Date().getHours()
      if (hour < 6) return '凌晨好'
      if (hour < 9) return '早上好'
      if (hour < 12) return '上午好'
      if (hour < 14) return '中午好'
      if (hour < 17) return '下午好'
      if (hour < 19) return '傍晚好'
      if (hour < 22) return '晚上好'
      return '夜深了'
    }

    //获取当前时间
    const [currentTime, setCurrentTime] = React.useState(new Date())
    React.useEffect(() => {
      const timer = setInterval(() => setCurrentTime(new Date()), 1000)
      return () => clearInterval(timer)
    }, [])

    const formatTime = (date: Date) => {
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    }

    return (
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space direction="vertical" size={4}>
            <Title level={3} style={{ margin: 0 }}>
              {getGreeting()}，{userInfo?.nickname || userInfo?.username}！
            </Title>
            <Space>
              <ClockCircleOutlined />
              <Text type="secondary">{formatTime(currentTime)}</Text>
            </Space>
          </Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={onRefresh}
            loading={loading}
          >
            刷新数据
          </Button>
        </div>
      </Card>
    )
  }
)

WelcomeHeader.displayName = 'WelcomeHeader'

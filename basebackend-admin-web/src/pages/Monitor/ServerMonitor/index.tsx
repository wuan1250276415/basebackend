import { useState, useEffect } from 'react'
import { Card, Row, Col, Descriptions, Progress, Statistic, Button, message, Spin } from 'antd'
import { ReloadOutlined, DatabaseOutlined, ClockCircleOutlined, ApiOutlined, DesktopOutlined } from '@ant-design/icons'
import { ServerInfo } from '@/types'
import { getServerInfo } from '@/api/monitor'

const ServerMonitorPage = () => {
  const [loading, setLoading] = useState(false)
  const [serverInfo, setServerInfo] = useState<ServerInfo | null>(null)

  useEffect(() => {
    loadData()
    // 每10秒自动刷新
    const timer = setInterval(() => {
      loadData()
    }, 10000)
    return () => clearInterval(timer)
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const response = await getServerInfo()
      setServerInfo(response.data as ServerInfo)
    } catch (error: any) {
      message.error(error.message || '加载服务器信息失败')
    } finally {
      setLoading(false)
    }
  }

  if (!serverInfo) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: 50 }}>
          <Spin size="large" />
        </div>
      </Card>
    )
  }

  // 计算内存使用百分比
  const memoryPercent = serverInfo.memoryUsage ? parseFloat(serverInfo.memoryUsage.replace('%', '')) : 0

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="CPU核心数"
              value={serverInfo.processorCount}
              prefix={<DesktopOutlined />}
              suffix="核"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="JVM内存使用率"
              value={memoryPercent}
              precision={2}
              suffix="%"
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: memoryPercent > 80 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="系统负载"
              value={serverInfo.systemLoad}
              prefix={<ApiOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="运行时间"
              value={serverInfo.uptime}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ fontSize: 16 }}
            />
          </Card>
        </Col>
      </Row>

      {/* 服务器信息 */}
      <Card
        title="服务器信息"
        extra={
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="服务器名称">{serverInfo.serverName}</Descriptions.Item>
          <Descriptions.Item label="服务器IP">{serverInfo.serverIp}</Descriptions.Item>
          <Descriptions.Item label="操作系统">{serverInfo.osName}</Descriptions.Item>
          <Descriptions.Item label="系统版本">{serverInfo.osVersion}</Descriptions.Item>
          <Descriptions.Item label="系统架构" span={2}>{serverInfo.osArch}</Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Java信息 */}
      <Card title="Java环境" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Java版本">{serverInfo.javaVersion}</Descriptions.Item>
          <Descriptions.Item label="Java供应商">{serverInfo.javaVendor}</Descriptions.Item>
          <Descriptions.Item label="JVM名称">{serverInfo.jvmName}</Descriptions.Item>
          <Descriptions.Item label="JVM版本">{serverInfo.jvmVersion}</Descriptions.Item>
          <Descriptions.Item label="JVM供应商" span={2}>{serverInfo.jvmVendor}</Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 内存信息 */}
      <Card title="内存使用情况">
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <Card>
              <Statistic
                title="总内存"
                value={serverInfo.totalMemory}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="已用内存"
                value={serverInfo.usedMemory}
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="空闲内存"
                value={serverInfo.freeMemory}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
        </Row>

        <div style={{ padding: '0 20px' }}>
          <div style={{ marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>内存使用率：</span>
            <span style={{ marginLeft: 10, fontSize: 16, fontWeight: 'bold' }}>
              {serverInfo.memoryUsage}
            </span>
          </div>
          <Progress
            percent={memoryPercent}
            status={memoryPercent > 80 ? 'exception' : 'success'}
            strokeColor={memoryPercent > 80 ? '#cf1322' : '#3f8600'}
          />
        </div>
      </Card>
    </div>
  )
}

export default ServerMonitorPage

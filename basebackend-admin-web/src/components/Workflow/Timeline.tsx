import React from 'react'
import { Timeline, Tag } from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

/**
 * 任务历史记录项
 */
export interface HistoryItem {
  id?: string
  name: string
  assignee?: string
  startTime: string
  endTime?: string
  deleteReason?: string
  comment?: string
}

/**
 * 任务历史时间轴组件
 */
interface TaskHistoryTimelineProps {
  history: HistoryItem[]
  loading?: boolean
  style?: React.CSSProperties
}

export const TaskHistoryTimeline: React.FC<TaskHistoryTimelineProps> = ({
  history,
  loading = false,
  style,
}) => {
  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0', ...style }}>
        <SyncOutlined spin style={{ fontSize: 24 }} />
        <div style={{ marginTop: 8 }}>加载中...</div>
      </div>
    )
  }

  if (history.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0', color: '#999', ...style }}>
        暂无任务历史
      </div>
    )
  }

  return (
    <Timeline style={style}>
      {history.map((item, index) => {
        const isCompleted = !!item.endTime
        const isCurrent = !item.endTime && index === history.length - 1
        const isTerminated = !!item.deleteReason

        let color = 'gray'
        let icon = <ClockCircleOutlined style={{ fontSize: 16 }} />

        if (isTerminated) {
          color = 'red'
          icon = <CloseCircleOutlined style={{ fontSize: 16 }} />
        } else if (isCompleted) {
          color = 'green'
          icon = <CheckCircleOutlined style={{ fontSize: 16 }} />
        } else if (isCurrent) {
          color = 'blue'
          icon = <SyncOutlined spin style={{ fontSize: 16 }} />
        }

        return (
          <Timeline.Item key={item.id || index} color={color} dot={icon}>
            <div>
              {/* 任务名称 */}
              <div style={{ fontWeight: 'bold', marginBottom: 4, fontSize: 14 }}>
                {item.name}
              </div>

              {/* 办理人 */}
              <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                办理人：{item.assignee || <Tag size="small">待认领</Tag>}
              </div>

              {/* 开始时间 */}
              <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                开始时间：{dayjs(item.startTime).format('YYYY-MM-DD HH:mm:ss')}
              </div>

              {/* 完成时间 */}
              {item.endTime && (
                <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                  完成时间：{dayjs(item.endTime).format('YYYY-MM-DD HH:mm:ss')}
                </div>
              )}

              {/* 耗时 */}
              {item.endTime && (
                <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                  耗时：
                  {(() => {
                    const duration = dayjs(item.endTime).diff(dayjs(item.startTime), 'minute')
                    if (duration < 60) {
                      return `${duration}分钟`
                    } else if (duration < 1440) {
                      const hours = Math.floor(duration / 60)
                      const minutes = duration % 60
                      return `${hours}小时${minutes}分钟`
                    } else {
                      const days = Math.floor(duration / 1440)
                      const hours = Math.floor((duration % 1440) / 60)
                      return `${days}天${hours}小时`
                    }
                  })()}
                </div>
              )}

              {/* 审批意见 */}
              {item.comment && (
                <div
                  style={{
                    marginTop: 8,
                    padding: 8,
                    backgroundColor: '#f5f5f5',
                    borderRadius: 4,
                    fontSize: 12,
                  }}
                >
                  <div style={{ color: '#999', marginBottom: 4 }}>审批意见：</div>
                  <div style={{ color: '#333' }}>{item.comment}</div>
                </div>
              )}

              {/* 状态标签 */}
              {!item.endTime && isCurrent && (
                <Tag color="processing" style={{ marginTop: 4 }}>
                  进行中
                </Tag>
              )}

              {/* 终止原因 */}
              {item.deleteReason && (
                <div style={{ color: '#f5222d', fontSize: 12, marginTop: 4 }}>
                  终止原因：{item.deleteReason}
                </div>
              )}
            </div>
          </Timeline.Item>
        )
      })}
    </Timeline>
  )
}

/**
 * 简化版任务历史时间轴（只显示关键信息）
 */
interface SimpleTimelineProps {
  history: HistoryItem[]
  style?: React.CSSProperties
}

export const SimpleTimeline: React.FC<SimpleTimelineProps> = ({ history, style }) => {
  if (history.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '20px 0', color: '#999', ...style }}>
        暂无历史记录
      </div>
    )
  }

  return (
    <Timeline style={style}>
      {history.map((item, index) => (
        <Timeline.Item
          key={item.id || index}
          color={item.endTime ? 'green' : 'blue'}
          dot={
            item.endTime ? (
              <CheckCircleOutlined style={{ fontSize: 14 }} />
            ) : (
              <ClockCircleOutlined style={{ fontSize: 14 }} />
            )
          }
        >
          <div style={{ fontSize: 12 }}>
            <div style={{ fontWeight: 'bold', marginBottom: 2 }}>{item.name}</div>
            <div style={{ color: '#999' }}>
              {item.assignee || '待认领'} · {dayjs(item.startTime).format('MM-DD HH:mm')}
            </div>
          </div>
        </Timeline.Item>
      ))}
    </Timeline>
  )
}

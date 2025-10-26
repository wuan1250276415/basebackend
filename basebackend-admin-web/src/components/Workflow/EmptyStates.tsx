import React from 'react'
import { Empty, Button } from 'antd'
import {
  InboxOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'

/**
 * 空状态组件 - 无待办任务
 */
export const EmptyTodoTasks: React.FC<{ onRefresh?: () => void }> = ({ onRefresh }) => {
  return (
    <Empty
      image={<CheckCircleOutlined style={{ fontSize: 64, color: '#52c41a' }} />}
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>太棒了！没有待办任务</div>
          <div style={{ color: '#999' }}>所有任务都已处理完毕</div>
        </div>
      }
    >
      {onRefresh && <Button onClick={onRefresh}>刷新</Button>}
    </Empty>
  )
}

/**
 * 空状态组件 - 无流程实例
 */
export const EmptyProcessInstances: React.FC<{ onCreate?: () => void }> = ({ onCreate }) => {
  return (
    <Empty
      image={<InboxOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>暂无流程实例</div>
          <div style={{ color: '#999' }}>还没有发起任何流程</div>
        </div>
      }
    >
      {onCreate && (
        <Button type="primary" onClick={onCreate}>
          发起新流程
        </Button>
      )}
    </Empty>
  )
}

/**
 * 空状态组件 - 无流程定义
 */
export const EmptyProcessDefinitions: React.FC<{ onDeploy?: () => void }> = ({
  onDeploy,
}) => {
  return (
    <Empty
      image={<FileTextOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>暂无流程定义</div>
          <div style={{ color: '#999' }}>请先部署流程定义</div>
        </div>
      }
    >
      {onDeploy && (
        <Button type="primary" onClick={onDeploy}>
          部署流程
        </Button>
      )}
    </Empty>
  )
}

/**
 * 空状态组件 - 无搜索结果
 */
export const EmptySearchResult: React.FC<{ onClear?: () => void }> = ({ onClear }) => {
  return (
    <Empty
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>未找到匹配结果</div>
          <div style={{ color: '#999' }}>请尝试其他搜索条件</div>
        </div>
      }
    >
      {onClear && <Button onClick={onClear}>清除筛选</Button>}
    </Empty>
  )
}

/**
 * 空状态组件 - 无历史记录
 */
export const EmptyHistory: React.FC = () => {
  return (
    <Empty
      image={<ClockCircleOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>暂无历史记录</div>
          <div style={{ color: '#999' }}>还没有任何操作记录</div>
        </div>
      }
    />
  )
}

/**
 * 通用空状态组件
 */
interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: {
    text: string
    onClick: () => void
    type?: 'default' | 'primary'
  }
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
}) => {
  return (
    <Empty
      image={icon || <InboxOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
      description={
        <div>
          <div style={{ fontSize: 16, marginBottom: 8 }}>{title}</div>
          {description && <div style={{ color: '#999' }}>{description}</div>}
        </div>
      }
    >
      {action && (
        <Button type={action.type || 'default'} onClick={action.onClick}>
          {action.text}
        </Button>
      )}
    </Empty>
  )
}

import React from 'react'
import { Tag } from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  PauseCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'

/**
 * 流程状态标签组件
 */
interface ProcessStatusTagProps {
  ended?: boolean
  suspended?: boolean
  deleteReason?: string
  style?: React.CSSProperties
}

export const ProcessStatusTag: React.FC<ProcessStatusTagProps> = ({
  ended,
  suspended,
  deleteReason,
  style,
}) => {
  if (deleteReason) {
    return (
      <Tag icon={<CloseCircleOutlined />} color="error" style={style}>
        已终止
      </Tag>
    )
  }

  if (ended) {
    return (
      <Tag icon={<CheckCircleOutlined />} color="success" style={style}>
        已完成
      </Tag>
    )
  }

  if (suspended) {
    return (
      <Tag icon={<PauseCircleOutlined />} color="warning" style={style}>
        已挂起
      </Tag>
    )
  }

  return (
    <Tag icon={<SyncOutlined spin />} color="processing" style={style}>
      进行中
    </Tag>
  )
}

/**
 * 任务状态标签组件
 */
interface TaskStatusTagProps {
  dueDate?: string
  endTime?: string
  style?: React.CSSProperties
}

export const TaskStatusTag: React.FC<TaskStatusTagProps> = ({
  dueDate,
  endTime,
  style,
}) => {
  if (endTime) {
    return (
      <Tag icon={<CheckCircleOutlined />} color="success" style={style}>
        已完成
      </Tag>
    )
  }

  if (dueDate) {
    const now = new Date()
    const due = new Date(dueDate)
    const hoursDiff = (due.getTime() - now.getTime()) / (1000 * 60 * 60)

    if (hoursDiff < 0) {
      return (
        <Tag icon={<CloseCircleOutlined />} color="error" style={style}>
          已超时
        </Tag>
      )
    } else if (hoursDiff < 24) {
      return (
        <Tag icon={<ClockCircleOutlined />} color="warning" style={style}>
          即将超时
        </Tag>
      )
    }
  }

  return (
    <Tag icon={<ClockCircleOutlined />} color="processing" style={style}>
      正常
    </Tag>
  )
}

/**
 * 优先级标签组件
 */
interface PriorityTagProps {
  priority: number
  style?: React.CSSProperties
}

export const PriorityTag: React.FC<PriorityTagProps> = ({ priority, style }) => {
  if (priority >= 80) {
    return (
      <Tag color="red" style={style}>
        紧急
      </Tag>
    )
  } else if (priority >= 50) {
    return (
      <Tag color="orange" style={style}>
        重要
      </Tag>
    )
  } else {
    return (
      <Tag color="blue" style={style}>
        普通
      </Tag>
    )
  }
}

/**
 * 流程类型标签组件
 */
interface ProcessTypeTagProps {
  processName: string
  style?: React.CSSProperties
}

export const ProcessTypeTag: React.FC<ProcessTypeTagProps> = ({
  processName,
  style,
}) => {
  const name = processName.toLowerCase()

  if (name.includes('leave') || name.includes('请假')) {
    return (
      <Tag color="blue" style={style}>
        请假审批
      </Tag>
    )
  } else if (name.includes('expense') || name.includes('报销')) {
    return (
      <Tag color="green" style={style}>
        报销审批
      </Tag>
    )
  } else if (name.includes('purchase') || name.includes('采购')) {
    return (
      <Tag color="orange" style={style}>
        采购审批
      </Tag>
    )
  } else {
    return (
      <Tag color="default" style={style}>
        其他流程
      </Tag>
    )
  }
}

/**
 * 流程定义状态标签
 */
interface DefinitionStatusTagProps {
  suspended: boolean
  style?: React.CSSProperties
}

export const DefinitionStatusTag: React.FC<DefinitionStatusTagProps> = ({
  suspended,
  style,
}) => {
  return suspended ? (
    <Tag color="warning" icon={<PauseCircleOutlined />} style={style}>
      已挂起
    </Tag>
  ) : (
    <Tag color="success" icon={<CheckCircleOutlined />} style={style}>
      激活
    </Tag>
  )
}

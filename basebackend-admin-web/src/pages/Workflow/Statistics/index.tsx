import React, { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, message, Spin } from 'antd'
import {
    AppstoreOutlined,
    PlayCircleOutlined,
    CheckCircleOutlined,
    WarningOutlined,
} from '@ant-design/icons'
import { Pie, Column } from '@ant-design/charts'
import { getWorkflowOverview } from '@/api/workflow/statistics'
import type { WorkflowOverview } from '@/types/workflow'

const Statistics: React.FC = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState<WorkflowOverview | null>(null)

    const loadData = async () => {
        setLoading(true)
        try {
            const response = await getWorkflowOverview()
            if (response.code === 200) {
                setData(response.data)
            } else {
                message.error(response.message || '加载统计数据失败')
            }
        } catch (error) {
            message.error('加载统计数据失败')
            console.error(error)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadData()
    }, [])

    if (loading || !data) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
            </div>
        )
    }

    // Instance Status Data for Pie Chart
    const instanceStatusData = [
        { type: '进行中', value: data.instances.runningInstances },
        { type: '已完成', value: data.instances.completedInstances },
        { type: '已终止', value: data.instances.terminatedInstances },
    ]

    const instanceConfig = {
        appendPadding: 10,
        data: instanceStatusData,
        angleField: 'value',
        colorField: 'type',
        radius: 0.8,
        label: {
            type: 'outer',
        },
        interactions: [{ type: 'element-active' }],
    }

    // Task Status Data for Pie Chart
    const taskStatusData = [
        { type: '待办', value: data.tasks.pendingTasks },
        { type: '已完成', value: data.tasks.completedTasks },
        { type: '逾期', value: data.tasks.overdueTasks },
    ]

    const taskConfig = {
        appendPadding: 10,
        data: taskStatusData,
        angleField: 'value',
        colorField: 'type',
        radius: 0.8,
        label: {
            type: 'outer',
        },
        interactions: [{ type: 'element-active' }],
    }

    return (
        <div>
            <Row gutter={[16, 16]}>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="流程定义总数"
                            value={data.processDefinitions.totalDefinitions}
                            prefix={<AppstoreOutlined />}
                        />
                        <div style={{ marginTop: 8, fontSize: 12, color: '#666' }}>
                            活跃: {data.processDefinitions.activeDefinitions} | 挂起: {data.processDefinitions.suspendedDefinitions}
                        </div>
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="运行中实例"
                            value={data.instances.runningInstances}
                            prefix={<PlayCircleOutlined />}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="待办任务"
                            value={data.tasks.pendingTasks}
                            prefix={<WarningOutlined />}
                            valueStyle={{ color: '#faad14' }}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="已完成任务"
                            value={data.tasks.completedTasks}
                            prefix={<CheckCircleOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
            </Row>

            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                    <Card title="流程实例状态分布">
                        <Pie {...instanceConfig} style={{ height: 300 }} />
                    </Card>
                </Col>
                <Col span={12}>
                    <Card title="任务状态分布">
                        <Pie {...taskConfig} style={{ height: 300 }} />
                    </Card>
                </Col>
            </Row>
        </div>
    )
}

export default Statistics

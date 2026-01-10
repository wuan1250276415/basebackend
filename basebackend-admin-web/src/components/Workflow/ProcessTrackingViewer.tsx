import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
    Card,
    Spin,
    message,
    Alert,
    Table,
    Tag,
    Tooltip,
    Descriptions,
    Row,
    Col,
    Timeline,
    Button,
    Space,
} from 'antd'
import {
    CheckCircleOutlined,
    SyncOutlined,
    CloseCircleOutlined,
    ReloadOutlined,
    FullscreenOutlined,
    FullscreenExitOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import {
    getProcessTracking,
    type ProcessTracking,
    type ActivityHistory,
} from '@/api/workflow/tracking'

// bpmn-js NavigatedViewer
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

// 高亮样式颜色
const COLORS = {
    COMPLETED: '#52c41a', // 绿色 - 已完成
    ACTIVE: '#1890ff', // 蓝色 - 进行中
    FAILED: '#ff4d4f', // 红色 - 失败
    PENDING: '#d9d9d9', // 灰色 - 待执行
}

interface ProcessTrackingViewerProps {
    processInstanceId: string
}

const ProcessTrackingViewer: React.FC<ProcessTrackingViewerProps> = ({
    processInstanceId,
}) => {
    const [loading, setLoading] = useState(false)
    const [tracking, setTracking] = useState<ProcessTracking | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [isFullscreen, setIsFullscreen] = useState(false)

    const containerRef = useRef<HTMLDivElement>(null)
    const viewerRef = useRef<any>(null)

    // 加载跟踪数据
    const loadTracking = useCallback(async () => {
        if (!processInstanceId) return

        setLoading(true)
        setError(null)
        try {
            const response = await getProcessTracking(processInstanceId)
            if (response.code === 200) {
                setTracking(response.data)
            } else {
                setError(response.message || '加载流程跟踪信息失败')
            }
        } catch (err) {
            setError('加载流程跟踪信息失败')
            console.error(err)
        } finally {
            setLoading(false)
        }
    }, [processInstanceId])

    useEffect(() => {
        loadTracking()
    }, [loadTracking])

    // 渲染 BPMN 图并高亮节点
    useEffect(() => {
        if (!tracking?.bpmnXml || !containerRef.current) return

        const initViewer = async () => {
            // 清理旧的 viewer
            if (viewerRef.current) {
                viewerRef.current.destroy()
            }

            const viewer = new NavigatedViewer({
                container: containerRef.current!,
            })

            viewerRef.current = viewer

            try {
                await viewer.importXML(tracking.bpmnXml)

                // 获取 canvas 和 overlays
                const canvas = viewer.get('canvas') as any
                const overlays = viewer.get('overlays') as any

                // 缩放到适合
                canvas.zoom('fit-viewport')

                // 高亮已完成节点（绿色）
                tracking.completedActivityIds?.forEach((activityId) => {
                    try {
                        canvas.addMarker(activityId, 'highlight-completed')
                        addOverlay(overlays, activityId, 'completed')
                    } catch (e) {
                        // 忽略找不到元素的错误
                    }
                })

                // 高亮进行中节点（蓝色）
                tracking.activeActivityIds?.forEach((activityId) => {
                    try {
                        canvas.addMarker(activityId, 'highlight-active')
                        addOverlay(overlays, activityId, 'active')
                    } catch (e) {
                        // 忽略找不到元素的错误
                    }
                })

                // 高亮失败节点（红色）
                tracking.failedActivityIds?.forEach((activityId) => {
                    try {
                        canvas.addMarker(activityId, 'highlight-failed')
                        addOverlay(overlays, activityId, 'failed')
                    } catch (e) {
                        // 忽略找不到元素的错误
                    }
                })
            } catch (err) {
                console.error('Failed to import BPMN', err)
                message.error('加载流程图失败')
            }
        }

        initViewer()

        return () => {
            if (viewerRef.current) {
                viewerRef.current.destroy()
                viewerRef.current = null
            }
        }
    }, [tracking])

    // 添加覆盖图标
    const addOverlay = (overlays: any, activityId: string, type: string) => {
        let html = ''
        switch (type) {
            case 'completed':
                html = `<div style="background: ${COLORS.COMPLETED}; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; color: white; font-size: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.3);">✓</div>`
                break
            case 'active':
                html = `<div style="background: ${COLORS.ACTIVE}; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; color: white; font-size: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.3); animation: pulse 1.5s infinite;">●</div>`
                break
            case 'failed':
                html = `<div style="background: ${COLORS.FAILED}; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; color: white; font-size: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.3);">✕</div>`
                break
        }

        overlays.add(activityId, 'status-indicator', {
            position: { top: -10, right: -10 },
            html: html,
        })
    }

    // 获取活动类型标签
    const getActivityTypeTag = (type: string) => {
        const typeMap: Record<string, { color: string; label: string }> = {
            startEvent: { color: 'green', label: '开始' },
            endEvent: { color: 'red', label: '结束' },
            userTask: { color: 'blue', label: '用户任务' },
            serviceTask: { color: 'purple', label: '服务任务' },
            exclusiveGateway: { color: 'orange', label: '排他网关' },
            parallelGateway: { color: 'orange', label: '并行网关' },
            inclusiveGateway: { color: 'orange', label: '包容网关' },
            callActivity: { color: 'cyan', label: '调用活动' },
            subProcess: { color: 'geekblue', label: '子流程' },
        }
        const info = typeMap[type] || { color: 'default', label: type }
        return <Tag color={info.color}>{info.label}</Tag>
    }

    // 获取活动状态标签
    const getActivityStatusTag = (activity: ActivityHistory) => {
        if (activity.canceled) {
            return <Tag icon={<CloseCircleOutlined />} color="warning">已取消</Tag>
        }
        if (activity.ended) {
            return <Tag icon={<CheckCircleOutlined />} color="success">已完成</Tag>
        }
        return <Tag icon={<SyncOutlined spin />} color="processing">进行中</Tag>
    }

    // 活动历史表格列
    const activityColumns: ColumnsType<ActivityHistory> = [
        {
            title: '活动名称',
            dataIndex: 'activityName',
            key: 'activityName',
            ellipsis: true,
            render: (text, record) => (
                <Tooltip title={record.activityId}>
                    <span>{text || record.activityId}</span>
                </Tooltip>
            ),
        },
        {
            title: '类型',
            dataIndex: 'activityType',
            key: 'activityType',
            width: 100,
            render: (text) => getActivityTypeTag(text),
        },
        {
            title: '状态',
            key: 'status',
            width: 100,
            render: (_, record) => getActivityStatusTag(record),
        },
        {
            title: '执行人',
            dataIndex: 'assignee',
            key: 'assignee',
            width: 100,
            render: (text) => text || '-',
        },
        {
            title: '开始时间',
            dataIndex: 'startTime',
            key: 'startTime',
            width: 160,
            render: (text) =>
                text ? (
                    <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
                        {dayjs(text).fromNow()}
                    </Tooltip>
                ) : (
                    '-'
                ),
        },
        {
            title: '耗时',
            dataIndex: 'durationInMillis',
            key: 'durationInMillis',
            width: 100,
            render: (ms) => {
                if (!ms) return '-'
                if (ms < 1000) return `${ms}ms`
                if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
                return `${(ms / 60000).toFixed(1)}min`
            },
        },
    ]

    // 切换全屏
    const toggleFullscreen = () => {
        setIsFullscreen(!isFullscreen)
    }

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: 50 }}>
                <Spin size="large" tip="加载中..." />
            </div>
        )
    }

    if (error) {
        return <Alert type="error" message={error} showIcon />
    }

    if (!tracking) {
        return <Alert type="info" message="暂无跟踪数据" showIcon />
    }

    return (
        <div>
            {/* 流程状态概览 */}
            <Card size="small" style={{ marginBottom: 16 }}>
                <Descriptions column={4} size="small">
                    <Descriptions.Item label="流程实例">
                        {tracking.processInstanceId}
                    </Descriptions.Item>
                    <Descriptions.Item label="流程定义">
                        {tracking.processDefinitionKey}
                    </Descriptions.Item>
                    <Descriptions.Item label="业务键">
                        {tracking.businessKey || '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态">
                        {tracking.ended ? (
                            <Tag color="default">已结束</Tag>
                        ) : tracking.suspended ? (
                            <Tag color="warning">已挂起</Tag>
                        ) : (
                            <Tag color="processing">运行中</Tag>
                        )}
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            {/* 图例 */}
            <Card size="small" style={{ marginBottom: 16 }}>
                <Space size="large">
                    <span>
                        <span
                            style={{
                                display: 'inline-block',
                                width: 12,
                                height: 12,
                                borderRadius: '50%',
                                background: COLORS.COMPLETED,
                                marginRight: 6,
                            }}
                        />
                        已完成 ({tracking.completedActivityIds?.length || 0})
                    </span>
                    <span>
                        <span
                            style={{
                                display: 'inline-block',
                                width: 12,
                                height: 12,
                                borderRadius: '50%',
                                background: COLORS.ACTIVE,
                                marginRight: 6,
                            }}
                        />
                        进行中 ({tracking.activeActivityIds?.length || 0})
                    </span>
                    <span>
                        <span
                            style={{
                                display: 'inline-block',
                                width: 12,
                                height: 12,
                                borderRadius: '50%',
                                background: COLORS.FAILED,
                                marginRight: 6,
                            }}
                        />
                        失败 ({tracking.failedActivityIds?.length || 0})
                    </span>
                    <span style={{ marginLeft: 'auto' }}>
                        <Button
                            icon={<ReloadOutlined />}
                            size="small"
                            onClick={loadTracking}
                            style={{ marginRight: 8 }}
                        >
                            刷新
                        </Button>
                        <Button
                            icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
                            size="small"
                            onClick={toggleFullscreen}
                        >
                            {isFullscreen ? '退出全屏' : '全屏'}
                        </Button>
                    </span>
                </Space>
            </Card>

            {/* BPMN 流程图 */}
            <Card
                title="流程图"
                size="small"
                style={{ marginBottom: 16 }}
                styles={{
                    body: {
                        height: isFullscreen ? 'calc(100vh - 200px)' : 400,
                        padding: 0,
                    },
                }}
            >
                <div
                    ref={containerRef}
                    style={{
                        width: '100%',
                        height: '100%',
                    }}
                />
            </Card>

            {/* 活动历史时间线 */}
            <Row gutter={16}>
                <Col span={8}>
                    <Card title="执行时间线" size="small">
                        <Timeline
                            mode="left"
                            items={tracking.activityHistories?.slice(0, 10).map((activity) => ({
                                color: activity.canceled
                                    ? 'gray'
                                    : activity.ended
                                        ? 'green'
                                        : 'blue',
                                dot: activity.ended ? (
                                    <CheckCircleOutlined />
                                ) : (
                                    <SyncOutlined spin />
                                ),
                                children: (
                                    <div>
                                        <div>{activity.activityName || activity.activityId}</div>
                                        <div style={{ fontSize: 12, color: '#999' }}>
                                            {activity.startTime
                                                ? dayjs(activity.startTime).format('HH:mm:ss')
                                                : '-'}
                                        </div>
                                    </div>
                                ),
                            }))}
                        />
                        {(tracking.activityHistories?.length || 0) > 10 && (
                            <div style={{ textAlign: 'center', color: '#999' }}>
                                还有 {tracking.activityHistories.length - 10} 条记录...
                            </div>
                        )}
                    </Card>
                </Col>
                <Col span={16}>
                    <Card title="活动历史详情" size="small">
                        <Table
                            columns={activityColumns}
                            dataSource={tracking.activityHistories}
                            rowKey="id"
                            size="small"
                            pagination={{ pageSize: 5, showSizeChanger: false }}
                            scroll={{ x: 600 }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* 高亮 CSS */}
            <style>{`
        .highlight-completed .djs-visual > :nth-child(1) {
          stroke: ${COLORS.COMPLETED} !important;
          stroke-width: 2px !important;
        }
        .highlight-active .djs-visual > :nth-child(1) {
          stroke: ${COLORS.ACTIVE} !important;
          stroke-width: 3px !important;
        }
        .highlight-failed .djs-visual > :nth-child(1) {
          stroke: ${COLORS.FAILED} !important;
          stroke-width: 3px !important;
        }
        @keyframes pulse {
          0% { transform: scale(1); opacity: 1; }
          50% { transform: scale(1.1); opacity: 0.8; }
          100% { transform: scale(1); opacity: 1; }
        }
      `}</style>
        </div>
    )
}

export default ProcessTrackingViewer

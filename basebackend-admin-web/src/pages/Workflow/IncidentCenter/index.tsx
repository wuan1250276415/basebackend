import React, { useState, useEffect, useCallback } from 'react'
import {
    Card,
    Table,
    Tag,
    Button,
    Space,
    message,
    Tooltip,
    Modal,
    Input,
    Row,
    Col,
    Statistic,
    Popconfirm,
    Descriptions,
    Timeline,
    Alert,
} from 'antd'
import {
    ReloadOutlined,
    ExclamationCircleOutlined,
    WarningOutlined,
    CheckCircleOutlined,
    FileTextOutlined,
    LinkOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import {
    listIncidents,
    getIncidentById,
    resolveIncident,
    setIncidentAnnotation,
    getIncidentStatistics,
    type Incident,
    type IncidentQueryParams,
    type IncidentStatistics,
} from '@/api/workflow/incident'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search, TextArea } = Input

const IncidentCenter: React.FC = () => {
    // 状态
    const [loading, setLoading] = useState(false)
    const [incidents, setIncidents] = useState<Incident[]>([])
    const [total, setTotal] = useState(0)
    const [current, setCurrent] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [statistics, setStatistics] = useState<IncidentStatistics | null>(null)

    // 详情弹窗
    const [detailModalVisible, setDetailModalVisible] = useState(false)
    const [currentIncident, setCurrentIncident] = useState<Incident | null>(null)
    const [detailLoading, setDetailLoading] = useState(false)

    // 注解弹窗
    const [annotationModalVisible, setAnnotationModalVisible] = useState(false)
    const [annotationIncidentId, setAnnotationIncidentId] = useState('')
    const [annotationText, setAnnotationText] = useState('')

    // 搜索条件
    const [processDefinitionKey, setProcessDefinitionKey] = useState('')

    // 加载异常事件列表
    const loadIncidents = useCallback(async () => {
        setLoading(true)
        try {
            const params: IncidentQueryParams = {
                current,
                size: pageSize,
                sortBy: 'incidentTimestamp',
                sortOrder: 'desc',
            }

            if (processDefinitionKey) {
                params.processDefinitionKey = processDefinitionKey
            }

            const response = await listIncidents(params)
            if (response.code === 200) {
                setIncidents(response.data?.records || [])
                setTotal(response.data?.total || 0)
            } else {
                message.error(response.message || '加载异常事件列表失败')
            }
        } catch (error) {
            message.error('加载异常事件列表失败')
            console.error(error)
        } finally {
            setLoading(false)
        }
    }, [current, pageSize, processDefinitionKey])

    // 加载统计信息
    const loadStatistics = useCallback(async () => {
        try {
            const response = await getIncidentStatistics()
            if (response.code === 200) {
                setStatistics(response.data)
            }
        } catch (error) {
            console.error('Failed to load statistics', error)
        }
    }, [])

    useEffect(() => {
        loadIncidents()
        loadStatistics()
    }, [loadIncidents, loadStatistics])

    // 查看详情
    const handleViewDetail = async (incident: Incident) => {
        setDetailLoading(true)
        setDetailModalVisible(true)
        try {
            const response = await getIncidentById(incident.id)
            if (response.code === 200) {
                setCurrentIncident(response.data)
            } else {
                message.error(response.message || '获取异常事件详情失败')
            }
        } catch (error) {
            message.error('获取异常事件详情失败')
            console.error(error)
        } finally {
            setDetailLoading(false)
        }
    }

    // 解决异常事件
    const handleResolve = async (incidentId: string) => {
        try {
            const response = await resolveIncident(incidentId)
            if (response.code === 200) {
                message.success('异常事件处理已启动')
                loadIncidents()
                loadStatistics()
            } else {
                message.error(response.message || '处理失败')
            }
        } catch (error) {
            message.error('处理失败')
            console.error(error)
        }
    }

    // 打开注解弹窗
    const handleOpenAnnotation = (incident: Incident) => {
        setAnnotationIncidentId(incident.id)
        setAnnotationText(incident.annotation || '')
        setAnnotationModalVisible(true)
    }

    // 保存注解
    const handleSaveAnnotation = async () => {
        try {
            const response = await setIncidentAnnotation(annotationIncidentId, annotationText)
            if (response.code === 200) {
                message.success('注解保存成功')
                setAnnotationModalVisible(false)
                loadIncidents()
            } else {
                message.error(response.message || '保存失败')
            }
        } catch (error) {
            message.error('保存失败')
            console.error(error)
        }
    }

    // 获取异常类型标签
    const getTypeTag = (type: string) => {
        switch (type) {
            case 'failedJob':
                return <Tag color="error" icon={<WarningOutlined />}>作业失败</Tag>
            case 'failedExternalTask':
                return <Tag color="error" icon={<ExclamationCircleOutlined />}>外部任务失败</Tag>
            default:
                return <Tag color="default">{type}</Tag>
        }
    }

    // 表格列定义
    const columns: ColumnsType<Incident> = [
        {
            title: 'ID',
            dataIndex: 'id',
            key: 'id',
            width: 100,
            ellipsis: true,
            render: (text, record) => (
                <a onClick={() => handleViewDetail(record)}>{text.substring(0, 8)}...</a>
            ),
        },
        {
            title: '异常类型',
            dataIndex: 'incidentType',
            key: 'incidentType',
            width: 120,
            render: (text) => getTypeTag(text),
        },
        {
            title: '异常消息',
            dataIndex: 'incidentMessage',
            key: 'incidentMessage',
            ellipsis: true,
            render: (text) =>
                text ? (
                    <Tooltip title={text}>
                        <span style={{ color: '#ff4d4f' }}>{text}</span>
                    </Tooltip>
                ) : (
                    '-'
                ),
        },
        {
            title: '活动 ID',
            dataIndex: 'activityId',
            key: 'activityId',
            width: 150,
            ellipsis: true,
        },
        {
            title: '流程实例',
            dataIndex: 'processInstanceId',
            key: 'processInstanceId',
            width: 120,
            ellipsis: true,
            render: (text) => (
                <Tooltip title={text}>
                    <a>{text?.substring(0, 8)}...</a>
                </Tooltip>
            ),
        },
        {
            title: '发生时间',
            dataIndex: 'incidentTimestamp',
            key: 'incidentTimestamp',
            width: 180,
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
            title: '注解',
            dataIndex: 'annotation',
            key: 'annotation',
            width: 100,
            render: (text) =>
                text ? (
                    <Tooltip title={text}>
                        <Tag icon={<FileTextOutlined />} color="blue">
                            已备注
                        </Tag>
                    </Tooltip>
                ) : (
                    '-'
                ),
        },
        {
            title: '操作',
            key: 'action',
            width: 180,
            fixed: 'right',
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="解决（重试作业）">
                        <Popconfirm
                            title="确定要尝试解决此异常吗？"
                            description="将重试关联的失败作业"
                            onConfirm={() => handleResolve(record.id)}
                            okText="确定"
                            cancelText="取消"
                        >
                            <Button
                                type="text"
                                size="small"
                                icon={<CheckCircleOutlined />}
                                style={{ color: '#52c41a' }}
                            />
                        </Popconfirm>
                    </Tooltip>
                    <Tooltip title="添加注解">
                        <Button
                            type="text"
                            size="small"
                            icon={<FileTextOutlined />}
                            onClick={() => handleOpenAnnotation(record)}
                        />
                    </Tooltip>
                    <Tooltip title="查看详情">
                        <Button
                            type="text"
                            size="small"
                            icon={<LinkOutlined />}
                            onClick={() => handleViewDetail(record)}
                        />
                    </Tooltip>
                </Space>
            ),
        },
    ]

    return (
        <div>
            {/* 顶部告警 */}
            {statistics && statistics.totalCount > 0 && (
                <Alert
                    message={`当前存在 ${statistics.totalCount} 个未解决的异常事件`}
                    type="warning"
                    showIcon
                    icon={<ExclamationCircleOutlined />}
                    style={{ marginBottom: 16 }}
                />
            )}

            {/* 统计卡片 */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="异常事件总数"
                            value={statistics?.totalCount || 0}
                            valueStyle={{ color: '#cf1322' }}
                            prefix={<ExclamationCircleOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={9}>
                    <Card>
                        <div style={{ fontSize: 14, color: 'rgba(0, 0, 0, 0.45)', marginBottom: 8 }}>
                            按类型分布
                        </div>
                        <div>
                            {statistics?.byType &&
                                Object.entries(statistics.byType).map(([key, count]) => (
                                    <Tag key={key} color="red" style={{ marginBottom: 4 }}>
                                        {key}: {count}
                                    </Tag>
                                ))}
                            {(!statistics?.byType || Object.keys(statistics.byType).length === 0) && (
                                <span style={{ color: '#52c41a' }}>暂无异常 ✓</span>
                            )}
                        </div>
                    </Card>
                </Col>
                <Col span={9}>
                    <Card>
                        <div style={{ fontSize: 14, color: 'rgba(0, 0, 0, 0.45)', marginBottom: 8 }}>
                            按流程定义分布
                        </div>
                        <div>
                            {statistics?.byProcessDefinition &&
                                Object.entries(statistics.byProcessDefinition).map(([key, count]) => {
                                    const defKey = key.split(':')[0] || key
                                    return (
                                        <Tag key={key} color="orange" style={{ marginBottom: 4 }}>
                                            {defKey}: {count}
                                        </Tag>
                                    )
                                })}
                            {(!statistics?.byProcessDefinition ||
                                Object.keys(statistics.byProcessDefinition).length === 0) && (
                                    <span style={{ color: '#52c41a' }}>暂无异常 ✓</span>
                                )}
                        </div>
                    </Card>
                </Col>
            </Row>

            {/* 异常事件列表 */}
            <Card
                title={
                    <Space>
                        <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                        异常事件中心
                    </Space>
                }
                extra={
                    <Space>
                        <Search
                            placeholder="流程定义 Key"
                            allowClear
                            style={{ width: 200 }}
                            onSearch={(value) => {
                                setProcessDefinitionKey(value)
                                setCurrent(1)
                            }}
                        />
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={() => {
                                loadIncidents()
                                loadStatistics()
                            }}
                        >
                            刷新
                        </Button>
                    </Space>
                }
            >
                <Table
                    columns={columns}
                    dataSource={incidents}
                    rowKey="id"
                    loading={loading}
                    scroll={{ x: 1200 }}
                    pagination={{
                        current,
                        pageSize,
                        total,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        showTotal: (t) => `共 ${t} 条`,
                        onChange: (page, size) => {
                            setCurrent(page)
                            setPageSize(size)
                        },
                    }}
                />
            </Card>

            {/* 详情弹窗 */}
            <Modal
                title="异常事件详情"
                open={detailModalVisible}
                onCancel={() => setDetailModalVisible(false)}
                footer={
                    currentIncident && (
                        <Space>
                            <Button onClick={() => handleOpenAnnotation(currentIncident)}>
                                添加注解
                            </Button>
                            <Button
                                type="primary"
                                icon={<CheckCircleOutlined />}
                                onClick={() => {
                                    handleResolve(currentIncident.id)
                                    setDetailModalVisible(false)
                                }}
                            >
                                解决异常
                            </Button>
                        </Space>
                    )
                }
                width={800}
            >
                {detailLoading ? (
                    <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
                ) : currentIncident ? (
                    <div>
                        <Descriptions column={2} bordered size="small">
                            <Descriptions.Item label="异常 ID">{currentIncident.id}</Descriptions.Item>
                            <Descriptions.Item label="异常类型">
                                {getTypeTag(currentIncident.incidentType)}
                            </Descriptions.Item>
                            <Descriptions.Item label="活动 ID">{currentIncident.activityId}</Descriptions.Item>
                            <Descriptions.Item label="失败活动 ID">
                                {currentIncident.failedActivityId || '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="流程实例 ID">
                                {currentIncident.processInstanceId}
                            </Descriptions.Item>
                            <Descriptions.Item label="执行实例 ID">
                                {currentIncident.executionId}
                            </Descriptions.Item>
                            <Descriptions.Item label="流程定义 ID" span={2}>
                                {currentIncident.processDefinitionId}
                            </Descriptions.Item>
                            <Descriptions.Item label="发生时间" span={2}>
                                {currentIncident.incidentTimestamp
                                    ? dayjs(currentIncident.incidentTimestamp).format('YYYY-MM-DD HH:mm:ss')
                                    : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="关联配置" span={2}>
                                {currentIncident.configuration || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        {currentIncident.incidentMessage && (
                            <div style={{ marginTop: 16 }}>
                                <strong>异常消息：</strong>
                                <div
                                    style={{
                                        background: '#fff2f0',
                                        border: '1px solid #ffccc7',
                                        borderRadius: 4,
                                        padding: 12,
                                        marginTop: 8,
                                        color: '#cf1322',
                                    }}
                                >
                                    {currentIncident.incidentMessage}
                                </div>
                            </div>
                        )}

                        {currentIncident.annotation && (
                            <div style={{ marginTop: 16 }}>
                                <strong>注解：</strong>
                                <div
                                    style={{
                                        background: '#e6f7ff',
                                        border: '1px solid #91d5ff',
                                        borderRadius: 4,
                                        padding: 12,
                                        marginTop: 8,
                                    }}
                                >
                                    {currentIncident.annotation}
                                </div>
                            </div>
                        )}

                        {(currentIncident.rootCauseIncidentId || currentIncident.causeIncidentId) && (
                            <div style={{ marginTop: 16 }}>
                                <strong>关联异常：</strong>
                                <Timeline style={{ marginTop: 8 }}>
                                    {currentIncident.rootCauseIncidentId && (
                                        <Timeline.Item color="red">
                                            根因异常：{currentIncident.rootCauseIncidentId}
                                        </Timeline.Item>
                                    )}
                                    {currentIncident.causeIncidentId && (
                                        <Timeline.Item color="orange">
                                            原因异常：{currentIncident.causeIncidentId}
                                        </Timeline.Item>
                                    )}
                                    <Timeline.Item color="blue">
                                        当前异常：{currentIncident.id}
                                    </Timeline.Item>
                                </Timeline>
                            </div>
                        )}
                    </div>
                ) : null}
            </Modal>

            {/* 注解弹窗 */}
            <Modal
                title="添加注解"
                open={annotationModalVisible}
                onCancel={() => setAnnotationModalVisible(false)}
                onOk={handleSaveAnnotation}
                okText="保存"
                cancelText="取消"
            >
                <TextArea
                    rows={4}
                    placeholder="请输入注解内容..."
                    value={annotationText}
                    onChange={(e) => setAnnotationText(e.target.value)}
                />
            </Modal>
        </div>
    )
}

export default IncidentCenter

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
    Tabs,
    Input,
    Row,
    Col,
    Statistic,
    Badge,
    Popconfirm,
} from 'antd'
import {
    ReloadOutlined,
    DeleteOutlined,
    PlayCircleOutlined,
    PauseCircleOutlined,
    ExclamationCircleOutlined,
    ClockCircleOutlined,
    SyncOutlined,
    WarningOutlined,
    CheckCircleOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import {
    listJobs,
    getJobById,
    retryJob,
    executeJob,
    deleteJob,
    suspendJob,
    activateJob,
    batchRetryJobs,
    batchDeleteJobs,
    getJobStatistics,
    type Job,
    type JobDetail,
    type JobQueryParams,
    type JobStatistics,
} from '@/api/workflow/job'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search } = Input
const { TabPane } = Tabs
const { confirm } = Modal

const JobManagement: React.FC = () => {
    // 状态
    const [loading, setLoading] = useState(false)
    const [jobs, setJobs] = useState<Job[]>([])
    const [total, setTotal] = useState(0)
    const [current, setCurrent] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [activeTab, setActiveTab] = useState('all')
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
    const [statistics, setStatistics] = useState<JobStatistics | null>(null)

    // 详情弹窗
    const [detailModalVisible, setDetailModalVisible] = useState(false)
    const [currentJob, setCurrentJob] = useState<JobDetail | null>(null)
    const [detailLoading, setDetailLoading] = useState(false)

    // 搜索条件
    const [processDefinitionKey, setProcessDefinitionKey] = useState('')

    // 加载作业列表
    const loadJobs = useCallback(async () => {
        setLoading(true)
        try {
            const params: JobQueryParams = {
                current,
                size: pageSize,
                sortBy: 'jobDuedate',
                sortOrder: 'asc',
            }

            // 根据 Tab 设置过滤条件
            if (activeTab === 'failed') {
                params.failedOnly = true
            } else if (activeTab === 'executable') {
                params.executableOnly = true
            } else if (activeTab === 'suspended') {
                params.suspendedOnly = true
            }

            // 搜索条件
            if (processDefinitionKey) {
                params.processDefinitionKey = processDefinitionKey
            }

            const response = await listJobs(params)
            if (response.code === 200) {
                setJobs(response.data?.records || [])
                setTotal(response.data?.total || 0)
            } else {
                message.error(response.message || '加载作业列表失败')
            }
        } catch (error) {
            message.error('加载作业列表失败')
            console.error(error)
        } finally {
            setLoading(false)
        }
    }, [current, pageSize, activeTab, processDefinitionKey])

    // 加载统计信息
    const loadStatistics = useCallback(async () => {
        try {
            const response = await getJobStatistics()
            if (response.code === 200) {
                setStatistics(response.data)
            }
        } catch (error) {
            console.error('Failed to load statistics', error)
        }
    }, [])

    useEffect(() => {
        loadJobs()
        loadStatistics()
    }, [loadJobs, loadStatistics])

    // 查看详情
    const handleViewDetail = async (job: Job) => {
        setDetailLoading(true)
        setDetailModalVisible(true)
        try {
            const response = await getJobById(job.id)
            if (response.code === 200) {
                setCurrentJob(response.data)
            } else {
                message.error(response.message || '获取作业详情失败')
            }
        } catch (error) {
            message.error('获取作业详情失败')
            console.error(error)
        } finally {
            setDetailLoading(false)
        }
    }

    // 重试作业
    const handleRetry = async (jobId: string, retries?: number) => {
        try {
            const response = await retryJob(jobId, { retries: retries || 3 })
            if (response.code === 200) {
                message.success('作业已设置重试')
                loadJobs()
                loadStatistics()
            } else {
                message.error(response.message || '重试失败')
            }
        } catch (error) {
            message.error('重试失败')
            console.error(error)
        }
    }

    // 立即执行
    const handleExecute = async (jobId: string) => {
        try {
            const response = await executeJob(jobId)
            if (response.code === 200) {
                message.success('作业执行成功')
                loadJobs()
                loadStatistics()
            } else {
                message.error(response.message || '执行失败')
            }
        } catch (error) {
            message.error('执行失败')
            console.error(error)
        }
    }

    // 删除作业
    const handleDelete = async (jobId: string) => {
        try {
            const response = await deleteJob(jobId)
            if (response.code === 200) {
                message.success('作业删除成功')
                loadJobs()
                loadStatistics()
            } else {
                message.error(response.message || '删除失败')
            }
        } catch (error) {
            message.error('删除失败')
            console.error(error)
        }
    }

    // 挂起/激活
    const handleSuspend = async (job: Job) => {
        try {
            const response = job.suspended
                ? await activateJob(job.id)
                : await suspendJob(job.id)
            if (response.code === 200) {
                message.success(job.suspended ? '作业已激活' : '作业已挂起')
                loadJobs()
            } else {
                message.error(response.message || '操作失败')
            }
        } catch (error) {
            message.error('操作失败')
            console.error(error)
        }
    }

    // 批量重试
    const handleBatchRetry = async () => {
        if (selectedRowKeys.length === 0) {
            message.warning('请选择要重试的作业')
            return
        }

        confirm({
            title: '批量重试',
            icon: <ExclamationCircleOutlined />,
            content: `确定要重试选中的 ${selectedRowKeys.length} 个作业吗？`,
            onOk: async () => {
                try {
                    const response = await batchRetryJobs(selectedRowKeys as string[], 3)
                    if (response.code === 200) {
                        message.success(`成功重试 ${response.data.success} 个作业`)
                        setSelectedRowKeys([])
                        loadJobs()
                        loadStatistics()
                    } else {
                        message.error(response.message || '批量重试失败')
                    }
                } catch (error) {
                    message.error('批量重试失败')
                    console.error(error)
                }
            },
        })
    }

    // 批量删除
    const handleBatchDelete = async () => {
        if (selectedRowKeys.length === 0) {
            message.warning('请选择要删除的作业')
            return
        }

        confirm({
            title: '批量删除',
            icon: <ExclamationCircleOutlined />,
            content: `确定要删除选中的 ${selectedRowKeys.length} 个作业吗？此操作不可恢复！`,
            okType: 'danger',
            onOk: async () => {
                try {
                    const response = await batchDeleteJobs(selectedRowKeys as string[])
                    if (response.code === 200) {
                        message.success(`成功删除 ${response.data.success} 个作业`)
                        setSelectedRowKeys([])
                        loadJobs()
                        loadStatistics()
                    } else {
                        message.error(response.message || '批量删除失败')
                    }
                } catch (error) {
                    message.error('批量删除失败')
                    console.error(error)
                }
            },
        })
    }

    // 获取状态标签
    const getStatusTag = (job: Job) => {
        if (job.failed) {
            return <Tag color="error" icon={<WarningOutlined />}>失败</Tag>
        }
        if (job.suspended) {
            return <Tag color="warning" icon={<PauseCircleOutlined />}>挂起</Tag>
        }
        if (job.retries > 0 && job.duedate && dayjs(job.duedate).isBefore(dayjs())) {
            return <Tag color="processing" icon={<SyncOutlined spin />}>可执行</Tag>
        }
        return <Tag color="default" icon={<ClockCircleOutlined />}>等待</Tag>
    }

    // 表格列定义
    const columns: ColumnsType<Job> = [
        {
            title: '作业 ID',
            dataIndex: 'id',
            key: 'id',
            width: 120,
            ellipsis: true,
            render: (text, record) => (
                <a onClick={() => handleViewDetail(record)}>{text}</a>
            ),
        },
        {
            title: '流程定义',
            dataIndex: 'processDefinitionKey',
            key: 'processDefinitionKey',
            width: 150,
            ellipsis: true,
        },
        {
            title: '状态',
            key: 'status',
            width: 100,
            render: (_, record) => getStatusTag(record),
        },
        {
            title: '重试次数',
            dataIndex: 'retries',
            key: 'retries',
            width: 90,
            render: (retries) => (
                <Badge
                    count={retries}
                    style={{
                        backgroundColor: retries === 0 ? '#ff4d4f' : retries <= 1 ? '#faad14' : '#52c41a',
                    }}
                />
            ),
        },
        {
            title: '截止时间',
            dataIndex: 'duedate',
            key: 'duedate',
            width: 180,
            render: (text) =>
                text ? (
                    <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
                        <span
                            style={{
                                color: dayjs(text).isBefore(dayjs()) ? '#1890ff' : 'inherit',
                            }}
                        >
                            {dayjs(text).fromNow()}
                        </span>
                    </Tooltip>
                ) : (
                    '-'
                ),
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
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
            title: '异常信息',
            dataIndex: 'exceptionMessage',
            key: 'exceptionMessage',
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
            title: '操作',
            key: 'action',
            width: 200,
            fixed: 'right',
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="重试">
                        <Button
                            type="text"
                            size="small"
                            icon={<ReloadOutlined />}
                            onClick={() => handleRetry(record.id)}
                        />
                    </Tooltip>
                    <Tooltip title="立即执行">
                        <Button
                            type="text"
                            size="small"
                            icon={<PlayCircleOutlined />}
                            onClick={() => handleExecute(record.id)}
                            disabled={record.suspended}
                        />
                    </Tooltip>
                    <Tooltip title={record.suspended ? '激活' : '挂起'}>
                        <Button
                            type="text"
                            size="small"
                            icon={record.suspended ? <CheckCircleOutlined /> : <PauseCircleOutlined />}
                            onClick={() => handleSuspend(record)}
                        />
                    </Tooltip>
                    <Popconfirm
                        title="确定要删除此作业吗？"
                        onConfirm={() => handleDelete(record.id)}
                        okText="确定"
                        cancelText="取消"
                    >
                        <Tooltip title="删除">
                            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                        </Tooltip>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    // 行选择配置
    const rowSelection = {
        selectedRowKeys,
        onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    }

    return (
        <div>
            {/* 统计卡片 */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="失败作业"
                            value={statistics?.failedJobCount || 0}
                            valueStyle={{ color: '#cf1322' }}
                            prefix={<WarningOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="可执行作业"
                            value={statistics?.executableJobCount || 0}
                            valueStyle={{ color: '#3f8600' }}
                            prefix={<PlayCircleOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={12}>
                    <Card>
                        <div style={{ fontSize: 14, color: 'rgba(0, 0, 0, 0.45)', marginBottom: 8 }}>
                            按流程定义统计失败作业
                        </div>
                        <div>
                            {statistics?.failedJobsByProcessDefinition &&
                                Object.entries(statistics.failedJobsByProcessDefinition).map(
                                    ([key, count]) => (
                                        <Tag key={key} color="red" style={{ marginBottom: 4 }}>
                                            {key}: {count}
                                        </Tag>
                                    )
                                )}
                            {(!statistics?.failedJobsByProcessDefinition ||
                                Object.keys(statistics.failedJobsByProcessDefinition).length === 0) && (
                                    <span style={{ color: '#52c41a' }}>暂无失败作业 ✓</span>
                                )}
                        </div>
                    </Card>
                </Col>
            </Row>

            {/* 作业列表 */}
            <Card
                title="作业管理"
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
                        <Button icon={<ReloadOutlined />} onClick={() => { loadJobs(); loadStatistics(); }}>
                            刷新
                        </Button>
                    </Space>
                }
            >
                <Tabs
                    activeKey={activeTab}
                    onChange={(key) => {
                        setActiveTab(key)
                        setCurrent(1)
                        setSelectedRowKeys([])
                    }}
                    tabBarExtraContent={
                        <Space>
                            <Button
                                type="primary"
                                ghost
                                icon={<ReloadOutlined />}
                                disabled={selectedRowKeys.length === 0}
                                onClick={handleBatchRetry}
                            >
                                批量重试
                            </Button>
                            <Button
                                danger
                                ghost
                                icon={<DeleteOutlined />}
                                disabled={selectedRowKeys.length === 0}
                                onClick={handleBatchDelete}
                            >
                                批量删除
                            </Button>
                        </Space>
                    }
                >
                    <TabPane tab="全部作业" key="all" />
                    <TabPane tab={<span><WarningOutlined /> 失败作业</span>} key="failed" />
                    <TabPane tab={<span><PlayCircleOutlined /> 可执行作业</span>} key="executable" />
                    <TabPane tab={<span><PauseCircleOutlined /> 挂起作业</span>} key="suspended" />
                </Tabs>

                <Table
                    rowSelection={rowSelection}
                    columns={columns}
                    dataSource={jobs}
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
                title="作业详情"
                open={detailModalVisible}
                onCancel={() => setDetailModalVisible(false)}
                footer={null}
                width={800}
            >
                {detailLoading ? (
                    <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
                ) : currentJob ? (
                    <div>
                        <Row gutter={[16, 16]}>
                            <Col span={12}>
                                <strong>作业 ID：</strong> {currentJob.id}
                            </Col>
                            <Col span={12}>
                                <strong>状态：</strong> {getStatusTag(currentJob)}
                            </Col>
                            <Col span={12}>
                                <strong>流程定义：</strong> {currentJob.processDefinitionKey}
                            </Col>
                            <Col span={12}>
                                <strong>流程实例 ID：</strong> {currentJob.processInstanceId}
                            </Col>
                            <Col span={12}>
                                <strong>重试次数：</strong> {currentJob.retries}
                            </Col>
                            <Col span={12}>
                                <strong>优先级：</strong> {currentJob.priority}
                            </Col>
                            <Col span={12}>
                                <strong>截止时间：</strong>{' '}
                                {currentJob.duedate
                                    ? dayjs(currentJob.duedate).format('YYYY-MM-DD HH:mm:ss')
                                    : '-'}
                            </Col>
                            <Col span={12}>
                                <strong>创建时间：</strong>{' '}
                                {currentJob.createTime
                                    ? dayjs(currentJob.createTime).format('YYYY-MM-DD HH:mm:ss')
                                    : '-'}
                            </Col>
                            <Col span={24}>
                                <strong>处理器类型：</strong> {currentJob.jobHandlerType}
                            </Col>
                        </Row>

                        {currentJob.exceptionMessage && (
                            <div style={{ marginTop: 16 }}>
                                <strong>异常信息：</strong>
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
                                    {currentJob.exceptionMessage}
                                </div>
                            </div>
                        )}

                        {currentJob.exceptionStacktrace && (
                            <div style={{ marginTop: 16 }}>
                                <strong>异常堆栈：</strong>
                                <pre
                                    style={{
                                        background: '#f5f5f5',
                                        border: '1px solid #d9d9d9',
                                        borderRadius: 4,
                                        padding: 12,
                                        marginTop: 8,
                                        maxHeight: 300,
                                        overflow: 'auto',
                                        fontSize: 12,
                                    }}
                                >
                                    {currentJob.exceptionStacktrace}
                                </pre>
                            </div>
                        )}

                        <div style={{ marginTop: 24, textAlign: 'right' }}>
                            <Space>
                                <Button icon={<ReloadOutlined />} onClick={() => handleRetry(currentJob.id)}>
                                    重试
                                </Button>
                                <Button
                                    type="primary"
                                    icon={<PlayCircleOutlined />}
                                    onClick={() => {
                                        handleExecute(currentJob.id)
                                        setDetailModalVisible(false)
                                    }}
                                    disabled={currentJob.suspended}
                                >
                                    立即执行
                                </Button>
                            </Space>
                        </div>
                    </div>
                ) : null}
            </Modal>
        </div>
    )
}

export default JobManagement

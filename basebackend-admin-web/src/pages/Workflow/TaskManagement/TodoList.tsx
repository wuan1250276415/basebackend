import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    Card,
    Table,
    Tag,
    Button,
    Tabs,
    Space,
    message,
    Tooltip,
    Badge,
    Modal,
    Input,
    Statistic,
    Row,
    Col,
    Alert,
} from 'antd'
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    UserOutlined,
    TeamOutlined,
    ExclamationCircleOutlined,
    WarningOutlined,
    ReloadOutlined,
    CheckOutlined,
    SendOutlined,
} from '@ant-design/icons'
import type { ColumnsType, TableRowSelection } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import {
    listTasks,
    claimTask,
    listOverdueTasks,
    listDueSoonTasks,
    countOverdueTasks,
    countDueSoonTasks,
    batchCompleteTasks,
    batchClaimTasks,
    batchDelegateTasks,
} from '@/api/workflow/task'
import { useAuthStore } from '@/stores/auth'
import type { Task } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const TodoList: React.FC = () => {
    const navigate = useNavigate()
    const { userInfo } = useAuthStore()

    const [loading, setLoading] = useState(false)
    const [tasks, setTasks] = useState<Task[]>([])
    const [activeTab, setActiveTab] = useState('assigned')
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])

    // 超时任务统计
    const [overdueCount, setOverdueCount] = useState(0)
    const [dueSoonCount, setDueSoonCount] = useState(0)

    // 批量操作弹窗
    const [batchDelegateVisible, setBatchDelegateVisible] = useState(false)
    const [delegateUserId, setDelegateUserId] = useState('')
    const [batchLoading, setBatchLoading] = useState(false)

    // 加载超时统计
    const loadOverdueStats = useCallback(async () => {
        try {
            const [overdueRes, dueSoonRes] = await Promise.all([
                countOverdueTasks(),
                countDueSoonTasks(24)
            ])
            if (overdueRes.code === 200) setOverdueCount(overdueRes.data || 0)
            if (dueSoonRes.code === 200) setDueSoonCount(dueSoonRes.data || 0)
        } catch (error) {
            console.error('Failed to load overdue stats', error)
        }
    }, [])

    // 加载任务列表
    const loadTasks = useCallback(async () => {
        if (!userInfo?.username) return

        setLoading(true)
        setSelectedRowKeys([])
        try {
            let response
            switch (activeTab) {
                case 'assigned':
                    // 我的待办任务
                    response = await listTasks({
                        assignee: userInfo.username,
                        current: 1,
                        size: 100,
                    })
                    break
                case 'candidate':
                    // 候选任务 (待认领)
                    response = await listTasks({
                        candidateUser: userInfo.username,
                        current: 1,
                        size: 100,
                    })
                    break
                case 'overdue':
                    // 已超时任务
                    response = await listOverdueTasks({
                        current: 1,
                        size: 100,
                    })
                    break
                case 'dueSoon':
                    // 即将超时任务
                    response = await listDueSoonTasks({
                        current: 1,
                        size: 100,
                    }, 24)
                    break
                default:
                    response = await listTasks({
                        assignee: userInfo.username,
                        current: 1,
                        size: 100,
                    })
            }

            if (response.code === 200) {
                setTasks(response.data?.records || [])
            } else {
                message.error(response.message || '加载任务失败')
            }
        } catch (error) {
            message.error('加载任务失败')
            console.error(error)
        } finally {
            setLoading(false)
        }
    }, [userInfo, activeTab])

    useEffect(() => {
        loadTasks()
        loadOverdueStats()
    }, [loadTasks, loadOverdueStats])

    // 处理任务 (办理)
    const handleProcess = (task: Task) => {
        navigate(`/workflow/todo/${task.id}`)
    }

    // 认领任务
    const handleClaim = async (task: Task) => {
        if (!userInfo?.username) return

        try {
            const response = await claimTask(task.id, { userId: userInfo.username })
            if (response.code === 200) {
                message.success('认领成功')
                loadTasks()
            } else {
                message.error(response.message || '认领失败')
            }
        } catch (error) {
            message.error('认领失败')
            console.error(error)
        }
    }

    // 批量完成任务
    const handleBatchComplete = async () => {
        if (selectedRowKeys.length === 0) {
            message.warning('请先选择要完成的任务')
            return
        }

        Modal.confirm({
            title: '批量完成任务',
            content: `确定要完成选中的 ${selectedRowKeys.length} 个任务吗？`,
            onOk: async () => {
                setBatchLoading(true)
                try {
                    const response = await batchCompleteTasks(selectedRowKeys as string[])
                    if (response.code === 200) {
                        message.success(`批量完成：成功 ${response.data?.success} 个，失败 ${response.data?.failed} 个`)
                        loadTasks()
                        loadOverdueStats()
                    } else {
                        message.error(response.message || '批量完成失败')
                    }
                } catch (error) {
                    message.error('批量完成失败')
                    console.error(error)
                } finally {
                    setBatchLoading(false)
                }
            }
        })
    }

    // 批量认领任务
    const handleBatchClaim = async () => {
        if (selectedRowKeys.length === 0) {
            message.warning('请先选择要认领的任务')
            return
        }
        if (!userInfo?.username) return

        Modal.confirm({
            title: '批量认领任务',
            content: `确定要认领选中的 ${selectedRowKeys.length} 个任务吗？`,
            onOk: async () => {
                setBatchLoading(true)
                try {
                    const response = await batchClaimTasks(selectedRowKeys as string[], userInfo.username)
                    if (response.code === 200) {
                        message.success(`批量认领：成功 ${response.data?.success} 个，失败 ${response.data?.failed} 个`)
                        loadTasks()
                    } else {
                        message.error(response.message || '批量认领失败')
                    }
                } catch (error) {
                    message.error('批量认领失败')
                    console.error(error)
                } finally {
                    setBatchLoading(false)
                }
            }
        })
    }

    // 批量委派任务
    const handleBatchDelegate = async () => {
        if (!delegateUserId.trim()) {
            message.warning('请输入被委派人用户名')
            return
        }

        setBatchLoading(true)
        try {
            const response = await batchDelegateTasks(selectedRowKeys as string[], delegateUserId.trim())
            if (response.code === 200) {
                message.success(`批量委派：成功 ${response.data?.success} 个，失败 ${response.data?.failed} 个`)
                setBatchDelegateVisible(false)
                setDelegateUserId('')
                loadTasks()
            } else {
                message.error(response.message || '批量委派失败')
            }
        } catch (error) {
            message.error('批量委派失败')
            console.error(error)
        } finally {
            setBatchLoading(false)
        }
    }

    // 获取超时状态标签
    const getDueDateTag = (dueDate: string | null | undefined) => {
        if (!dueDate) return null

        const due = dayjs(dueDate)
        const now = dayjs()

        if (due.isBefore(now)) {
            // 已超时
            return (
                <Tag icon={<ExclamationCircleOutlined />} color="error">
                    已超时
                </Tag>
            )
        } else if (due.diff(now, 'hour') <= 24) {
            // 24小时内超时
            return (
                <Tag icon={<WarningOutlined />} color="warning">
                    即将超时
                </Tag>
            )
        }
        return null
    }

    // 表格多选配置
    const rowSelection: TableRowSelection<Task> = {
        selectedRowKeys,
        onChange: (keys) => setSelectedRowKeys(keys),
    }

    const columns: ColumnsType<Task> = [
        {
            title: '任务名称',
            dataIndex: 'name',
            key: 'name',
            render: (text, record) => (
                <Space>
                    <span>{text}</span>
                    {record.priority >= 50 && (
                        <Tag color={record.priority >= 80 ? 'red' : 'orange'}>
                            {record.priority >= 80 ? '紧急' : '重要'}
                        </Tag>
                    )}
                    {getDueDateTag(record.dueDate)}
                </Space>
            ),
        },
        {
            title: '流程定义',
            dataIndex: 'processDefinitionId',
            key: 'processDefinitionId',
            ellipsis: true,
            width: 200,
            render: (text) => {
                // 提取流程定义 key
                const key = text?.split(':')[0] || text
                return <Tooltip title={text}>{key}</Tooltip>
            }
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 150,
            render: (text) => (
                <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
                    {dayjs(text).fromNow()}
                </Tooltip>
            ),
        },
        {
            title: '到期时间',
            dataIndex: 'dueDate',
            key: 'dueDate',
            width: 150,
            render: (text) => {
                if (!text) return '-'
                const isOverdue = dayjs(text).isBefore(dayjs())
                return (
                    <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
                        <span style={{ color: isOverdue ? '#ff4d4f' : 'inherit', fontWeight: isOverdue ? 'bold' : 'normal' }}>
                            {dayjs(text).fromNow()}
                        </span>
                    </Tooltip>
                )
            },
        },
        {
            title: '操作',
            key: 'action',
            width: 120,
            render: (_, record) => (
                <Space>
                    {activeTab === 'assigned' || activeTab === 'overdue' || activeTab === 'dueSoon' ? (
                        <Button
                            type="primary"
                            size="small"
                            icon={<CheckCircleOutlined />}
                            onClick={() => handleProcess(record)}
                        >
                            办理
                        </Button>
                    ) : (
                        <Button
                            type="primary"
                            size="small"
                            ghost
                            onClick={() => handleClaim(record)}
                        >
                            认领
                        </Button>
                    )}
                </Space>
            ),
        },
    ]

    // Tab 配置
    const tabItems = [
        {
            key: 'assigned',
            label: (
                <span>
                    <UserOutlined />
                    我的待办
                </span>
            ),
        },
        {
            key: 'candidate',
            label: (
                <span>
                    <TeamOutlined />
                    待认领
                </span>
            ),
        },
        {
            key: 'overdue',
            label: (
                <Badge count={overdueCount} size="small" offset={[8, 0]}>
                    <span style={{ color: overdueCount > 0 ? '#ff4d4f' : 'inherit' }}>
                        <ExclamationCircleOutlined />
                        已超时
                    </span>
                </Badge>
            ),
        },
        {
            key: 'dueSoon',
            label: (
                <Badge count={dueSoonCount} size="small" offset={[8, 0]}>
                    <span style={{ color: dueSoonCount > 0 ? '#faad14' : 'inherit' }}>
                        <ClockCircleOutlined />
                        即将超时
                    </span>
                </Badge>
            ),
        },
    ]

    return (
        <Card
            title="待办任务"
            extra={
                <Space>
                    <Button icon={<ReloadOutlined />} onClick={() => { loadTasks(); loadOverdueStats() }}>
                        刷新
                    </Button>
                </Space>
            }
        >
            {/* 超时告警 */}
            {overdueCount > 0 && (
                <Alert
                    message={`您有 ${overdueCount} 个已超时任务需要处理！`}
                    type="error"
                    showIcon
                    icon={<ExclamationCircleOutlined />}
                    style={{ marginBottom: 16 }}
                    action={
                        <Button size="small" danger onClick={() => setActiveTab('overdue')}>
                            查看
                        </Button>
                    }
                />
            )}

            {/* 统计卡片 */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
                <Col span={6}>
                    <Card size="small">
                        <Statistic
                            title="我的待办"
                            value={activeTab === 'assigned' ? tasks.length : '-'}
                            prefix={<UserOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic
                            title="待认领"
                            value={activeTab === 'candidate' ? tasks.length : '-'}
                            prefix={<TeamOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic
                            title="已超时"
                            value={overdueCount}
                            valueStyle={{ color: overdueCount > 0 ? '#ff4d4f' : undefined }}
                            prefix={<ExclamationCircleOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card size="small">
                        <Statistic
                            title="即将超时(24h)"
                            value={dueSoonCount}
                            valueStyle={{ color: dueSoonCount > 0 ? '#faad14' : undefined }}
                            prefix={<ClockCircleOutlined />}
                        />
                    </Card>
                </Col>
            </Row>

            <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={tabItems}
                tabBarExtraContent={
                    selectedRowKeys.length > 0 && (
                        <Space>
                            <span style={{ color: '#666' }}>已选 {selectedRowKeys.length} 项</span>
                            {activeTab === 'assigned' && (
                                <Button
                                    type="primary"
                                    size="small"
                                    icon={<CheckOutlined />}
                                    loading={batchLoading}
                                    onClick={handleBatchComplete}
                                >
                                    批量完成
                                </Button>
                            )}
                            {activeTab === 'candidate' && (
                                <Button
                                    type="primary"
                                    size="small"
                                    ghost
                                    loading={batchLoading}
                                    onClick={handleBatchClaim}
                                >
                                    批量认领
                                </Button>
                            )}
                            <Button
                                size="small"
                                icon={<SendOutlined />}
                                onClick={() => {
                                    if (selectedRowKeys.length === 0) {
                                        message.warning('请先选择要委派的任务')
                                        return
                                    }
                                    setBatchDelegateVisible(true)
                                }}
                            >
                                批量委派
                            </Button>
                        </Space>
                    )
                }
            />

            <Table
                columns={columns}
                dataSource={tasks}
                rowKey="id"
                loading={loading}
                rowSelection={rowSelection}
                pagination={{
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total) => `共 ${total} 条`,
                }}
            />

            {/* 批量委派弹窗 */}
            <Modal
                title="批量委派任务"
                open={batchDelegateVisible}
                onOk={handleBatchDelegate}
                onCancel={() => {
                    setBatchDelegateVisible(false)
                    setDelegateUserId('')
                }}
                confirmLoading={batchLoading}
            >
                <p>将选中的 {selectedRowKeys.length} 个任务委派给：</p>
                <Input
                    placeholder="请输入被委派人用户名"
                    value={delegateUserId}
                    onChange={(e) => setDelegateUserId(e.target.value)}
                />
            </Modal>
        </Card>
    )
}

export default TodoList

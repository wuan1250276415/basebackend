import React, { useState, useEffect } from 'react'
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
} from 'antd'
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    UserOutlined,
    TeamOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import { listTasks, claimTask } from '@/api/workflow/task'
import { useAuthStore } from '@/stores/auth'
import type { Task } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { TabPane } = Tabs

const TodoList: React.FC = () => {
    const navigate = useNavigate()
    const { userInfo } = useAuthStore()

    const [loading, setLoading] = useState(false)
    const [tasks, setTasks] = useState<Task[]>([])
    const [activeTab, setActiveTab] = useState('assigned')

    // 加载任务列表
    const loadTasks = async () => {
        if (!userInfo?.username) return

        setLoading(true)
        try {
            let response
            if (activeTab === 'assigned') {
                // 我的待办任务
                response = await listTasks({
                    assignee: userInfo.username,
                    current: 1,
                    size: 100, // 暂不分页，显示全部
                })
            } else {
                // 候选任务 (待认领)
                response = await listTasks({
                    candidateUser: userInfo.username,
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
    }

    useEffect(() => {
        loadTasks()
    }, [userInfo, activeTab])

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
                </Space>
            ),
        },
        {
            title: '流程定义ID',
            dataIndex: 'processDefinitionId',
            key: 'processDefinitionId',
            ellipsis: true,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 180,
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
            width: 180,
            render: (text) =>
                text ? (
                    <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
                        <span style={{ color: dayjs(text).isBefore(dayjs()) ? 'red' : 'inherit' }}>
                            {dayjs(text).fromNow()}
                        </span>
                    </Tooltip>
                ) : (
                    '-'
                ),
        },
        {
            title: '操作',
            key: 'action',
            width: 120,
            render: (_, record) => (
                <Space>
                    {activeTab === 'assigned' ? (
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

    return (
        <Card title="待办任务">
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
                <TabPane
                    tab={
                        <span>
                            <UserOutlined />
                            我的待办
                        </span>
                    }
                    key="assigned"
                />
                <TabPane
                    tab={
                        <span>
                            <TeamOutlined />
                            待认领任务
                        </span>
                    }
                    key="candidate"
                />
            </Tabs>

            <Table
                columns={columns}
                dataSource={tasks}
                rowKey="id"
                loading={loading}
                pagination={{
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total) => `共 ${total} 条`,
                }}
            />
        </Card>
    )
}

export default TodoList

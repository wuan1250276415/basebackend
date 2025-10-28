import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  message,
  Modal,
  Tag,
  Tooltip,
  Progress,
} from 'antd'
import {
  RollbackOutlined,
  DeleteOutlined,
  ClearOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FileRecycleBin } from '@/types/file'
import {
  getRecycleBinList,
  restoreFile,
  permanentDeleteFile,
  batchRestoreFiles,
  batchPermanentDelete,
  emptyRecycleBin,
} from '@/api/file'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const RecycleBin: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [recycleBinList, setRecycleBinList] = useState<FileRecycleBin[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])

  // 加载回收站列表
  const loadRecycleBinList = async () => {
    setLoading(true)
    try {
      const res = await getRecycleBinList({ current, size: pageSize })
      const pageData = res.data
      setRecycleBinList(pageData?.records ?? [])
      setTotal(pageData?.total ?? 0)
    } catch (error) {
      message.error('加载回收站列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRecycleBinList()
  }, [current, pageSize])

  // 恢复文件
  const handleRestore = async (item: FileRecycleBin) => {
    try {
      await restoreFile(item.fileId)
      message.success('文件恢复成功')
      loadRecycleBinList()
    } catch (error) {
      message.error('文件恢复失败')
    }
  }

  // 彻底删除
  const handlePermanentDelete = (item: FileRecycleBin) => {
    Modal.confirm({
      title: '确认彻底删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要彻底删除文件 "${item.fileName}" 吗？此操作不可恢复！`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await permanentDeleteFile(item.fileId)
          message.success('文件已彻底删除')
          loadRecycleBinList()
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  // 批量恢复
  const handleBatchRestore = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要恢复的文件')
      return
    }

    Modal.confirm({
      title: '确认批量恢复',
      content: `确定要恢复选中的 ${selectedRowKeys.length} 个文件吗？`,
      onOk: async () => {
        try {
          await batchRestoreFiles(selectedRowKeys as string[])
          message.success('批量恢复成功')
          setSelectedRowKeys([])
          loadRecycleBinList()
        } catch (error) {
          message.error('批量恢复失败')
        }
      },
    })
  }

  // 批量彻底删除
  const handleBatchPermanentDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的文件')
      return
    }

    Modal.confirm({
      title: '确认批量彻底删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要彻底删除选中的 ${selectedRowKeys.length} 个文件吗？此操作不可恢复！`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await batchPermanentDelete(selectedRowKeys as string[])
          message.success('批量删除成功')
          setSelectedRowKeys([])
          loadRecycleBinList()
        } catch (error) {
          message.error('批量删除失败')
        }
      },
    })
  }

  // 清空回收站
  const handleEmptyRecycleBin = () => {
    Modal.confirm({
      title: '确认清空回收站',
      icon: <ExclamationCircleOutlined />,
      content: '确定要清空回收站吗？所有文件将被彻底删除，此操作不可恢复！',
      okText: '确认清空',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await emptyRecycleBin()
          message.success('回收站已清空')
          loadRecycleBinList()
        } catch (error) {
          message.error('清空回收站失败')
        }
      },
    })
  }

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  // 计算剩余天数
  const getRemainingDays = (expireAt: string): number => {
    const now = dayjs()
    const expire = dayjs(expireAt)
    return expire.diff(now, 'day')
  }

  // 计算过期进度
  const getExpireProgress = (deletedAt: string, expireAt: string): number => {
    const deleted = dayjs(deletedAt)
    const expire = dayjs(expireAt)
    const now = dayjs()
    const total = expire.diff(deleted, 'second')
    const elapsed = now.diff(deleted, 'second')
    return Math.min(100, Math.round((elapsed / total) * 100))
  }

  const columns: ColumnsType<FileRecycleBin> = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      key: 'fileName',
      ellipsis: true,
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '删除人',
      dataIndex: 'deletedByName',
      key: 'deletedByName',
      width: 120,
    },
    {
      title: '删除时间',
      dataIndex: 'deletedAt',
      key: 'deletedAt',
      width: 180,
      render: (time: string) => (
        <Tooltip title={time}>
          {dayjs(time).fromNow()}
        </Tooltip>
      ),
    },
    {
      title: '剩余天数',
      key: 'remaining',
      width: 150,
      render: (_: any, record: FileRecycleBin) => {
        const remaining = getRemainingDays(record.expireAt)
        const progress = getExpireProgress(record.deletedAt, record.expireAt)

        return (
          <Tooltip title={`将在 ${record.expireAt} 自动删除`}>
            <div>
              <div style={{ marginBottom: 4 }}>
                <Tag color={remaining <= 7 ? 'red' : remaining <= 15 ? 'orange' : 'blue'}>
                  剩余 {remaining} 天
                </Tag>
              </div>
              <Progress
                percent={progress}
                size="small"
                showInfo={false}
                status={remaining <= 7 ? 'exception' : 'active'}
              />
            </div>
          </Tooltip>
        )
      },
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 150,
      render: (_: any, record: FileRecycleBin) => (
        <Space size="small">
          <Tooltip title="恢复">
            <Button
              type="text"
              size="small"
              icon={<RollbackOutlined />}
              onClick={() => handleRestore(record)}
            />
          </Tooltip>
          <Tooltip title="彻底删除">
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handlePermanentDelete(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title="回收站"
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadRecycleBinList}>
            刷新
          </Button>
        }
      >
        {/* 工具栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<RollbackOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchRestore}
          >
            批量恢复
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchPermanentDelete}
          >
            批量删除
          </Button>
          <Button
            danger
            icon={<ClearOutlined />}
            onClick={handleEmptyRecycleBin}
          >
            清空回收站
          </Button>
        </Space>

        {/* 提示信息 */}
        <div
          style={{
            marginBottom: 16,
            padding: '12px',
            background: '#fef7e6',
            border: '1px solid #ffd591',
            borderRadius: 4,
          }}
        >
          <ExclamationCircleOutlined style={{ color: '#fa8c16', marginRight: 8 }} />
          回收站中的文件将在30天后自动删除。彻底删除的文件无法恢复，请谨慎操作。
        </div>

        {/* 列表 */}
        <Table
          rowKey="id"
          columns={columns}
          dataSource={recycleBinList}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          pagination={{
            current,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => {
              setCurrent(page)
              setPageSize(size)
            },
          }}
          scroll={{ x: 1000 }}
        />
      </Card>
    </div>
  )
}

export default RecycleBin

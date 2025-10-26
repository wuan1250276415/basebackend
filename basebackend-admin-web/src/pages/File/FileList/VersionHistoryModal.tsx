import React, { useState, useEffect } from 'react'
import {
  Modal,
  Table,
  Button,
  Space,
  message,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  DownloadOutlined,
  RollbackOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FileMetadata, FileVersion } from '@/types/file'
import { getFileVersions, revertToVersion, downloadVersion } from '@/api/file'

const { Text } = Typography

interface VersionHistoryModalProps {
  visible: boolean
  file: FileMetadata
  onCancel: () => void
}

const VersionHistoryModal: React.FC<VersionHistoryModalProps> = ({
  visible,
  file,
  onCancel,
}) => {
  const [loading, setLoading] = useState(false)
  const [versionList, setVersionList] = useState<FileVersion[]>([])

  const loadVersions = async () => {
    setLoading(true)
    try {
      const res = await getFileVersions(file.fileId)
      setVersionList(res.data || [])
    } catch (error) {
      message.error('加载版本历史失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (visible) {
      loadVersions()
    }
  }, [visible, file.fileId])

  const handleRevert = async (version: FileVersion) => {
    Modal.confirm({
      title: '确认回退',
      content: `确定要回退到版本 v${version.versionNumber} 吗？`,
      onOk: async () => {
        try {
          await revertToVersion(file.fileId, version.id)
          message.success('版本回退成功')
          loadVersions()
        } catch (error) {
          message.error('版本回退失败')
        }
      },
    })
  }

  const handleDownload = async (version: FileVersion) => {
    try {
      const blob = await downloadVersion(file.fileId, version.id)
      const url = window.URL.createObjectURL(blob as Blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${file.originalName}_v${version.versionNumber}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      message.success('下载成功')
    } catch (error) {
      message.error('下载失败')
    }
  }

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const columns: ColumnsType<FileVersion> = [
    {
      title: '版本号',
      dataIndex: 'versionNumber',
      key: 'versionNumber',
      width: 100,
      render: (version: number, record: FileVersion) => (
        <Space>
          <Tag color="blue">v{version}</Tag>
          {record.isCurrent && (
            <Tooltip title="当前版本">
              <CheckCircleOutlined style={{ color: '#52c41a' }} />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: 'MD5',
      dataIndex: 'md5',
      key: 'md5',
      ellipsis: true,
      render: (md5: string) => (
        <Tooltip title={md5}>
          <Text code copyable style={{ fontSize: 12 }}>
            {md5.substring(0, 16)}...
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '变更说明',
      dataIndex: 'changeDescription',
      key: 'changeDescription',
      ellipsis: true,
      render: (desc: string) => desc || '-',
    },
    {
      title: '创建人',
      dataIndex: 'createdByName',
      key: 'createdByName',
      width: 120,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 120,
      render: (_: any, record: FileVersion) => (
        <Space size="small">
          <Tooltip title="下载此版本">
            <Button
              type="text"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
            />
          </Tooltip>
          {!record.isCurrent && (
            <Tooltip title="回退到此版本">
              <Button
                type="text"
                size="small"
                icon={<RollbackOutlined />}
                onClick={() => handleRevert(record)}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Modal
      title={`版本历史 - ${file.originalName}`}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={1000}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={versionList}
        loading={loading}
        pagination={false}
        scroll={{ x: 800 }}
      />
    </Modal>
  )
}

export default VersionHistoryModal

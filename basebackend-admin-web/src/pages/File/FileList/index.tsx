import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  message,
  Modal,
  Dropdown,
  Tag,
  Tooltip,
  Switch,
  Row,
  Col,
  Statistic,
} from 'antd'
import {
  UploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  ShareAltOutlined,
  FolderOutlined,
  FileOutlined,
  AppstoreOutlined,
  BarsOutlined,
  HistoryOutlined,
  SettingOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FileMetadata, ViewMode } from '@/types/file'
import {
  getFileList,
  deleteFile,
  downloadFile,
  batchDeleteFiles,
  getFileStatistics,
} from '@/api/file'
import FileUploadModal from './FileUploadModal'
import FileDetailModal from './FileDetailModal'
import FileShareModal from './FileShareModal'
import VersionHistoryModal from './VersionHistoryModal'
import FileGridView from './FileGridView'

const { Search } = Input

const FileList: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [fileList, setFileList] = useState<FileMetadata[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [searchText, setSearchText] = useState('')
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [viewMode, setViewMode] = useState<ViewMode>('list')

  // Modal状态
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [shareModalVisible, setShareModalVisible] = useState(false)
  const [versionModalVisible, setVersionModalVisible] = useState(false)
  const [currentFile, setCurrentFile] = useState<FileMetadata | null>(null)

  // 统计信息
  const [statistics, setStatistics] = useState<any>(null)

  // 加载文件列表
  const loadFileList = async () => {
    setLoading(true)
    try {
      const params = {
        current,
        size: pageSize,
        fileName: searchText || undefined,
      }
      const res = await getFileList(params)
      setFileList(res.records)
      setTotal(res.total)
    } catch (error) {
      message.error('加载文件列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 加载统计信息
  const loadStatistics = async () => {
    try {
      const res = await getFileStatistics()
      setStatistics(res.data)
    } catch (error) {
      console.error('加载统计信息失败', error)
    }
  }

  useEffect(() => {
    loadFileList()
    loadStatistics()
  }, [current, pageSize])

  // 搜索
  const handleSearch = () => {
    setCurrent(1)
    loadFileList()
  }

  // 下载文件
  const handleDownload = async (file: FileMetadata) => {
    try {
      const blob = await downloadFile(file.fileId)
      const url = window.URL.createObjectURL(blob as Blob)
      const link = document.createElement('a')
      link.href = url
      link.download = file.originalName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      message.success('下载成功')
    } catch (error) {
      message.error('下载失败')
    }
  }

  // 删除文件
  const handleDelete = (file: FileMetadata) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除文件 "${file.originalName}" 吗？文件将被移入回收站。`,
      onOk: async () => {
        try {
          await deleteFile(file.fileId)
          message.success('删除成功')
          loadFileList()
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  // 批量删除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的文件')
      return
    }

    Modal.confirm({
      title: '确认批量删除',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个文件吗？`,
      onOk: async () => {
        try {
          await batchDeleteFiles(selectedRowKeys as string[])
          message.success('批量删除成功')
          setSelectedRowKeys([])
          loadFileList()
        } catch (error) {
          message.error('批量删除失败')
        }
      },
    })
  }

  // 查看详情
  const handleViewDetail = (file: FileMetadata) => {
    setCurrentFile(file)
    setDetailModalVisible(true)
  }

  // 查看版本历史
  const handleViewVersions = (file: FileMetadata) => {
    setCurrentFile(file)
    setVersionModalVisible(true)
  }

  // 分享文件
  const handleShare = (file: FileMetadata) => {
    setCurrentFile(file)
    setShareModalVisible(true)
  }

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  // 表格列定义
  const columns: ColumnsType<FileMetadata> = [
    {
      title: '文件名',
      dataIndex: 'originalName',
      key: 'originalName',
      ellipsis: true,
      render: (text: string, record: FileMetadata) => (
        <Space>
          {record.isFolder ? <FolderOutlined /> : <FileOutlined />}
          <a onClick={() => handleViewDetail(record)}>{text}</a>
        </Space>
      ),
    },
    {
      title: '文件类型',
      dataIndex: 'fileExtension',
      key: 'fileExtension',
      width: 100,
      render: (text: string) => <Tag>{text ? text.toUpperCase() : '-'}</Tag>,
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '所有者',
      dataIndex: 'ownerName',
      key: 'ownerName',
      width: 120,
    },
    {
      title: '存储类型',
      dataIndex: 'storageType',
      key: 'storageType',
      width: 100,
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '公开',
      dataIndex: 'isPublic',
      key: 'isPublic',
      width: 80,
      render: (isPublic: boolean) => (
        <Tag color={isPublic ? 'green' : 'default'}>
          {isPublic ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '下载次数',
      dataIndex: 'downloadCount',
      key: 'downloadCount',
      width: 100,
    },
    {
      title: '上传时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 200,
      render: (_: any, record: FileMetadata) => (
        <Space size="small">
          <Tooltip title="下载">
            <Button
              type="text"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
            />
          </Tooltip>
          <Tooltip title="预览">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            />
          </Tooltip>
          <Tooltip title="版本历史">
            <Button
              type="text"
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => handleViewVersions(record)}
            />
          </Tooltip>
          <Tooltip title="分享">
            <Button
              type="text"
              size="small"
              icon={<ShareAltOutlined />}
              onClick={() => handleShare(record)}
            />
          </Tooltip>
          <Tooltip title="删除">
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDelete(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      {/* 统计信息 */}
      {statistics && (
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="总文件数"
                value={statistics.totalFiles}
                suffix="个"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="总大小"
                value={formatFileSize(statistics.totalSize)}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="本地存储"
                value={formatFileSize(statistics.storageUsage?.local || 0)}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="对象存储"
                value={formatFileSize(statistics.storageUsage?.minio || 0)}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* 主卡片 */}
      <Card
        title="文件管理"
        extra={
          <Space>
            <Tooltip title={viewMode === 'list' ? '切换到网格视图' : '切换到列表视图'}>
              <Button
                icon={viewMode === 'list' ? <AppstoreOutlined /> : <BarsOutlined />}
                onClick={() => setViewMode(viewMode === 'list' ? 'grid' : 'list')}
              />
            </Tooltip>
            <Button icon={<ReloadOutlined />} onClick={loadFileList}>
              刷新
            </Button>
          </Space>
        }
      >
        {/* 工具栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            onClick={() => setUploadModalVisible(true)}
          >
            上传文件
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchDelete}
          >
            批量删除
          </Button>
          <Search
            placeholder="搜索文件名"
            allowClear
            style={{ width: 300 }}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onSearch={handleSearch}
          />
        </Space>

        {/* 文件列表 */}
        {viewMode === 'list' ? (
          <Table
            rowKey="fileId"
            columns={columns}
            dataSource={fileList}
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
            scroll={{ x: 1200 }}
          />
        ) : (
          <FileGridView
            fileList={fileList}
            loading={loading}
            onDownload={handleDownload}
            onDelete={handleDelete}
            onViewDetail={handleViewDetail}
            onShare={handleShare}
          />
        )}
      </Card>

      {/* 上传文件弹窗 */}
      <FileUploadModal
        visible={uploadModalVisible}
        onCancel={() => setUploadModalVisible(false)}
        onSuccess={() => {
          setUploadModalVisible(false)
          loadFileList()
        }}
      />

      {/* 文件详情弹窗 */}
      {currentFile && (
        <FileDetailModal
          visible={detailModalVisible}
          file={currentFile}
          onCancel={() => setDetailModalVisible(false)}
        />
      )}

      {/* 分享弹窗 */}
      {currentFile && (
        <FileShareModal
          visible={shareModalVisible}
          file={currentFile}
          onCancel={() => setShareModalVisible(false)}
        />
      )}

      {/* 版本历史弹窗 */}
      {currentFile && (
        <VersionHistoryModal
          visible={versionModalVisible}
          file={currentFile}
          onCancel={() => setVersionModalVisible(false)}
        />
      )}
    </div>
  )
}

export default FileList

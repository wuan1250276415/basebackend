import React from 'react'
import { Modal, Descriptions, Tag, Space } from 'antd'
import type { FileMetadata } from '@/types/file'

interface FileDetailModalProps {
  visible: boolean
  file: FileMetadata
  onCancel: () => void
}

const FileDetailModal: React.FC<FileDetailModalProps> = ({
  visible,
  file,
  onCancel,
}) => {
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  return (
    <Modal
      title="文件详情"
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={800}
    >
      <Descriptions column={2} bordered>
        <Descriptions.Item label="文件名" span={2}>
          {file.originalName}
        </Descriptions.Item>
        <Descriptions.Item label="文件ID">
          {file.fileId}
        </Descriptions.Item>
        <Descriptions.Item label="文件大小">
          {formatFileSize(file.fileSize)}
        </Descriptions.Item>
        <Descriptions.Item label="文件类型">
          {file.contentType}
        </Descriptions.Item>
        <Descriptions.Item label="文件扩展名">
          <Tag>{file.fileExtension?.toUpperCase()}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="存储类型">
          <Tag color="blue">{file.storageType}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="MD5">
          <code style={{ fontSize: 12 }}>{file.md5}</code>
        </Descriptions.Item>
        <Descriptions.Item label="所有者">
          {file.ownerName}
        </Descriptions.Item>
        <Descriptions.Item label="是否公开">
          <Tag color={file.isPublic ? 'green' : 'default'}>
            {file.isPublic ? '是' : '否'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="当前版本">
          v{file.version}
        </Descriptions.Item>
        <Descriptions.Item label="下载次数">
          {file.downloadCount}
        </Descriptions.Item>
        <Descriptions.Item label="浏览次数">
          {file.viewCount}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间">
          {file.createTime}
        </Descriptions.Item>
        <Descriptions.Item label="更新时间">
          {file.updateTime}
        </Descriptions.Item>
        {file.description && (
          <Descriptions.Item label="描述" span={2}>
            {file.description}
          </Descriptions.Item>
        )}
        <Descriptions.Item label="文件路径" span={2}>
          <code style={{ fontSize: 12 }}>{file.filePath}</code>
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  )
}

export default FileDetailModal

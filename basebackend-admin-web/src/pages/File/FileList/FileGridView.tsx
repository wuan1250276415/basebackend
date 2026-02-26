import { File, Folder, Download, Trash2, Eye, Share2, FileText, ImageIcon, FileSpreadsheet, FileArchive } from 'lucide-react';
import React from 'react'
import { Card, Row, Col, Space, Button, Tooltip, Tag, Empty, Spin } from 'antd'

import type { FileMetadata } from '@/types/file'

interface FileGridViewProps {
  fileList: FileMetadata[]
  loading: boolean
  onDownload: (file: FileMetadata) => void
  onDelete: (file: FileMetadata) => void
  onViewDetail: (file: FileMetadata) => void
  onShare: (file: FileMetadata) => void
}

const FileGridView: React.FC<FileGridViewProps> = ({
  fileList,
  loading,
  onDownload,
  onDelete,
  onViewDetail,
  onShare,
}) => {
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const getFileIcon = (file: FileMetadata) => {
    if (file.isFolder) return <Folder style={{ fontSize: 48 }} />

    const ext = file.fileExtension?.toLowerCase()
    const iconStyle = { fontSize: 48, color: '#1890ff' }

    switch (ext) {
      case 'pdf':
        return <FileText style={{ ...iconStyle, color: '#f5222d' }} />
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return <Image style={{ ...iconStyle, color: '#52c41a' }} />
      case 'doc':
      case 'docx':
        return <FileText style={{ ...iconStyle, color: '#1890ff' }} />
      case 'xls':
      case 'xlsx':
        return <FileSpreadsheet style={{ ...iconStyle, color: '#52c41a' }} />
      case 'zip':
      case 'rar':
      case '7z':
        return <FileArchive style={{ ...iconStyle, color: '#faad14' }} />
      case 'txt':
        return <FileText style={iconStyle} />
      default:
        return <File style={iconStyle} />
    }
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (fileList.length === 0) {
    return <Empty description="暂无文件" />
  }

  return (
    <Row gutter={[16, 16]}>
      {fileList.map((file) => (
        <Col key={file.fileId} xs={24} sm={12} md={8} lg={6} xl={4}>
          <Card
            hoverable
            style={{ height: '100%' }}
            bodyStyle={{
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <div
              style={{
                cursor: 'pointer',
                textAlign: 'center',
                marginBottom: 12,
              }}
              onClick={() => onViewDetail(file)}
            >
              {getFileIcon(file)}
              <div
                style={{
                  marginTop: 8,
                  fontSize: 14,
                  fontWeight: 500,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  width: '100%',
                }}
              >
                <Tooltip title={file.originalName}>
                  {file.originalName}
                </Tooltip>
              </div>
            </div>

            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div style={{ fontSize: 12, color: '#888' }}>
                {formatFileSize(file.fileSize)}
              </div>
              {file.fileExtension && (
                <Tag size="small">{file.fileExtension.toUpperCase()}</Tag>
              )}
              <Space size="small" style={{ width: '100%', justifyContent: 'center' }}>
                <Tooltip title="下载">
                  <Button
                    type="text"
                    size="small"
                    icon={<Download />}
                    onClick={() => onDownload(file)}
                  />
                </Tooltip>
                <Tooltip title="预览">
                  <Button
                    type="text"
                    size="small"
                    icon={<Eye />}
                    onClick={() => onViewDetail(file)}
                  />
                </Tooltip>
                <Tooltip title="分享">
                  <Button
                    type="text"
                    size="small"
                    icon={<Share2 />}
                    onClick={() => onShare(file)}
                  />
                </Tooltip>
                <Tooltip title="删除">
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<Trash2 />}
                    onClick={() => onDelete(file)}
                  />
                </Tooltip>
              </Space>
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  )
}

export default FileGridView

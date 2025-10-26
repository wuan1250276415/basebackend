import React, { useState } from 'react'
import { Modal, Upload, message, Progress, Form, InputNumber, Space, Alert } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { uploadFile } from '@/api/file'

const { Dragger } = Upload

interface FileUploadModalProps {
  visible: boolean
  folderId?: number
  onCancel: () => void
  onSuccess: () => void
}

const FileUploadModal: React.FC<FileUploadModalProps> = ({
  visible,
  folderId,
  onCancel,
  onSuccess,
}) => {
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [fileList, setFileList] = useState<any[]>([])
  const [form] = Form.useForm()

  const handleUpload = async () => {
    if (fileList.length === 0) {
      message.warning('请选择要上传的文件')
      return
    }

    setUploading(true)
    setUploadProgress(0)

    try {
      const file = fileList[0]
      const targetFolderId = form.getFieldValue('folderId') || folderId

      await uploadFile(file, targetFolderId, (percent) => {
        setUploadProgress(percent)
      })

      message.success('文件上传成功')
      setFileList([])
      form.resetFields()
      onSuccess()
    } catch (error: any) {
      message.error(error.response?.data?.message || '文件上传失败')
    } finally {
      setUploading(false)
      setUploadProgress(0)
    }
  }

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    fileList,
    beforeUpload: (file) => {
      // 检查文件大小（100MB）
      const maxSize = 100 * 1024 * 1024
      if (file.size > maxSize) {
        message.error('文件大小不能超过100MB')
        return false
      }

      setFileList([file])
      return false // 阻止自动上传
    },
    onRemove: () => {
      setFileList([])
    },
  }

  const handleCancel = () => {
    if (!uploading) {
      setFileList([])
      form.resetFields()
      onCancel()
    }
  }

  return (
    <Modal
      title="上传文件"
      open={visible}
      onOk={handleUpload}
      onCancel={handleCancel}
      confirmLoading={uploading}
      width={600}
      okText="上传"
      cancelText="取消"
      maskClosable={!uploading}
      closable={!uploading}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <Alert
          message="支持的文件类型"
          description="支持 JPG, JPEG, PNG, GIF, PDF, DOC, DOCX, XLS, XLSX 等常见文件格式，单个文件最大100MB"
          type="info"
          showIcon
        />

        <Form form={form} layout="vertical">
          <Form.Item label="目标文件夹ID" name="folderId">
            <InputNumber
              style={{ width: '100%' }}
              placeholder="留空则上传到根目录"
              min={1}
            />
          </Form.Item>
        </Form>

        <Dragger {...uploadProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p className="ant-upload-hint">
            支持单个文件上传，文件大小不超过100MB
          </p>
        </Dragger>

        {uploading && (
          <div>
            <div style={{ marginBottom: 8 }}>
              上传进度：{uploadProgress}%
            </div>
            <Progress percent={uploadProgress} status="active" />
          </div>
        )}
      </Space>
    </Modal>
  )
}

export default FileUploadModal

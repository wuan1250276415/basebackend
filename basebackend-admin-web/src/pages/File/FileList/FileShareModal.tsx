import React, { useState } from 'react'
import {
  Modal,
  Form,
  Input,
  DatePicker,
  InputNumber,
  Switch,
  message,
  Space,
  Button,
  Alert,
} from 'antd'
import { CopyOutlined } from '@ant-design/icons'
import type { FileMetadata } from '@/types/file'
import { createFileShare } from '@/api/file'
import dayjs from 'dayjs'

interface FileShareModalProps {
  visible: boolean
  file: FileMetadata
  onCancel: () => void
}

const FileShareModal: React.FC<FileShareModalProps> = ({
  visible,
  file,
  onCancel,
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [shareInfo, setShareInfo] = useState<any>(null)

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const data = {
        fileId: file.fileId,
        sharePassword: values.sharePassword,
        expireTime: values.expireTime
          ? dayjs(values.expireTime).format('YYYY-MM-DD HH:mm:ss')
          : undefined,
        downloadLimit: values.downloadLimit,
        allowDownload: values.allowDownload ?? true,
        allowPreview: values.allowPreview ?? true,
      }

      const res = await createFileShare(data)
      setShareInfo(res.data)
      message.success('创建分享成功')
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建分享失败')
    } finally {
      setLoading(false)
    }
  }

  const copyShareLink = () => {
    if (!shareInfo) return

    const shareLink = `${window.location.origin}/share/${shareInfo.shareCode}`
    const text = shareInfo.sharePassword
      ? `分享链接：${shareLink}\n提取码：${shareInfo.sharePassword}`
      : `分享链接：${shareLink}`

    navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  }

  const handleCancel = () => {
    form.resetFields()
    setShareInfo(null)
    onCancel()
  }

  return (
    <Modal
      title="分享文件"
      open={visible}
      onCancel={handleCancel}
      footer={
        shareInfo ? (
          <Space>
            <Button onClick={handleCancel}>关闭</Button>
            <Button type="primary" icon={<CopyOutlined />} onClick={copyShareLink}>
              复制分享信息
            </Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={handleCancel}>取消</Button>
            <Button type="primary" loading={loading} onClick={handleSubmit}>
              创建分享
            </Button>
          </Space>
        )
      }
      width={600}
    >
      {shareInfo ? (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Alert
            message="分享创建成功"
            description={
              <div>
                <p>
                  分享链接：
                  <a
                    href={`/share/${shareInfo.shareCode}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {window.location.origin}/share/{shareInfo.shareCode}
                  </a>
                </p>
                {shareInfo.sharePassword && (
                  <p>提取码：<strong>{shareInfo.sharePassword}</strong></p>
                )}
                {shareInfo.expireTime && (
                  <p>有效期至：{shareInfo.expireTime}</p>
                )}
              </div>
            }
            type="success"
            showIcon
          />
        </Space>
      ) : (
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            allowDownload: true,
            allowPreview: true,
          }}
        >
          <Alert
            message={`正在分享文件：${file.originalName}`}
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <Form.Item
            label="提取码"
            name="sharePassword"
            extra="留空表示不设置提取码"
          >
            <Input placeholder="请输入提取码（可选）" maxLength={6} />
          </Form.Item>

          <Form.Item
            label="过期时间"
            name="expireTime"
            extra="留空表示永久有效"
          >
            <DatePicker
              showTime
              style={{ width: '100%' }}
              placeholder="选择过期时间"
              disabledDate={(current) => {
                return current && current < dayjs().endOf('day')
              }}
            />
          </Form.Item>

          <Form.Item
            label="下载次数限制"
            name="downloadLimit"
            extra="留空表示不限制下载次数"
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="下载次数限制"
              min={1}
            />
          </Form.Item>

          <Form.Item
            label="允许下载"
            name="allowDownload"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="允许预览"
            name="allowPreview"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      )}
    </Modal>
  )
}

export default FileShareModal

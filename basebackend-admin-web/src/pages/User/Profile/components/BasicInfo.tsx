import { useState } from 'react'
import {
  Card,
  Descriptions,
  Button,
  Space,
  Avatar,
  Tag,
  Modal,
  Form,
  Input,
  DatePicker,
  Select,
  Upload,
  message,
} from 'antd'
import { EditOutlined, UserOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'
import { ProfileDetail, UpdateProfileRequest } from '@/types'
import { updateProfile } from '@/api/profile'

interface BasicInfoProps {
  data: ProfileDetail
  onUpdate: () => void
}

const BasicInfo = ({ data, onUpdate }: BasicInfoProps) => {
  const [form] = Form.useForm()
  const [modalVisible, setModalVisible] = useState(false)
  const [loading, setLoading] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])

  // 性别显示
  const getGenderText = (gender?: number) => {
    switch (gender) {
      case 1:
        return '男'
      case 2:
        return '女'
      default:
        return '未知'
    }
  }

  // 状态显示
  const getStatusTag = (status: number) => {
    return status === 1 ? (
      <Tag color="success">正常</Tag>
    ) : (
      <Tag color="error">禁用</Tag>
    )
  }

  // 打开编辑弹窗
  const handleOpenModal = () => {
    form.setFieldsValue({
      ...data,
      birthday: data.birthday ? dayjs(data.birthday) : undefined,
    })
    setModalVisible(true)
  }

  // 提交更新
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const updateData: UpdateProfileRequest = {
        ...values,
        birthday: values.birthday ? values.birthday.format('YYYY-MM-DD') : undefined,
      }

      await updateProfile(updateData)
      message.success('更新个人资料成功')
      setModalVisible(false)
      onUpdate()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      } else {
        message.error('更新个人资料失败')
      }
    } finally {
      setLoading(false)
    }
  }

  // 头像上传配置
  const uploadProps = {
    beforeUpload: (file: File) => {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        message.error('只能上传图片文件!')
        return false
      }
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        message.error('图片大小不能超过 2MB!')
        return false
      }
      return false // 阻止自动上传
    },
    fileList,
    onChange: ({ fileList: newFileList }) => {
      setFileList(newFileList)
    },
  }

  return (
    <>
      <Card>
        <div style={{ display: 'flex', marginBottom: 24 }}>
          <Avatar
            size={100}
            src={data.avatar}
            icon={<UserOutlined />}
            style={{ marginRight: 24 }}
          />
          <div style={{ flex: 1 }}>
            <h2 style={{ marginBottom: 8 }}>{data.nickname || data.username}</h2>
            <Space>
              <Tag color="blue">{data.username}</Tag>
              {getStatusTag(data.status)}
            </Space>
          </div>
          <Button type="primary" icon={<EditOutlined />} onClick={handleOpenModal}>
            编辑资料
          </Button>
        </div>

        <Descriptions column={2} bordered>
          <Descriptions.Item label="用户ID">{data.userId}</Descriptions.Item>
          <Descriptions.Item label="用户名">{data.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{data.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label="性别">{getGenderText(data.gender)}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{data.email || '-'}</Descriptions.Item>
          <Descriptions.Item label="手机号">{data.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="生日">{data.birthday || '-'}</Descriptions.Item>
          <Descriptions.Item label="所属部门">{data.deptName || '-'}</Descriptions.Item>
          <Descriptions.Item label="最后登录IP">{data.loginIp || '-'}</Descriptions.Item>
          <Descriptions.Item label="最后登录时间">
            {data.loginTime || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">{data.createTime}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Modal
        title="编辑个人资料"
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="昵称"
            name="nickname"
            rules={[{ max: 50, message: '昵称长度不能超过50个字符' }]}
          >
            <Input placeholder="请输入昵称" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { type: 'email', message: '邮箱格式不正确' },
              { max: 100, message: '邮箱长度不能超过100个字符' },
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item label="性别" name="gender">
            <Select placeholder="请选择性别">
              <Select.Option value={0}>未知</Select.Option>
              <Select.Option value={1}>男</Select.Option>
              <Select.Option value={2}>女</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="生日" name="birthday">
            <DatePicker style={{ width: '100%' }} placeholder="请选择生日" />
          </Form.Item>

          <Form.Item label="头像" name="avatar">
            <Input placeholder="请输入头像URL" />
          </Form.Item>

          <Form.Item label="上传头像">
            <Upload {...uploadProps} maxCount={1}>
              <Button icon={<UploadOutlined />}>选择图片</Button>
            </Upload>
            <div style={{ color: '#999', marginTop: 8 }}>
              提示: 支持jpg、png格式，文件大小不超过2MB
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default BasicInfo

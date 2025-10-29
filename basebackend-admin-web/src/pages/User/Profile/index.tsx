import { useState, useEffect } from 'react'
import { Card, Tabs, message, Spin } from 'antd'
import { UserOutlined, SafetyOutlined, SettingOutlined } from '@ant-design/icons'
import { getProfile } from '@/api/profile'
import { ProfileDetail } from '@/types'
import BasicInfo from './components/BasicInfo'
import SecuritySettings from './components/SecuritySettings'
import Preferences from './components/Preferences'
import './index.css'

const Profile = () => {
  const [loading, setLoading] = useState(false)
  const [profileData, setProfileData] = useState<ProfileDetail | null>(null)

  // 加载个人资料
  const loadProfile = async () => {
    setLoading(true)
    try {
      const response = await getProfile()
      setProfileData(response.data)
    } catch (error) {
      message.error('加载个人资料失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProfile()
  }, [])

  const tabItems = [
    {
      key: 'basic',
      label: (
        <span>
          <UserOutlined />
          基本信息
        </span>
      ),
      children: profileData && <BasicInfo data={profileData} onUpdate={loadProfile} />,
    },
    {
      key: 'security',
      label: (
        <span>
          <SafetyOutlined />
          安全设置
        </span>
      ),
      children: profileData && <SecuritySettings data={profileData} />,
    },
    {
      key: 'preferences',
      label: (
        <span>
          <SettingOutlined />
          偏好设置
        </span>
      ),
      children: <Preferences />,
    },
  ]

  return (
    <div className="profile-container">
      <Card className="profile-card">
        <Spin spinning={loading}>
          <Tabs items={tabItems} defaultActiveKey="basic" />
        </Spin>
      </Card>
    </div>
  )
}

export default Profile

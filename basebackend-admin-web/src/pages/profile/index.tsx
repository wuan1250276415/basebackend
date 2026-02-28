/**
 * 个人中心页面
 * 使用 Tabs 分隔"个人信息"和"修改密码"两个区域
 * 个人信息：展示用户资料 + 编辑表单
 * 修改密码：旧密码、新密码、确认密码表单
 */
import { useEffect, useState } from 'react';
import {
  Card,
  Tabs,
  Descriptions,
  Form,
  Input,
  Button,
  Avatar,
  Spin,
  message,
  Row,
  Col,
} from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { profileApi } from '@/api/profileApi';
import type { ProfileDetail, UpdateProfileParams, ChangePasswordParams } from '@/types';

const ProfilePage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<ProfileDetail | null>(null);
  const [profileForm] = Form.useForm<UpdateProfileParams>();
  const [passwordForm] = Form.useForm<ChangePasswordParams>();
  const [profileSubmitting, setProfileSubmitting] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  /** 加载个人资料 */
  const fetchProfile = async () => {
    setLoading(true);
    try {
      const data = await profileApi.getProfile();
      setProfile(data);
      // 回填编辑表单
      profileForm.setFieldsValue({
        nickname: data.nickname,
        email: data.email,
        phone: data.phone,
      });
    } catch {
      // 错误已由全局拦截器处理
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  /** 提交个人资料更新 */
  const handleProfileSubmit = async (values: UpdateProfileParams) => {
    setProfileSubmitting(true);
    try {
      await profileApi.updateProfile(values);
      message.success('个人资料更新成功');
      // 重新加载资料以刷新展示区域
      await fetchProfile();
    } catch {
      // 错误已由全局拦截器处理
    } finally {
      setProfileSubmitting(false);
    }
  };

  /** 提交密码修改 */
  const handlePasswordSubmit = async (values: ChangePasswordParams) => {
    setPasswordSubmitting(true);
    try {
      await profileApi.changePassword(values);
      message.success('密码修改成功');
      passwordForm.resetFields();
    } catch {
      // 错误已由全局拦截器处理
    } finally {
      setPasswordSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Row gutter={24}>
        {/* 左侧：用户基本信息卡片 */}
        <Col xs={24} md={8}>
          <Card bordered={false} style={{ textAlign: 'center' }}>
            <Avatar
              size={100}
              src={profile?.avatar || undefined}
              icon={!profile?.avatar ? <UserOutlined /> : undefined}
              style={{ marginBottom: 16 }}
            />
            <h2 style={{ marginBottom: 4 }}>{profile?.nickname || '-'}</h2>
            <p style={{ color: 'rgba(0,0,0,0.45)', marginBottom: 24 }}>
              {profile?.deptName || '-'}
            </p>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="用户名">{profile?.username || '-'}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{profile?.email || '-'}</Descriptions.Item>
              <Descriptions.Item label="手机号">{profile?.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label="最后登录IP">{profile?.loginIp || '-'}</Descriptions.Item>
              <Descriptions.Item label="最后登录时间">{profile?.loginTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="注册时间">{profile?.createTime || '-'}</Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        {/* 右侧：Tabs 切换编辑资料 / 修改密码 */}
        <Col xs={24} md={16}>
          <Card bordered={false}>
            <Tabs
              defaultActiveKey="profile"
              items={[
                {
                  key: 'profile',
                  label: '个人信息',
                  children: (
                    <Form
                      form={profileForm}
                      layout="vertical"
                      onFinish={handleProfileSubmit}
                      style={{ maxWidth: 480 }}
                    >
                      <Form.Item
                        name="nickname"
                        label="昵称"
                        rules={[{ required: true, message: '请输入昵称' }]}
                      >
                        <Input prefix={<UserOutlined />} placeholder="请输入昵称" />
                      </Form.Item>
                      <Form.Item
                        name="email"
                        label="邮箱"
                        rules={[
                          { type: 'email', message: '请输入有效的邮箱地址' },
                        ]}
                      >
                        <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                      </Form.Item>
                      <Form.Item
                        name="phone"
                        label="手机号"
                        rules={[
                          { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' },
                        ]}
                      >
                        <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
                      </Form.Item>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" loading={profileSubmitting}>
                          保存修改
                        </Button>
                      </Form.Item>
                    </Form>
                  ),
                },
                {
                  key: 'password',
                  label: '修改密码',
                  children: (
                    <Form
                      form={passwordForm}
                      layout="vertical"
                      onFinish={handlePasswordSubmit}
                      style={{ maxWidth: 480 }}
                    >
                      <Form.Item
                        name="oldPassword"
                        label="旧密码"
                        rules={[{ required: true, message: '请输入旧密码' }]}
                      >
                        <Input.Password prefix={<LockOutlined />} placeholder="请输入旧密码" />
                      </Form.Item>
                      <Form.Item
                        name="newPassword"
                        label="新密码"
                        rules={[
                          { required: true, message: '请输入新密码' },
                          { min: 6, message: '密码长度不能少于6位' },
                        ]}
                      >
                        <Input.Password prefix={<LockOutlined />} placeholder="请输入新密码" />
                      </Form.Item>
                      <Form.Item
                        name="confirmPassword"
                        label="确认密码"
                        dependencies={['newPassword']}
                        rules={[
                          { required: true, message: '请确认新密码' },
                          ({ getFieldValue }) => ({
                            validator(_, value) {
                              if (!value || getFieldValue('newPassword') === value) {
                                return Promise.resolve();
                              }
                              return Promise.reject(new Error('两次输入的密码不一致'));
                            },
                          }),
                        ]}
                      >
                        <Input.Password prefix={<LockOutlined />} placeholder="请再次输入新密码" />
                      </Form.Item>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" loading={passwordSubmitting}>
                          修改密码
                        </Button>
                      </Form.Item>
                    </Form>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ProfilePage;

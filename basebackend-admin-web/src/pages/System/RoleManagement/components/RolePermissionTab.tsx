import React, { useState, useEffect } from 'react'
import {
  Collapse,
  Tree,
  Checkbox,
  Button,
  Space,
  message,
  Spin,
  Input,
  Select,
  Form,
  Card,
} from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import {
  getRoleResources,
  assignRoleResources,
  getRolePermissions,
  assignRolePermissions,
  getRoleListOperations,
  configureListOperations,
  configureDataPermissions,
} from '@/api/role'
import request from '@/utils/request'

const { Panel } = Collapse
const { TextArea } = Input
const { Option } = Select

interface RolePermissionTabProps {
  roleId: string
  appId?: string
}

const RolePermissionTab: React.FC<RolePermissionTabProps> = ({ roleId, appId }) => {
  const [loading, setLoading] = useState(false)
  const [activeKey, setActiveKey] = useState<string[]>(['menu'])

  // 菜单权限
  const [menuTreeData, setMenuTreeData] = useState<DataNode[]>([])
  const [checkedMenuKeys, setCheckedMenuKeys] = useState<React.Key[]>([])
  const [menuLoading, setMenuLoading] = useState(false)

  // 按钮权限
  const [buttonList, setButtonList] = useState<any[]>([])
  const [checkedButtonKeys, setCheckedButtonKeys] = useState<string[]>([])
  const [buttonLoading, setButtonLoading] = useState(false)

  // 列表权限
  const [operationList, setOperationList] = useState<any[]>([])
  const [checkedOperationKeys, setCheckedOperationKeys] = useState<string[]>([])
  const [operationLoading, setOperationLoading] = useState(false)
  const [resourceType, setResourceType] = useState<string>('user')

  // 数据权限
  const [dataPermissionForm] = Form.useForm()
  const [dataPermissionLoading, setDataPermissionLoading] = useState(false)

  // 加载菜单权限
  const loadMenuPermissions = async () => {
    if (!roleId) return

    setMenuLoading(true)
    try {
      // 获取应用资源树
      const resourceRes = await request.get('/basebackend-system-api/api/system/application/resource/tree/'+appId)

      if (resourceRes.code === 200) {
        const resources = resourceRes.data || []
        const treeData = convertToTreeData(resources)
        setMenuTreeData(treeData)
      }

      // 获取角色已选资源
      const roleRes = await getRoleResources(roleId)
      if (roleRes.code === 200) {
        setCheckedMenuKeys(roleRes.data || [])
      }
    } catch (error) {
      message.error('加载菜单权限失败')
      console.error(error)
    } finally {
      setMenuLoading(false)
    }
  }

  // 转换为树形数据
  const convertToTreeData = (resources: any[]): DataNode[] => {
    return resources.map((res: any) => ({
      key: res.id,
      title: res.resourceName,
      children: res.children ? convertToTreeData(res.children) : undefined,
    }))
  }

  // 加载按钮权限
  const loadButtonPermissions = async () => {
    if (!roleId) return

    setButtonLoading(true)
    try {
      // 获取所有按钮权限
      const permRes = await request.get('/basebackend-system-api/api/system/permissions', {
        params: { permissionType: 3 }, // 3-API权限
      })

      if (permRes.code === 200) {
        setButtonList(permRes.data || [])
      }

      // 获取角色已选权限
      const roleRes = await getRolePermissions(roleId)
      if (roleRes.code === 200) {
        setCheckedButtonKeys(roleRes.data || [])
      }
    } catch (error) {
      message.error('加载按钮权限失败')
      console.error(error)
    } finally {
      setButtonLoading(false)
    }
  }

  // 加载列表操作权限
  const loadListOperations = async (resType: string) => {
    if (!roleId) return

    setOperationLoading(true)
    try {
      // 获取所有列表操作
      const opRes = await request.get('/basebackend-system-api/api/admin/list-operations')
      if (opRes.code === 200) {
        setOperationList(opRes.data || [])
      }

      // 获取角色已选操作
      const roleRes = await getRoleListOperations(roleId, resType)
      if (roleRes.code === 200) {
        setCheckedOperationKeys(roleRes.data || [])
      }
    } catch (error) {
      message.error('加载列表权限失败')
      console.error(error)
    } finally {
      setOperationLoading(false)
    }
  }

  useEffect(() => {
    if (roleId) {
      if (activeKey.includes('menu')) {
        loadMenuPermissions()
      }
      if (activeKey.includes('button')) {
        loadButtonPermissions()
      }
      if (activeKey.includes('list')) {
        loadListOperations(resourceType)
      }
    }
  }, [roleId, appId, activeKey, resourceType])

  // 保存菜单权限
  const handleSaveMenuPermissions = async () => {
    setLoading(true)
    try {
      await assignRoleResources(roleId, checkedMenuKeys as string[])
      message.success('菜单权限保存成功')
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败')
    } finally {
      setLoading(false)
    }
  }

  // 保存按钮权限
  const handleSaveButtonPermissions = async () => {
    setLoading(true)
    try {
      await assignRolePermissions(roleId, checkedButtonKeys)
      message.success('按钮权限保存成功')
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败')
    } finally {
      setLoading(false)
    }
  }

  // 保存列表操作权限
  const handleSaveListOperations = async () => {
    setLoading(true)
    try {
      await configureListOperations(roleId, {
        resourceType,
        operationIds: checkedOperationKeys,
      })
      message.success('列表权限保存成功')
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败')
    } finally {
      setLoading(false)
    }
  }

  // 保存数据权限
  const handleSaveDataPermissions = async () => {
    try {
      const values = await dataPermissionForm.validateFields()
      setDataPermissionLoading(true)

      await configureDataPermissions(roleId, JSON.stringify(values))
      message.success('数据权限保存成功')
    } catch (error: any) {
      if (error.response) {
        message.error(error.response?.data?.message || '保存失败')
      }
    } finally {
      setDataPermissionLoading(false)
    }
  }

  return (
    <Spin spinning={loading}>
      <Collapse
        activeKey={activeKey}
        onChange={(keys) => setActiveKey(keys as string[])}
      >
        {/* 菜单权限 */}
        <Panel header="菜单权限" key="menu">
          <Spin spinning={menuLoading}>
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Tree
                checkable
                treeData={menuTreeData}
                checkedKeys={checkedMenuKeys}
                onCheck={(checkedKeys: any) => setCheckedMenuKeys(checkedKeys)}
                height={400}
              />
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSaveMenuPermissions}
              >
                保存菜单权限
              </Button>
            </Space>
          </Spin>
        </Panel>

        {/* 按钮权限 */}
        <Panel header="按钮权限" key="button">
          <Spin spinning={buttonLoading}>
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Checkbox.Group
                value={checkedButtonKeys}
                onChange={(checkedValues) =>
                  setCheckedButtonKeys(checkedValues as string[])
                }
                style={{ width: '100%' }}
              >
                <Space direction="vertical" style={{ width: '100%' }}>
                  {buttonList.map((btn) => (
                    <Card key={btn.id} size="small" style={{ width: '100%' }}>
                      <Checkbox value={btn.id}>
                        <strong>{btn.permissionName}</strong>
                        <span style={{ marginLeft: 8, color: '#999' }}>
                          ({btn.permissionKey})
                        </span>
                      </Checkbox>
                    </Card>
                  ))}
                </Space>
              </Checkbox.Group>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSaveButtonPermissions}
              >
                保存按钮权限
              </Button>
            </Space>
          </Spin>
        </Panel>

        {/* 列表权限 */}
        <Panel header="列表权限" key="list">
          <Spin spinning={operationLoading}>
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Space>
                <span>资源类型：</span>
                <Select
                  value={resourceType}
                  onChange={(value) => {
                    setResourceType(value)
                    loadListOperations(value)
                  }}
                  style={{ width: 200 }}
                >
                  <Option value="user">用户</Option>
                  <Option value="role">角色</Option>
                  <Option value="menu">菜单</Option>
                  <Option value="dept">部门</Option>
                </Select>
              </Space>

              <Checkbox.Group
                value={checkedOperationKeys}
                onChange={(checkedValues) =>
                  setCheckedOperationKeys(checkedValues as string[])
                }
                style={{ width: '100%' }}
              >
                <Space direction="vertical" style={{ width: '100%' }}>
                  {operationList.map((op) => (
                    <Card key={op.id} size="small" style={{ width: '100%' }}>
                      <Checkbox value={op.id}>
                        <strong>{op.operationName}</strong>
                        <span style={{ marginLeft: 8, color: '#999' }}>
                          ({op.operationType})
                        </span>
                      </Checkbox>
                    </Card>
                  ))}
                </Space>
              </Checkbox.Group>

              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSaveListOperations}
              >
                保存列表权限
              </Button>
            </Space>
          </Spin>
        </Panel>

        {/* 数据权限 */}
        <Panel header="数据权限" key="data">
          <Spin spinning={dataPermissionLoading}>
            <Form form={dataPermissionForm} layout="vertical">
              <Form.Item
                label="部门ID列表"
                name="deptIds"
                help="多个部门ID用逗号分隔，如：1,2,3"
              >
                <Input placeholder="请输入部门ID列表" />
              </Form.Item>

              <Form.Item
                label="自定义过滤规则"
                name="customRule"
                help="支持变量：{{currentUserId}}, {{currentDeptId}}"
              >
                <TextArea
                  rows={4}
                  placeholder='示例：createBy = {{currentUserId}}'
                />
              </Form.Item>

              <Form.Item label="数据范围" name="scope">
                <Select placeholder="请选择数据范围">
                  <Option value="all">全部数据</Option>
                  <Option value="dept">本部门数据</Option>
                  <Option value="deptAndSub">本部门及以下数据</Option>
                  <Option value="self">仅本人数据</Option>
                </Select>
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSaveDataPermissions}
                >
                  保存数据权限
                </Button>
              </Form.Item>
            </Form>
          </Spin>
        </Panel>
      </Collapse>
    </Spin>
  )
}

export default RolePermissionTab

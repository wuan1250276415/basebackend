import React, { useState, useEffect } from 'react'
import { Card, Form, Button, message } from 'antd'
import { getDeptTree } from '@/api/dept'
import { Dept } from '@/types'
import DeptTreeSelect from './DeptTreeSelect'

const DeptTreeSelectTest: React.FC = () => {
  const [form] = Form.useForm()
  const [deptList, setDeptList] = useState<Dept[]>([])

  // 加载部门数据
  const loadDeptList = async () => {
    try {
      const response = await getDeptTree()
      setDeptList(response.data)
    } catch (error) {
      console.error('加载部门列表失败', error)
    }
  }

  useEffect(() => {
    loadDeptList()
  }, [])

  const handleSubmit = (values: any) => {
    console.log('选择的部门:', values)
    message.success(`选择的部门ID: ${values.deptId}`)
  }

  return (
    <Card title="部门树形选择测试" style={{ margin: 20 }}>
      <Form form={form} onFinish={handleSubmit} layout="vertical">
        <Form.Item
          name="deptId"
          label="选择部门"
          rules={[{ required: true, message: '请选择部门' }]}
        >
          <DeptTreeSelect 
            placeholder="请选择部门" 
            treeData={deptList}
          />
        </Form.Item>
        
        <Form.Item>
          <Button type="primary" htmlType="submit">
            提交测试
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}

export default DeptTreeSelectTest

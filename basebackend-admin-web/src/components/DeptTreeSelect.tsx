import React from 'react'
import { TreeSelect } from 'antd'
import { Dept } from '@/types'

interface DeptTreeSelectProps {
  value?: string
  onChange?: (value: string) => void
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  treeData?: Dept[]
}

interface DeptTreeSelectComponent extends React.FC<DeptTreeSelectProps> {
  TreeNode: typeof TreeSelect.TreeNode
}

const DeptTreeSelect: DeptTreeSelectComponent = ({
  value,
  onChange,
  placeholder = '请选择部门',
  disabled = false,
  allowClear = true,
  treeData = [],
}) => {
  // 将部门树形数据转换为TreeSelect需要的格式
  const convertDeptToTreeData = (depts: Dept[], level = 0): any[] => {
    return depts.map(dept => ({
      title: dept.deptName,
      value: dept.id,
      key: dept.id,
      children: dept.children ? convertDeptToTreeData(dept.children, level + 1) : undefined,
    }))
  }

  const treeSelectData = convertDeptToTreeData(treeData)

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      allowClear={allowClear}
      treeData={treeSelectData}
      showSearch
      treeNodeFilterProp="title"
      dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
      style={{ width: '100%' }}
    />
  )
}

// 为了保持API兼容性，添加TreeNode属性
DeptTreeSelect.TreeNode = TreeSelect.TreeNode

export default DeptTreeSelect

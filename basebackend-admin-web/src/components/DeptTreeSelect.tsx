import React, { useEffect, useState } from 'react';
import { TreeSelect, type TreeSelectProps } from 'antd';
import { deptApi } from '@/api/deptApi';
import type { DeptDTO } from '@/types';

/**
 * 部门树选择器组件
 *
 * 从后端获取部门树数据，渲染为 Ant Design TreeSelect，
 * 支持单选和搜索过滤。用于用户管理（选择所属部门）和部门管理页面。
 *
 * @example
 * <DeptTreeSelect value={deptId} onChange={(id) => setDeptId(id)} />
 */

/** 组件属性，继承 TreeSelect 常用属性 */
interface DeptTreeSelectProps
  extends Omit<TreeSelectProps, 'treeData' | 'loading' | 'showSearch' | 'treeNodeFilterProp'> {
  /** 当前选中的部门 ID */
  value?: number;
  /** 选中部门时的回调 */
  onChange?: (value: number) => void;
}

/** 将 DeptDTO 树转换为 TreeSelect 的 treeData 格式 */
function transformToTreeData(
  depts: DeptDTO[],
): TreeSelectProps['treeData'] {
  return depts.map((dept) => ({
    title: dept.deptName,
    value: dept.id,
    children: dept.children?.length ? transformToTreeData(dept.children) : undefined,
  }));
}

const DeptTreeSelect: React.FC<DeptTreeSelectProps> = ({ value, onChange, ...restProps }) => {
  const [treeData, setTreeData] = useState<TreeSelectProps['treeData']>([]);
  const [loading, setLoading] = useState(false);

  /** 组件挂载时获取部门树数据 */
  useEffect(() => {
    setLoading(true);
    deptApi
      .tree()
      .then((data) => {
        setTreeData(transformToTreeData(data));
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      treeData={treeData}
      loading={loading}
      showSearch
      treeNodeFilterProp="title"
      placeholder="请选择部门"
      allowClear
      treeDefaultExpandAll
      style={{ width: '100%' }}
      {...restProps}
    />
  );
};

export default DeptTreeSelect;

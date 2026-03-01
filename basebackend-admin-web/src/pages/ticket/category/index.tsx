import { useRef, useEffect, useState } from 'react';
import { Button, Modal, Form, Input, InputNumber, TreeSelect, Space, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ticketApi } from '@/api/ticketApi';
import type { TicketCategoryTree } from '@/api/ticketApi';
import { Permission } from '@/components/Permission';

function flattenTree(tree: TicketCategoryTree[]): TicketCategoryTree[] {
  const result: TicketCategoryTree[] = [];
  const walk = (nodes: TicketCategoryTree[]) => {
    for (const node of nodes) {
      result.push(node);
      if (node.children?.length) walk(node.children);
    }
  };
  walk(tree);
  return result;
}

function toTreeSelectData(tree: TicketCategoryTree[]): { title: string; value: number; children?: any[] }[] {
  return tree.map((node) => ({
    title: node.name,
    value: node.id,
    children: node.children?.length ? toTreeSelectData(node.children) : undefined,
  }));
}

const CategoryPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [categories, setCategories] = useState<TicketCategoryTree[]>([]);

  const fetchCategories = async () => {
    const data = await ticketApi.getCategoryTree();
    setCategories(data);
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const columns: ProColumns<TicketCategoryTree>[] = [
    { title: '分类名称', dataIndex: 'name', width: 200 },
    { title: '图标', dataIndex: 'icon', width: 80, hideInSearch: true },
    { title: 'SLA (小时)', dataIndex: 'slaHours', width: 100, hideInSearch: true },
    { title: '排序', dataIndex: 'sortOrder', width: 80, hideInSearch: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      hideInSearch: true,
      valueEnum: { 0: { text: '禁用', status: 'Error' }, 1: { text: '启用', status: 'Success' } },
    },
    { title: '描述', dataIndex: 'description', ellipsis: true, hideInSearch: true },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Permission code="ticket:category:edit">
            <a onClick={() => handleEdit(record)}><EditOutlined /> 编辑</a>
          </Permission>
          <Permission code="ticket:category:delete">
            <a
              style={{ color: '#ff4d4f' }}
              onClick={() => {
                Modal.confirm({
                  title: '确认删除',
                  content: `确认删除分类「${record.name}」？`,
                  onOk: async () => {
                    await ticketApi.deleteCategory(record.id);
                    message.success('删除成功');
                    fetchCategories();
                  },
                });
              }}
            >
              <DeleteOutlined />
            </a>
          </Permission>
        </Space>
      ),
    },
  ];

  const handleAdd = () => {
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: TicketCategoryTree) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingId) {
      await ticketApi.updateCategory(editingId, values);
      message.success('更新成功');
    } else {
      await ticketApi.createCategory(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    fetchCategories();
  };

  return (
    <>
      <ProTable<TicketCategoryTree>
        headerTitle="工单分类管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        dataSource={flattenTree(categories)}
        search={false}
        pagination={false}
        toolBarRender={() => [
          <Permission key="add" code="ticket:category:create">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增分类
            </Button>
          </Permission>,
        ]}
      />

      <Modal
        title={editingId ? '编辑分类' : '新增分类'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ sortOrder: 0, slaHours: 24, status: 1 }}>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input placeholder="请输入分类名称" maxLength={50} />
          </Form.Item>
          <Form.Item name="parentId" label="上级分类">
            <TreeSelect
              placeholder="无上级（顶级分类）"
              treeData={toTreeSelectData(categories)}
              treeDefaultExpandAll
              allowClear
            />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="图标标识" />
          </Form.Item>
          <Form.Item name="slaHours" label="SLA (小时)">
            <InputNumber min={1} max={720} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="分类描述" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default CategoryPage;

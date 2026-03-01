import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  TreeSelect,
  Space,
  Steps,
  Descriptions,
  Tag,
  Upload,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  InboxOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd';
import { ticketApi } from '@/api/ticketApi';
import type { TicketCreateDTO, TicketCategoryTree } from '@/api/ticketApi';
import { uploadFile } from '@/api/file';
import PriorityBadge from '@/components/ticket/PriorityBadge';

const { Dragger } = Upload;

const PRIORITY_OPTIONS = [
  { label: '紧急', value: 1 },
  { label: '高', value: 2 },
  { label: '中', value: 3 },
  { label: '低', value: 4 },
  { label: '最低', value: 5 },
];

function buildCategoryTreeData(tree: TicketCategoryTree[]): { title: string; value: number; children?: any[] }[] {
  return tree.map((node) => ({
    title: node.name,
    value: node.id,
    children: node.children?.length ? buildCategoryTreeData(node.children) : undefined,
  }));
}

function findCategoryName(tree: TicketCategoryTree[], id: number): string {
  for (const node of tree) {
    if (node.id === id) return node.name;
    if (node.children?.length) {
      const found = findCategoryName(node.children, id);
      if (found) return found;
    }
  }
  return '';
}

const TicketCreatePage: React.FC = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [current, setCurrent] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [categories, setCategories] = useState<TicketCategoryTree[]>([]);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [uploadedAttachments, setUploadedAttachments] = useState<
    { fileId: number; fileName: string; fileSize: number; fileType: string; fileUrl: string }[]
  >([]);

  useEffect(() => {
    ticketApi.getCategoryTree().then(setCategories);
  }, []);

  const steps = [
    { title: '基本信息', description: '标题、分类、优先级' },
    { title: '描述与附件', description: '详细描述和相关文件' },
    { title: '确认提交', description: '核对信息并提交' },
  ];

  const validateStep = async (): Promise<boolean> => {
    try {
      if (current === 0) {
        await form.validateFields(['title', 'categoryId', 'priority']);
      } else if (current === 1) {
        await form.validateFields(['description', 'tags']);
      }
      return true;
    } catch {
      return false;
    }
  };

  const handleNext = async () => {
    const valid = await validateStep();
    if (valid) setCurrent(current + 1);
  };

  const handlePrev = () => {
    setCurrent(current - 1);
  };

  const handleUpload = async (file: File): Promise<boolean> => {
    try {
      const result: any = await uploadFile(file);
      const meta = result?.data ?? result;
      setUploadedAttachments((prev) => [
        ...prev,
        {
          fileId: meta.id,
          fileName: meta.originalName || file.name,
          fileSize: meta.fileSize || file.size,
          fileType: meta.contentType || file.type,
          fileUrl: meta.url || '',
        },
      ]);
      message.success(`${file.name} 上传成功`);
      return true;
    } catch {
      message.error(`${file.name} 上传失败`);
      return false;
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      // 获取幂等 token
      const token = await ticketApi.getIdempotentToken();

      const values = form.getFieldsValue(true);
      const dto: TicketCreateDTO = {
        title: values.title,
        description: values.description,
        categoryId: values.categoryId,
        priority: values.priority,
        tags: values.tags,
        attachmentIds: uploadedAttachments.map((a) => a.fileId),
      };

      const ticket = await ticketApi.createWithToken(dto, token);
      message.success('工单创建成功');
      navigate(`/ticket/detail?id=${ticket.id}`);
    } catch {
      // 错误已由 request 拦截器处理
    } finally {
      setSubmitting(false);
    }
  };

  const formValues = form.getFieldsValue(true);

  const renderStepContent = () => {
    switch (current) {
      case 0:
        return (
          <>
            <Form.Item
              name="title"
              label="标题"
              rules={[{ required: true, message: '请输入工单标题' }]}
            >
              <Input placeholder="请输入工单标题" maxLength={200} showCount />
            </Form.Item>

            <Form.Item
              name="categoryId"
              label="分类"
              rules={[{ required: true, message: '请选择分类' }]}
            >
              <TreeSelect
                placeholder="请选择工单分类"
                treeData={buildCategoryTreeData(categories)}
                treeDefaultExpandAll
                allowClear
              />
            </Form.Item>

            <Form.Item name="priority" label="优先级">
              <Select options={PRIORITY_OPTIONS} />
            </Form.Item>
          </>
        );

      case 1:
        return (
          <>
            <Form.Item name="description" label="描述">
              <Input.TextArea
                rows={6}
                placeholder="请描述问题详情..."
                maxLength={5000}
                showCount
              />
            </Form.Item>

            <Form.Item name="tags" label="标签">
              <Input placeholder="多个标签用逗号分隔" />
            </Form.Item>

            <Form.Item label="附件">
              <Dragger
                multiple
                fileList={fileList}
                beforeUpload={(file) => {
                  const maxSize = 10 * 1024 * 1024;
                  if (file.size > maxSize) {
                    message.error('单个文件不能超过 10MB');
                    return Upload.LIST_IGNORE;
                  }
                  setFileList((prev) => [...prev, file as any]);
                  handleUpload(file as File);
                  return false;
                }}
                onRemove={(file) => {
                  setFileList((prev) => prev.filter((f) => f.uid !== file.uid));
                  setUploadedAttachments((prev) =>
                    prev.filter((a) => a.fileName !== file.name),
                  );
                }}
              >
                <p className="ant-upload-drag-icon">
                  <InboxOutlined />
                </p>
                <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
                <p className="ant-upload-hint">
                  支持常见文件格式，单个文件最大 10MB
                </p>
              </Dragger>
            </Form.Item>
          </>
        );

      case 2:
        return (
          <Descriptions column={1} bordered size="middle">
            <Descriptions.Item label="标题">
              {formValues.title}
            </Descriptions.Item>
            <Descriptions.Item label="分类">
              {findCategoryName(categories, formValues.categoryId)}
            </Descriptions.Item>
            <Descriptions.Item label="优先级">
              <PriorityBadge priority={formValues.priority} />
            </Descriptions.Item>
            <Descriptions.Item label="描述">
              {formValues.description || <span style={{ color: '#999' }}>无</span>}
            </Descriptions.Item>
            <Descriptions.Item label="标签">
              {formValues.tags ? (
                formValues.tags.split(',').map((tag: string) => (
                  <Tag key={tag.trim()}>{tag.trim()}</Tag>
                ))
              ) : (
                <span style={{ color: '#999' }}>无</span>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="附件">
              {uploadedAttachments.length > 0 ? (
                uploadedAttachments.map((a) => (
                  <Tag key={a.fileId}>{a.fileName}</Tag>
                ))
              ) : (
                <span style={{ color: '#999' }}>无</span>
              )}
            </Descriptions.Item>
          </Descriptions>
        );

      default:
        return null;
    }
  };

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/ticket')}>
          返回列表
        </Button>
      </Space>

      <Card title="创建工单">
        <Steps current={current} items={steps} style={{ marginBottom: 32 }} />

        <Form
          form={form}
          layout="vertical"
          initialValues={{ priority: 3 }}
          style={{ maxWidth: 800 }}
        >
          {renderStepContent()}
        </Form>

        <div style={{ marginTop: 24, maxWidth: 800 }}>
          <Space>
            {current > 0 && (
              <Button onClick={handlePrev}>上一步</Button>
            )}
            {current < steps.length - 1 && (
              <Button type="primary" onClick={handleNext}>
                下一步
              </Button>
            )}
            {current === steps.length - 1 && (
              <Button type="primary" loading={submitting} onClick={handleSubmit}>
                提交工单
              </Button>
            )}
            <Button onClick={() => navigate('/ticket')}>取消</Button>
          </Space>
        </div>
      </Card>
    </div>
  );
};

export default TicketCreatePage;

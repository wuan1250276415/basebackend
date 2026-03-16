import { useState } from 'react';
import { Drawer, Tabs, Button, Input, Card, Progress, Typography, Space, List, message, Spin, Tag } from 'antd';
import { RobotOutlined, CopyOutlined, BulbOutlined, FileTextOutlined, TagsOutlined } from '@ant-design/icons';
import { ticketApi } from '@/api/ticketApi';
import type { TicketClassifyResult } from '@/api/ticketApi';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface AiAssistantPanelProps {
  open: boolean;
  onClose: () => void;
  ticketId?: number;
  onCategorySelect?: (categoryId: number, categoryName: string) => void;
}

const AiAssistantPanel: React.FC<AiAssistantPanelProps> = ({ open, onClose, ticketId, onCategorySelect }) => {
  // Classify state
  const [classifyTitle, setClassifyTitle] = useState('');
  const [classifyDesc, setClassifyDesc] = useState('');
  const [classifyLoading, setClassifyLoading] = useState(false);
  const [classifyResult, setClassifyResult] = useState<TicketClassifyResult | null>(null);

  // Summarize state
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summary, setSummary] = useState('');

  // Suggest reply state
  const [replyLoading, setReplyLoading] = useState(false);
  const [replies, setReplies] = useState<string[]>([]);

  const handleClassify = async () => {
    if (!classifyTitle.trim()) {
      message.warning('请输入工单标题');
      return;
    }
    setClassifyLoading(true);
    try {
      const result = await ticketApi.aiClassify(classifyTitle, classifyDesc || undefined);
      setClassifyResult(result);
    } catch {
      // handled by interceptor
    } finally {
      setClassifyLoading(false);
    }
  };

  const handleSummarize = async () => {
    if (!ticketId) {
      message.warning('请在工单详情页使用此功能');
      return;
    }
    setSummaryLoading(true);
    try {
      const result = await ticketApi.aiSummarize(ticketId);
      setSummary(result);
    } catch {
      // handled by interceptor
    } finally {
      setSummaryLoading(false);
    }
  };

  const handleSuggestReply = async () => {
    if (!ticketId) {
      message.warning('请在工单详情页使用此功能');
      return;
    }
    setReplyLoading(true);
    try {
      const result = await ticketApi.aiSuggestReply(ticketId);
      setReplies(result);
    } catch {
      // handled by interceptor
    } finally {
      setReplyLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('已复制到剪贴板');
  };

  const confidenceColor = (c: number) => {
    if (c >= 0.8) return '#52c41a';
    if (c >= 0.5) return '#fa8c16';
    return '#ff4d4f';
  };

  const tabItems = [
    {
      key: 'classify',
      label: <span><TagsOutlined /> 自动分类</span>,
      children: (
        <div>
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <div>
              <Text strong>工单标题</Text>
              <Input
                placeholder="输入工单标题"
                value={classifyTitle}
                onChange={(e) => setClassifyTitle(e.target.value)}
                style={{ marginTop: 4 }}
              />
            </div>
            <div>
              <Text strong>工单描述（可选）</Text>
              <TextArea
                placeholder="输入工单描述，可提高分类准确度"
                value={classifyDesc}
                onChange={(e) => setClassifyDesc(e.target.value)}
                rows={3}
                style={{ marginTop: 4 }}
              />
            </div>
            <Button type="primary" icon={<RobotOutlined />} loading={classifyLoading} onClick={handleClassify} block>
              AI 自动分类
            </Button>

            {classifyResult && (
              <Card size="small" style={{ marginTop: 8 }}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Text strong>推荐分类: {classifyResult.categoryName || '未知'}</Text>
                    {onCategorySelect && classifyResult.categoryId && (
                      <Button
                        size="small"
                        type="primary"
                        onClick={() => onCategorySelect(classifyResult.categoryId, classifyResult.categoryName)}
                      >
                        应用
                      </Button>
                    )}
                  </div>
                  <div>
                    <Text type="secondary">置信度: </Text>
                    <Progress
                      percent={Math.round((classifyResult.confidence ?? 0) * 100)}
                      size="small"
                      strokeColor={confidenceColor(classifyResult.confidence ?? 0)}
                      style={{ width: 150, display: 'inline-flex' }}
                    />
                  </div>
                  {classifyResult.reasoning && (
                    <div>
                      <Text type="secondary">推理: {classifyResult.reasoning}</Text>
                    </div>
                  )}
                </Space>
              </Card>
            )}
          </Space>
        </div>
      ),
    },
    {
      key: 'summarize',
      label: <span><FileTextOutlined /> 工单摘要</span>,
      children: (
        <div>
          {!ticketId ? (
            <Text type="secondary">请在工单详情页使用此功能</Text>
          ) : (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" icon={<RobotOutlined />} loading={summaryLoading} onClick={handleSummarize} block>
                生成工单摘要
              </Button>
              {summary && (
                <Card size="small">
                  <Paragraph style={{ marginBottom: 0 }}>{summary}</Paragraph>
                  <div style={{ textAlign: 'right', marginTop: 8 }}>
                    <Button size="small" icon={<CopyOutlined />} onClick={() => copyToClipboard(summary)}>
                      复制
                    </Button>
                  </div>
                </Card>
              )}
              {summaryLoading && <Spin style={{ display: 'flex', justifyContent: 'center', padding: 20 }} />}
            </Space>
          )}
        </div>
      ),
    },
    {
      key: 'reply',
      label: <span><BulbOutlined /> 推荐回复</span>,
      children: (
        <div>
          {!ticketId ? (
            <Text type="secondary">请在工单详情页使用此功能</Text>
          ) : (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" icon={<RobotOutlined />} loading={replyLoading} onClick={handleSuggestReply} block>
                生成推荐回复
              </Button>
              {replies.length > 0 && (
                <List
                  size="small"
                  dataSource={replies}
                  renderItem={(item, index) => (
                    <List.Item
                      extra={
                        <Button size="small" icon={<CopyOutlined />} onClick={() => copyToClipboard(item)}>
                          复制
                        </Button>
                      }
                    >
                      <div>
                        <Tag color="blue">方案 {index + 1}</Tag>
                        <Text style={{ fontSize: 13 }}>{item}</Text>
                      </div>
                    </List.Item>
                  )}
                />
              )}
              {replyLoading && <Spin style={{ display: 'flex', justifyContent: 'center', padding: 20 }} />}
            </Space>
          )}
        </div>
      ),
    },
  ];

  return (
    <Drawer
      title={<Space><RobotOutlined /> AI 智能助手</Space>}
      open={open}
      onClose={onClose}
      width={420}
      destroyOnClose
    >
      <Tabs items={tabItems} size="small" />
    </Drawer>
  );
};

export default AiAssistantPanel;

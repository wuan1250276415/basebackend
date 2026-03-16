import { useCallback, useEffect, useRef, useState } from 'react';
import { Card, Input, Space, Tag, List, Empty, Spin, Typography, Checkbox, Divider } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { ticketApi } from '@/api/ticketApi';
import type { TicketSearchDoc, SearchResult, TicketQueryDTO } from '@/api/ticketApi';
import TicketStatusTag from '@/components/ticket/TicketStatusTag';
import PriorityBadge from '@/components/ticket/PriorityBadge';

const { Text, Title } = Typography;

const STATUS_OPTIONS = [
  { label: '待处理', value: 'OPEN' },
  { label: '处理中', value: 'IN_PROGRESS' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
];

const PRIORITY_OPTIONS = [
  { label: '紧急', value: 1 },
  { label: '高', value: 2 },
  { label: '中', value: 3 },
  { label: '低', value: 4 },
];

const SearchPage: React.FC = () => {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SearchResult<TicketSearchDoc> | null>(null);
  const [page, setPage] = useState(1);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
  const [selectedPriority, setSelectedPriority] = useState<number | undefined>();
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>();

  const doSearch = useCallback(async (kw: string, pg: number, filters: Partial<TicketQueryDTO> = {}) => {
    if (!kw.trim()) {
      setResult(null);
      return;
    }
    setLoading(true);
    try {
      const res = await ticketApi.searchTickets(kw, filters, pg, 10);
      setResult(res);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, []);

  const handleSearch = useCallback((value: string) => {
    setPage(1);
    const filters: Partial<TicketQueryDTO> = {};
    if (selectedStatuses.length === 1) filters.status = selectedStatuses[0];
    if (selectedPriority) filters.priority = selectedPriority;
    doSearch(value, 1, filters);
  }, [doSearch, selectedStatuses, selectedPriority]);

  const handleInputChange = (value: string) => {
    setKeyword(value);
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => handleSearch(value), 300);
  };

  useEffect(() => {
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, []);

  useEffect(() => {
    if (keyword.trim()) handleSearch(keyword);
  }, [selectedStatuses, selectedPriority]); // eslint-disable-line react-hooks/exhaustive-deps

  const renderHighlight = (text: string, highlights?: string[]) => {
    if (!highlights || highlights.length === 0) return text;
    return <span dangerouslySetInnerHTML={{ __html: highlights[0] }} />;
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>工单搜索</Title>

      <Card style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="输入关键词搜索工单标题、描述..."
          size="large"
          prefix={<SearchOutlined />}
          value={keyword}
          onChange={(e) => handleInputChange(e.target.value)}
          onSearch={handleSearch}
          allowClear
          enterButton="搜索"
        />
      </Card>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* Facet Filters */}
        <Card size="small" style={{ width: 220, flexShrink: 0 }} title="筛选条件">
          <div style={{ marginBottom: 12 }}>
            <Text strong>状态</Text>
            <Checkbox.Group
              options={STATUS_OPTIONS}
              value={selectedStatuses}
              onChange={(vals) => setSelectedStatuses(vals as string[])}
              style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 8 }}
            />
          </div>
          <Divider style={{ margin: '8px 0' }} />
          <div>
            <Text strong>优先级</Text>
            <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 4 }}>
              {PRIORITY_OPTIONS.map((opt) => (
                <Tag.CheckableTag
                  key={opt.value}
                  checked={selectedPriority === opt.value}
                  onChange={(checked) => setSelectedPriority(checked ? opt.value : undefined)}
                >
                  {opt.label}
                </Tag.CheckableTag>
              ))}
            </div>
          </div>
        </Card>

        {/* Search Results */}
        <div style={{ flex: 1 }}>
          {loading && <Spin spinning style={{ display: 'flex', justifyContent: 'center', padding: 40 }} />}

          {!loading && result && (
            <>
              <Text type="secondary" style={{ marginBottom: 12, display: 'block' }}>
                找到 {result.totalHits} 条结果（耗时 {result.tookMs}ms）
              </Text>
              <List
                dataSource={result.hits}
                pagination={{
                  current: page,
                  pageSize: 10,
                  total: result.totalHits,
                  onChange: (pg) => {
                    setPage(pg);
                    doSearch(keyword, pg);
                  },
                }}
                renderItem={(hit) => (
                  <Card
                    size="small"
                    hoverable
                    style={{ marginBottom: 8 }}
                    onClick={() => navigate(`/ticket/detail?id=${hit.source.id}`)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Space>
                        <Text type="secondary">{hit.source.ticketNo}</Text>
                        <TicketStatusTag status={hit.source.status} />
                        <PriorityBadge priority={hit.source.priority} />
                      </Space>
                      <Text type="secondary" style={{ fontSize: 12 }}>{hit.source.createTime}</Text>
                    </div>
                    <div style={{ marginBottom: 4 }}>
                      <Text strong style={{ fontSize: 14 }}>
                        {renderHighlight(hit.source.title, hit.highlights?.title)}
                      </Text>
                    </div>
                    {hit.highlights?.description && (
                      <div>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {renderHighlight(hit.source.description || '', hit.highlights.description)}
                        </Text>
                      </div>
                    )}
                    <div style={{ marginTop: 4 }}>
                      <Space size={16}>
                        {hit.source.assigneeName && <Text type="secondary" style={{ fontSize: 12 }}>处理人: {hit.source.assigneeName}</Text>}
                        {hit.source.categoryName && <Text type="secondary" style={{ fontSize: 12 }}>分类: {hit.source.categoryName}</Text>}
                      </Space>
                    </div>
                  </Card>
                )}
              />
            </>
          )}

          {!loading && !result && keyword === '' && (
            <Empty description="输入关键词开始搜索" style={{ padding: 60 }} />
          )}
        </div>
      </div>
    </div>
  );
};

export default SearchPage;

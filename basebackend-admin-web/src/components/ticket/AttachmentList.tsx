import React, { useState } from 'react';
import { List, Button, Popconfirm, Typography, Space, Upload, message } from 'antd';
import { PaperClipOutlined, DeleteOutlined, DownloadOutlined, InboxOutlined } from '@ant-design/icons';
import type { AttachmentItem } from '@/api/ticketApi';
import { ticketApi } from '@/api/ticketApi';
import { uploadFile } from '@/api/file';

const { Dragger } = Upload;

interface AttachmentListProps {
  ticketId: number;
  attachments: AttachmentItem[];
  onRefresh: () => void;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const AttachmentList: React.FC<AttachmentListProps> = ({ ticketId, attachments, onRefresh }) => {
  const [uploading, setUploading] = useState(false);

  const handleDelete = async (attachmentId: number) => {
    await ticketApi.deleteAttachment(ticketId, attachmentId);
    message.success('附件已删除');
    onRefresh();
  };

  const handleUpload = async (file: File) => {
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      message.error('单个文件不能超过 10MB');
      return false;
    }

    setUploading(true);
    try {
      const result: any = await uploadFile(file);
      const meta = result?.data ?? result;
      await ticketApi.addAttachment(ticketId, {
        fileId: meta.id,
        fileName: meta.originalName || file.name,
        fileSize: meta.fileSize || file.size,
        fileType: meta.contentType || file.type,
        fileUrl: meta.url || '',
      });
      message.success(`${file.name} 上传成功`);
      onRefresh();
    } catch {
      message.error(`${file.name} 上传失败`);
    } finally {
      setUploading(false);
    }
    return false;
  };

  return (
    <div>
      <List
        size="small"
        dataSource={attachments}
        locale={{ emptyText: '暂无附件' }}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button
                key="download"
                type="link"
                size="small"
                icon={<DownloadOutlined />}
                href={item.fileUrl}
                target="_blank"
              >
                下载
              </Button>,
              <Popconfirm key="del" title="确认删除该附件？" onConfirm={() => handleDelete(item.id)}>
                <Button type="link" size="small" icon={<DeleteOutlined />} danger />
              </Popconfirm>,
            ]}
          >
            <Space>
              <PaperClipOutlined />
              <Typography.Text>{item.fileName}</Typography.Text>
              <Typography.Text type="secondary">{formatFileSize(item.fileSize)}</Typography.Text>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>{item.createTime}</Typography.Text>
            </Space>
          </List.Item>
        )}
      />

      <div style={{ marginTop: 16 }}>
        <Dragger
          multiple
          showUploadList={false}
          beforeUpload={(file) => {
            handleUpload(file as File);
            return false;
          }}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">
            {uploading ? '上传中...' : '点击或拖拽文件到此区域上传'}
          </p>
          <p className="ant-upload-hint">支持常见文件格式，单个文件最大 10MB</p>
        </Dragger>
      </div>
    </div>
  );
};

export default AttachmentList;

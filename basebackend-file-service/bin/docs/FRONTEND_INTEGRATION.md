# 前端集成指南

本文档说明如何在React应用中使用文件服务的预览功能。

## 📦 依赖安装

### 安装PDF.js React组件

```bash
npm install react-pdf
npm install pdfjs-dist
```

### 安装图片预览组件

```bash
npm install react-image-gallery
npm install react-image-zoom
```

## 🔧 PDF预览集成

### 1. 基本用法

```tsx
import React from 'react';
import PdfViewer from './components/PdfViewer';

function App() {
  return (
    <div>
      <h1>PDF预览</h1>
      <PdfViewer
        fileId="file-12345"
        onLoadSuccess={(document) => {
          console.log('PDF加载成功:', document.numPages);
        }}
        onLoadError={(error) => {
          console.error('PDF加载失败:', error);
        }}
      />
    </div>
  );
}
```

### 2. 直接使用URL

```tsx
<PdfViewer
  fileUrl="https://example.com/document.pdf"
  onLoadSuccess={(document) => {
    console.log('PDF总页数:', document.numPages);
  }}
/>
```

### 3. 高级用法（自定义样式）

```tsx
import { useState } from 'react';
import PdfViewer from './components/PdfViewer';

function AdvancedPdfViewer() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  return (
    <div className="advanced-pdf-viewer">
      <div className="viewer-header">
        <div className="page-controls">
          <button
            onClick={() => {
              const newPage = Math.max(1, currentPage - 1);
              setCurrentPage(newPage);
            }}
            disabled={currentPage === 1}
          >
            上一页
          </button>
          <span>
            {currentPage} / {totalPages}
          </span>
          <button
            onClick={() => {
              const newPage = Math.min(totalPages, currentPage + 1);
              setCurrentPage(newPage);
            }}
            disabled={currentPage === totalPages}
          >
            下一页
          </button>
        </div>
      </div>

      <div className="viewer-content">
        <PdfViewer
          fileId="file-12345"
          onLoadSuccess={(document) => {
            setTotalPages(document.numPages);
            setLoading(false);
          }}
          onLoadError={(error) => {
            console.error('加载失败:', error);
            setLoading(false);
          }}
        />
      </div>
    </div>
  );
}
```

## 🖼️ 图片预览集成

### 1. 基本用法

```tsx
import React, { useState } from 'react';

function ImagePreview() {
  const [imageUrl, setImageUrl] = useState('/api/files/12345/preview');

  return (
    <div className="image-preview">
      <img
        src={imageUrl}
        alt="预览图片"
        style={{
          maxWidth: '100%',
          height: 'auto',
          cursor: 'pointer'
        }}
        onClick={() => {
          // 打开全屏预览
          window.open(imageUrl, '_blank');
        }}
      />
    </div>
  );
}
```

### 2. 缩略图预览

```tsx
import React, { useState } from 'react';

function ThumbnailPreview({ fileId }: { fileId: string }) {
  const [thumbnailUrl, setThumbnailUrl] = useState('');

  // 获取缩略图
  React.useEffect(() => {
    fetch(`/api/files/${fileId}/thumbnail`)
      .then(res => res.json())
      .then(data => setThumbnailUrl(data.url))
      .catch(err => console.error('获取缩略图失败:', err));
  }, [fileId]);

  return (
    <div className="thumbnail-container">
      <img
        src={thumbnailUrl}
        alt="缩略图"
        style={{
          width: '200px',
          height: '150px',
          objectFit: 'cover',
          border: '1px solid #ddd',
          borderRadius: '4px'
        }}
        onClick={() => {
          // 点击查看原图
          window.open(`/api/files/${fileId}`, '_blank');
        }}
      />
    </div>
  );
}
```

## 🎨 完整文件管理界面示例

```tsx
import React, { useState } from 'react';
import PdfViewer from './PdfViewer';
import { Button, Card, Modal } from 'antd';

interface FileItem {
  fileId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
}

function FileManager() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null);
  const [previewVisible, setPreviewVisible] = useState(false);

  const openPreview = async (file: FileItem) => {
    setPreviewFile(file);

    if (file.fileType === 'pdf') {
      setPreviewVisible(true);
    } else if (file.fileType.startsWith('image/')) {
      // 图片预览
      window.open(`/api/files/${file.fileId}/preview`, '_blank');
    } else {
      // 其他类型文件直接下载
      window.open(`/api/files/${file.fileId}`, '_blank');
    }
  };

  return (
    <div className="file-manager">
      <div className="file-list">
        {files.map(file => (
          <Card
            key={file.fileId}
            hoverable
            style={{ marginBottom: 16 }}
            onClick={() => openPreview(file)}
          >
            <Card.Meta
              title={file.fileName}
              description={`${file.fileType} - ${formatFileSize(file.fileSize)}`}
            />
          </Card>
        ))}
      </div>

      <Modal
        title={previewFile?.fileName}
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={null}
        width={800}
      >
        {previewFile?.fileType === 'pdf' && (
          <PdfViewer
            fileId={previewFile.fileId}
            onLoadSuccess={(document) => {
              console.log('PDF加载成功');
            }}
            onLoadError={(error) => {
              console.error('PDF加载失败:', error);
            }}
          />
        )}
      </Modal>
    </div>
  );
}

// 格式化文件大小
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}
```

## 📡 API调用示例

### 获取文件预览URL

```typescript
// JavaScript/TypeScript
async function getFilePreviewUrl(fileId: string): Promise<string> {
  const response = await fetch(`/api/files/${fileId}/preview`);
  const data = await response.json();
  return data.url;
}

// 使用示例
getFilePreviewUrl('file-12345')
  .then(url => {
    console.log('预览URL:', url);
  })
  .catch(err => {
    console.error('获取预览URL失败:', err);
  });
```

### 获取图片缩略图

```typescript
async function getThumbnailUrl(fileId: string, width: number = 200, height: number = 200): Promise<string> {
  const response = await fetch(`/api/files/${fileId}/thumbnail?width=${width}&height=${height}`);
  const data = await response.json();
  return data.url;
}

// 使用示例
getThumbnailUrl('file-12345', 300, 300)
  .then(url => {
    console.log('缩略图URL:', url);
  });
```

## 🔍 搜索和过滤

```typescript
interface SearchParams {
  keyword?: string;
  fileType?: string;
  dateFrom?: string;
  dateTo?: string;
  sizeMin?: number;
  sizeMax?: number;
}

async function searchFiles(params: SearchParams): Promise<FileItem[]> {
  const queryParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      queryParams.append(key, value.toString());
    }
  });

  const response = await fetch(`/api/files/search?${queryParams.toString()}`);
  const data = await response.json();
  return data.files;
}

// 使用示例
searchFiles({
  keyword: 'document',
  fileType: 'pdf'
}).then(files => {
  console.log('搜索结果:', files);
});
```

## 🎯 最佳实践

1. **性能优化**
   - 对大文件列表使用虚拟滚动
   - 图片懒加载
   - 预览图片缓存

2. **用户体验**
   - 添加加载状态
   - 错误处理
   - 响应式设计

3. **安全性**
   - 验证文件类型
   - 限制文件大小
   - 权限控制

## 📚 更多资源

- [react-pdf 文档](https://react-pdf-viewer.netlify.app/)
- [PDF.js 官方文档](https://mozilla.github.io/pdf.js/)
- [Ant Design 组件库](https://ant.design/)

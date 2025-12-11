import React, { useState, useEffect } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import 'react-pdf/dist/Page/TextLayer.css';
import 'react-pdf/dist/Page/AnnotationLayer.css';

// 配置PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.js`;

interface PdfViewerProps {
  fileId: string;
  fileUrl?: string;
  onLoadSuccess?: (document: any) => void;
  onLoadError?: (error: Error) => void;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({
  fileId,
  fileUrl,
  onLoadSuccess,
  onLoadError
}) => {
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState<number>(1);
  const [pdfUrl, setPdfUrl] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [scale, setScale] = useState<number>(1.0);

  // 加载PDF URL
  useEffect(() => {
    const loadPdfUrl = async () => {
      setLoading(true);
      try {
        if (fileUrl) {
          setPdfUrl(fileUrl);
        } else {
          // 从文件ID获取PDF预览URL
          const response = await fetch(`/api/files/${fileId}/preview`);
          const data = await response.json();
          setPdfUrl(data.url);
        }
      } catch (error) {
        console.error('加载PDF URL失败:', error);
        onLoadError?.(error as Error);
      } finally {
        setLoading(false);
      }
    };

    loadPdfUrl();
  }, [fileId, fileUrl, onLoadError]);

  // PDF加载成功回调
  const handleLoadSuccess = (document: any) => {
    setNumPages(document.numPages);
    setLoading(false);
    onLoadSuccess?.(document);
  };

  // PDF加载失败回调
  const handleLoadError = (error: Error) => {
    console.error('PDF加载失败:', error);
    setLoading(false);
    onLoadError?.(error);
  };

  // 上一页
  const previousPage = () => {
    if (pageNumber > 1) {
      setPageNumber(pageNumber - 1);
    }
  };

  // 下一页
  const nextPage = () => {
    if (pageNumber < numPages) {
      setPageNumber(pageNumber + 1);
    }
  };

  // 放大
  const zoomIn = () => {
    setScale(scale + 0.25);
  };

  // 缩小
  const zoomOut = () => {
    if (scale > 0.5) {
      setScale(scale - 0.25);
    }
  };

  // 适应宽度
  const fitToWidth = () => {
    setScale(1.0);
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '20px' }}>
        <div>加载PDF中...</div>
      </div>
    );
  }

  return (
    <div className="pdf-viewer">
      {/* 工具栏 */}
      <div className="pdf-toolbar">
        <div className="pdf-controls">
          <button onClick={previousPage} disabled={pageNumber <= 1}>
            上一页
          </button>
          <span>
            第 {pageNumber} 页 / 共 {numPages} 页
          </span>
          <button onClick={nextPage} disabled={pageNumber >= numPages}>
            下一页
          </button>
        </div>

        <div className="pdf-zoom">
          <button onClick={zoomOut}>-</button>
          <span>{Math.round(scale * 100)}%</span>
          <button onClick={zoomIn}>+</button>
          <button onClick={fitToWidth}>适应宽度</button>
        </div>
      </div>

      {/* PDF页面 */}
      <div className="pdf-container">
        <Document
          file={pdfUrl}
          onLoadSuccess={handleLoadSuccess}
          onLoadError={handleLoadError}
          loading={<div>加载PDF...</div>}
        >
          <Page
            pageNumber={pageNumber}
            scale={scale}
            renderTextLayer={true}
            renderAnnotationLayer={true}
          />
        </Document>
      </div>
    </div>
  );
};

export default PdfViewer;

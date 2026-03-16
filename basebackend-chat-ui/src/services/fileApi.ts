import axios from 'axios';
import type { ApiResult } from '@/types';

/** 文件上传响应 */
export interface UploadResult {
  fileName: string;
  storedName: string;
  fileSize: number;
  contentType: string;
  url: string;
}

/**
 * 上传文件（图片/文件/语音/视频）
 * 使用 multipart/form-data，需要单独的 axios 实例
 */
export async function uploadFile(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);

  const token = localStorage.getItem('token');
  const tenantId = localStorage.getItem('tenantId') || '0';
  const userId = localStorage.getItem('userId');

  const resp = await axios.post<ApiResult<UploadResult>>('/api/chat/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      'X-Tenant-Id': tenantId,
      ...(userId ? { 'X-User-Id': userId } : {}),
    },
    timeout: 60000,
  });

  if (resp.data.code !== 200) {
    throw new Error(resp.data.message || '上传失败');
  }
  return resp.data.data;
}

import request from '@/utils/request'
import type { Result, PageResult } from '@/types'
import type {
  FileMetadata,
  FileVersion,
  FilePermission,
  FileRecycleBin,
  FileShare,
  FileOperationLog,
  FileTag,
  FileQueryParams,
  FileStatistics,
} from '@/types/file'

// ==================== 文件基本操作 ====================

/**
 * 上传文件（V2版本 - 支持权限控制和版本管理）
 */
export const uploadFile = (file: File, folderId?: number, onProgress?: (percent: number) => void) => {
  const formData = new FormData()
  formData.append('file', file)
  if (folderId) {
    formData.append('folderId', String(folderId))
  }

  return request.post<Result<FileMetadata>>('/api/files/upload-v2', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    },
  })
}

/**
 * 下载文件（V2版本）
 */
export const downloadFile = (fileId: string) => {
  return request.get(`/api/files/download-v2/${fileId}`, {
    responseType: 'blob',
  })
}

/**
 * 获取文件列表
 */
export const getFileList = (params: FileQueryParams) => {
  return request.get<PageResult<FileMetadata>>('/api/files/list', { params })
}

/**
 * 获取文件详情
 */
export const getFileDetail = (fileId: string) => {
  return request.get<Result<FileMetadata>>(`/api/files/${fileId}`)
}

/**
 * 删除文件（移入回收站）
 */
export const deleteFile = (fileId: string) => {
  return request.delete<Result<void>>(`/api/files/${fileId}`)
}

/**
 * 批量删除文件
 */
export const batchDeleteFiles = (fileIds: string[]) => {
  return request.delete<Result<void>>('/api/files/batch', { data: fileIds })
}

/**
 * 重命名文件
 */
export const renameFile = (fileId: string, newName: string) => {
  return request.put<Result<void>>(`/api/files/${fileId}/rename`, null, {
    params: { newName },
  })
}

/**
 * 移动文件
 */
export const moveFile = (fileId: string, targetFolderId: number) => {
  return request.put<Result<void>>(`/api/files/${fileId}/move`, null, {
    params: { targetFolderId },
  })
}

/**
 * 复制文件
 */
export const copyFile = (fileId: string, targetFolderId?: number) => {
  return request.post<Result<FileMetadata>>(`/api/files/${fileId}/copy`, null, {
    params: { targetFolderId },
  })
}

// ==================== 版本管理 ====================

/**
 * 获取文件版本历史
 */
export const getFileVersions = (fileId: string) => {
  return request.get<Result<FileVersion[]>>(`/api/files/${fileId}/versions`)
}

/**
 * 创建文件版本
 */
export const createFileVersion = (fileId: string, file: File, description?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (description) {
    formData.append('description', description)
  }

  return request.post<Result<FileVersion>>(`/api/files/${fileId}/version`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/**
 * 版本回退
 */
export const revertToVersion = (fileId: string, versionId: number) => {
  return request.post<Result<void>>(`/api/files/${fileId}/revert/${versionId}`)
}

/**
 * 下载指定版本
 */
export const downloadVersion = (fileId: string, versionId: number) => {
  return request.get(`/api/files/${fileId}/versions/${versionId}/download`, {
    responseType: 'blob',
  })
}

// ==================== 回收站管理 ====================

/**
 * 获取回收站列表
 */
export const getRecycleBinList = (params: { current?: number; size?: number }) => {
  return request.get<PageResult<FileRecycleBin>>('/api/files/recycle-bin', { params })
}

/**
 * 恢复文件
 */
export const restoreFile = (fileId: string) => {
  return request.post<Result<void>>(`/api/files/${fileId}/restore`)
}

/**
 * 彻底删除文件
 */
export const permanentDeleteFile = (fileId: string) => {
  return request.delete<Result<void>>(`/api/files/${fileId}/permanent`)
}

/**
 * 批量恢复文件
 */
export const batchRestoreFiles = (fileIds: string[]) => {
  return request.post<Result<void>>('/api/files/restore/batch', fileIds)
}

/**
 * 批量彻底删除
 */
export const batchPermanentDelete = (fileIds: string[]) => {
  return request.delete<Result<void>>('/api/files/permanent/batch', { data: fileIds })
}

/**
 * 清空回收站
 */
export const emptyRecycleBin = () => {
  return request.delete<Result<void>>('/api/files/recycle-bin/empty')
}

// ==================== 权限管理 ====================

/**
 * 获取文件权限列表
 */
export const getFilePermissions = (fileId: string) => {
  return request.get<Result<FilePermission[]>>(`/api/files/${fileId}/permissions`)
}

/**
 * 添加文件权限
 */
export const addFilePermission = (fileId: string, permission: Partial<FilePermission>) => {
  return request.post<Result<void>>(`/api/files/${fileId}/permissions`, permission)
}

/**
 * 删除文件权限
 */
export const deleteFilePermission = (fileId: string, permissionId: number) => {
  return request.delete<Result<void>>(`/api/files/${fileId}/permissions/${permissionId}`)
}

/**
 * 设置文件公开状态
 */
export const setFilePublic = (fileId: string, isPublic: boolean) => {
  return request.put<Result<void>>(`/api/files/${fileId}/public`, null, {
    params: { isPublic },
  })
}

// ==================== 文件分享 ====================

/**
 * 创建文件分享
 */
export const createFileShare = (data: {
  fileId: string
  sharePassword?: string
  expireTime?: string
  downloadLimit?: number
  allowDownload?: boolean
  allowPreview?: boolean
}) => {
  return request.post<Result<FileShare>>('/api/files/share', data)
}

/**
 * 获取分享信息
 */
export const getShareInfo = (shareCode: string, password?: string) => {
  return request.get<Result<FileShare>>(`/api/files/share/${shareCode}`, {
    params: { password },
  })
}

/**
 * 取消分享
 */
export const cancelShare = (shareId: number) => {
  return request.delete<Result<void>>(`/api/files/share/${shareId}`)
}

/**
 * 获取我的分享列表
 */
export const getMyShares = (params: { current?: number; size?: number }) => {
  return request.get<PageResult<FileShare>>('/api/files/share/my', { params })
}

// ==================== 文件标签 ====================

/**
 * 获取所有标签
 */
export const getAllTags = () => {
  return request.get<Result<FileTag[]>>('/api/files/tags')
}

/**
 * 创建标签
 */
export const createTag = (data: { tagName: string; tagColor: string; description?: string }) => {
  return request.post<Result<FileTag>>('/api/files/tags', data)
}

/**
 * 为文件添加标签
 */
export const addFileTag = (fileId: string, tagId: number) => {
  return request.post<Result<void>>(`/api/files/${fileId}/tags/${tagId}`)
}

/**
 * 移除文件标签
 */
export const removeFileTag = (fileId: string, tagId: number) => {
  return request.delete<Result<void>>(`/api/files/${fileId}/tags/${tagId}`)
}

/**
 * 获取文件的标签
 */
export const getFileTags = (fileId: string) => {
  return request.get<Result<FileTag[]>>(`/api/files/${fileId}/tags`)
}

// ==================== 操作日志 ====================

/**
 * 获取文件操作日志
 */
export const getFileOperationLogs = (fileId: string, params: { current?: number; size?: number }) => {
  return request.get<PageResult<FileOperationLog>>(`/api/files/${fileId}/logs`, { params })
}

// ==================== 统计信息 ====================

/**
 * 获取文件统计信息
 */
export const getFileStatistics = () => {
  return request.get<Result<FileStatistics>>('/api/files/statistics')
}

/**
 * 获取存储使用情况
 */
export const getStorageUsage = () => {
  return request.get<Result<{
    used: number
    total: number
    percentage: number
  }>>('/api/files/storage/usage')
}

// ==================== 文件预览 ====================

/**
 * 获取文件预览URL
 */
export const getFilePreviewUrl = (fileId: string) => {
  return request.get<Result<string>>(`/api/files/${fileId}/preview-url`)
}

/**
 * 获取缩略图URL
 */
export const getThumbnailUrl = (fileId: string) => {
  return request.get<Result<string>>(`/api/files/${fileId}/thumbnail-url`)
}

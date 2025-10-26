/**
 * 文件管理相关类型定义
 */

// 文件元数据
export interface FileMetadata {
  id: number
  fileId: string
  fileName: string
  originalName: string
  filePath: string
  fileSize: number
  contentType: string
  fileExtension: string
  md5: string
  sha256?: string
  storageType: string
  bucketName?: string
  folderId?: number
  folderPath?: string
  isFolder: boolean
  ownerId: number
  ownerName: string
  isPublic: boolean
  isDeleted: boolean
  deletedAt?: string
  deletedBy?: number
  version: number
  latestVersionId?: number
  downloadCount: number
  viewCount: number
  thumbnailPath?: string
  tags?: string
  description?: string
  metadata?: string
  createTime: string
  updateTime: string
}

// 文件版本
export interface FileVersion {
  id: number
  fileId: string
  versionNumber: number
  filePath: string
  fileSize: number
  md5: string
  changeDescription?: string
  createdBy: number
  createdByName: string
  isCurrent: boolean
  createTime: string
  updateTime: string
}

// 文件权限
export interface FilePermission {
  id: number
  fileId: string
  userId?: number
  userName?: string
  roleId?: number
  roleName?: string
  deptId?: number
  deptName?: string
  permissionType: 'READ' | 'WRITE' | 'DELETE' | 'SHARE'
  expireTime?: string
  grantedBy: number
  grantedByName: string
  createTime: string
  updateTime: string
}

// 回收站记录
export interface FileRecycleBin {
  id: number
  fileId: string
  fileName: string
  filePath: string
  fileSize: number
  deletedBy: number
  deletedByName: string
  deletedAt: string
  expireAt: string
  originalMetadata?: string
  createTime: string
  updateTime: string
}

// 文件分享
export interface FileShare {
  id: number
  fileId: string
  shareCode: string
  sharePassword?: string
  sharedBy: number
  sharedByName: string
  expireTime?: string
  downloadLimit?: number
  downloadCount: number
  viewCount: number
  allowDownload: boolean
  allowPreview: boolean
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
  createTime: string
  updateTime: string
}

// 文件操作日志
export interface FileOperationLog {
  id: number
  fileId: string
  operationType: string
  operatorId: number
  operatorName: string
  operationDetail: string
  operationTime: string
  ipAddress?: string
  userAgent?: string
  createTime: string
}

// 文件标签
export interface FileTag {
  id: number
  tagName: string
  tagColor: string
  description?: string
  createdBy: number
  createdByName: string
  usageCount: number
  createTime: string
  updateTime: string
}

// 文件上传参数
export interface FileUploadParams {
  file: File
  folderId?: number
  onProgress?: (percent: number) => void
}

// 文件下载信息
export interface FileDownloadInfo {
  fileName: string
  fileSize: number
  contentType: string
  url: string
}

// 文件查询参数
export interface FileQueryParams {
  fileName?: string
  fileExtension?: string
  folderId?: number
  ownerId?: number
  isPublic?: boolean
  startTime?: string
  endTime?: string
  current?: number
  size?: number
}

// 文件统计信息
export interface FileStatistics {
  totalFiles: number
  totalSize: number
  storageUsage: {
    local: number
    minio: number
    oss: number
    s3: number
  }
  fileTypeDistribution: {
    type: string
    count: number
    size: number
  }[]
}

// 视图模式
export type ViewMode = 'grid' | 'list'

// 排序字段
export type SortField = 'fileName' | 'fileSize' | 'createTime' | 'updateTime'

// 排序顺序
export type SortOrder = 'ascend' | 'descend'

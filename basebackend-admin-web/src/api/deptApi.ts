import request from '@/api/request';
import type { DeptDTO } from '@/types';

/**
 * 部门管理 API
 * 对接后端 /api/system/depts/* 接口
 */
export const deptApi = {
  /** 查询部门树 */
  tree: (): Promise<DeptDTO[]> =>
    request.get('/api/system/depts/tree'),

  /** 查询部门列表（支持按名称和状态筛选） */
  list: (params?: { deptName?: string; status?: number }): Promise<DeptDTO[]> =>
    request.get('/api/system/depts', { params }),

  /** 创建部门 */
  create: (data: Partial<DeptDTO>): Promise<void> =>
    request.post('/api/system/depts', data),

  /** 更新部门 */
  update: (id: number, data: Partial<DeptDTO>): Promise<void> =>
    request.put(`/api/system/depts/${id}`, data),

  /** 删除部门 */
  delete: (id: number): Promise<void> =>
    request.delete(`/api/system/depts/${id}`),

  /** 校验部门名称唯一性 */
  checkName: (params: { deptName: string; parentId: number }): Promise<boolean> =>
    request.get('/api/system/depts/check-dept-name', { params }),
};

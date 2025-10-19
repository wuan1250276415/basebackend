import request from '@/utils/request'
import { Dept, Result } from '@/types'

// 获取部门树
export const getDeptTree = () => {
  return request.get<Dept[]>('/admin/depts/tree')
}

// 获取部门列表
export const getDeptList = () => {
  return request.get<Dept[]>('/admin/depts')
}

// 根据ID查询部门
export const getDeptById = (id: string) => {
  return request.get<Result<Dept>>(`/admin/depts/${id}`)
}

// 创建部门
export const createDept = (data: Dept) => {
  return request.post<Result<string>>('/admin/depts', data)
}

// 更新部门
export const updateDept = (id: string, data: Dept) => {
  return request.put<Result<string>>(`/admin/depts/${id}`, data)
}

// 删除部门
export const deleteDept = (id: string) => {
  return request.delete<Result<string>>(`/admin/depts/${id}`)
}

// 根据部门ID获取子部门列表
export const getChildrenByDeptId = (id: string) => {
  return request.get<Result<Dept[]>>(`/admin/depts/${id}/children`)
}

// 根据部门ID获取所有子部门ID列表
export const getChildrenDeptIds = (id: string) => {
  return request.get<Result<string[]>>(`/admin/depts/${id}/children-ids`)
}

// 检查部门名称唯一性
export const checkDeptNameUnique = (deptName: string, parentId: string, deptId?: string) => {
  return request.get<Result<boolean>>('/admin/depts/check-dept-name', {
    params: { deptName, parentId, deptId },
  })
}

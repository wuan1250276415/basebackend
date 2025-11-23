import request from '@/utils/request'
import { Dept, Result } from '@/types'

// 获取部门树
export const getDeptTree = () => {
  return request.get<Dept[]>('/basebackend-system-api/api/system/depts/tree')
}

// 获取部门列表
export const getDeptList = () => {
  return request.get<Dept[]>('/basebackend-system-api/api/system/depts/')
}

// 根据ID查询部门
export const getDeptById = (id: string) => {
  return request.get<Result<Dept>>(`/basebackend-system-api/api/system/depts/${id}`)
}

// 创建部门
export const createDept = (data: Dept) => {
  return request.post<Result<string>>('/basebackend-system-api/api/system/depts', data)
}

// 更新部门
export const updateDept = (id: string, data: Dept) => {
  return request.put<Result<string>>(`/basebackend-system-api/api/system/depts/${id}`, data)
}

// 删除部门
export const deleteDept = (id: string) => {
  return request.delete<Result<string>>(`/basebackend-system-api/api/system/depts/${id}`)
}

// 根据部门ID获取子部门列表
export const getChildrenByDeptId = (id: string) => {
  return request.get<Result<Dept[]>>(`/basebackend-system-api/api/system/depts/${id}/children`)
}

// 根据部门ID获取所有子部门ID列表
export const getChildrenDeptIds = (id: string) => {
  return request.get<Result<string[]>>(`/basebackend-system-api/api/system/depts/${id}/children-ids`)
}

// 检查部门名称唯一性
export const checkDeptNameUnique = (deptName: string, parentId: string, deptId?: string) => {
  return request.get<Result<boolean>>('/basebackend-system-api/api/system/depts/check-dept-name', {
    params: { deptName, parentId, deptId },
  })
}

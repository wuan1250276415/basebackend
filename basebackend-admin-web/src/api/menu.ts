import request from '@/utils/request'
import { Menu, Result } from '@/types'

const BASE_URL = '/basebackend-system-api/api/system/application/resource'
// 获取菜单树
export const getMenuTree = () => {
  return request.get<Menu[]>(`${BASE_URL}/tree`)
}

// 获取菜单列表
export const getMenuList = () => {
  return request.get<Menu[]>(`${BASE_URL}`)
}

// 根据ID查询菜单
export const getMenuById = (id: string) => {
  return request.get<Result<Menu>>(`${BASE_URL}/${id}`)
}

// 创建菜单
export const createMenu = (data: Menu) => {
  return request.post<Result<string>>(`${BASE_URL}`, data)
}

// 更新菜单
export const updateMenu = (id: string, data: Menu) => {
  return request.put<Result<string>>(`${BASE_URL}/${id}`, data)
}

// 删除菜单
export const deleteMenu = (id: string) => {
  return request.delete<Result<string>>(`${BASE_URL}/${id}`)
}

// 获取前端路由
export const getRoutes = () => {
  return request.get<Result<Menu[]>>(`${BASE_URL}/routes`)
}

// 根据用户ID获取菜单树
export const getMenuTreeByUserId = (userId: string) => {
  return request.get<Result<Menu[]>>(`${BASE_URL}/user/${userId}`)
}

// 获取当前登录用户的菜单树
export const getCurrentUserMenuTree = () => {
  return request.get<Menu[]>(`${BASE_URL}/current-user`)
}

// 检查菜单名称唯一性
export const checkMenuNameUnique = (menuName: string, parentId: string, menuId?: string) => {
  return request.get<Result<boolean>>(`${BASE_URL}/check-menu-name`, {
    params: { menuName, parentId, menuId },
  })
}

import request from '@/utils/request'
import { Menu, Result } from '@/types'

// 获取菜单树
export const getMenuTree = () => {
  return request.get<Menu[]>('/admin/menus/tree')
}

// 获取菜单列表
export const getMenuList = () => {
  return request.get<Menu[]>('/admin/menus')
}

// 根据ID查询菜单
export const getMenuById = (id: string) => {
  return request.get<Result<Menu>>(`/admin/menus/${id}`)
}

// 创建菜单
export const createMenu = (data: Menu) => {
  return request.post<Result<string>>('/admin/menus', data)
}

// 更新菜单
export const updateMenu = (id: string, data: Menu) => {
  return request.put<Result<string>>(`/admin/menus/${id}`, data)
}

// 删除菜单
export const deleteMenu = (id: string) => {
  return request.delete<Result<string>>(`/admin/menus/${id}`)
}

// 获取前端路由
export const getRoutes = () => {
  return request.get<Result<Menu[]>>('/admin/menus/routes')
}

// 根据用户ID获取菜单树
export const getMenuTreeByUserId = (userId: string) => {
  return request.get<Result<Menu[]>>(`/admin/menus/user/${userId}`)
}

// 获取当前登录用户的菜单树
export const getCurrentUserMenuTree = () => {
  return request.get<Menu[]>('/admin/menus/current-user')
}

// 检查菜单名称唯一性
export const checkMenuNameUnique = (menuName: string, parentId: string, menuId?: string) => {
  return request.get<Result<boolean>>('/admin/menus/check-menu-name', {
    params: { menuName, parentId, menuId },
  })
}

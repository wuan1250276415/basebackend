import request from '@/utils/request'
import { Dict, DictData, Result, PageResult } from '@/types'

// 分页查询字典列表
export const getDictPage = (params: any) => {
  return request.get<PageResult<Dict>>('/admin/dicts', { params })
}

// 根据ID查询字典
export const getDictById = (id: string) => {
  return request.get<Result<Dict>>(`/admin/dicts/${id}`)
}

// 创建字典
export const createDict = (data: Dict) => {
  return request.post<Result<string>>('/admin/dicts', data)
}

// 更新字典
export const updateDict = (id: string, data: Dict) => {
  return request.put<Result<string>>(`/admin/dicts/${id}`, data)
}

// 删除字典
export const deleteDict = (id: string) => {
  return request.delete<Result<string>>(`/admin/dicts/${id}`)
}

// 根据字典类型查询字典数据
export const getDictDataByType = (dictType: string) => {
  return request.get<Result<DictData[]>>(`/admin/dicts/data/type/${dictType}`)
}

// 分页查询字典数据列表
export const getDictDataPage = (params: any) => {
  return request.get<PageResult<DictData>>('/admin/dicts/data', { params })
}

// 根据ID查询字典数据
export const getDictDataById = (id: string) => {
  return request.get<Result<DictData>>(`/admin/dicts/data/${id}`)
}

// 创建字典数据
export const createDictData = (data: DictData) => {
  return request.post<Result<string>>('/admin/dicts/data', data)
}

// 更新字典数据
export const updateDictData = (id: string, data: DictData) => {
  return request.put<Result<string>>(`/admin/dicts/data/${id}`, data)
}

// 删除字典数据
export const deleteDictData = (id: string) => {
  return request.delete<Result<string>>(`/admin/dicts/data/${id}`)
}

// 刷新字典缓存
export const refreshDictCache = () => {
  return request.post<Result<string>>('/admin/dicts/refresh-cache')
}

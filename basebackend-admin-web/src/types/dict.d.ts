/**
 * 字典管理类型定义
 * 对应后端 DictDTO 和 DictDataDTO
 */

/** 字典类型数据传输对象，对应后端 DictDTO */
export interface DictTypeDTO {
  /** 字典类型ID */
  id: number;
  /** 应用ID */
  appId: number;
  /** 字典名称 */
  dictName: string;
  /** 字典类型标识 */
  dictType: string;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 备注 */
  remark: string;
}

/** 字典数据传输对象，对应后端 DictDataDTO */
export interface DictDataDTO {
  /** 字典数据ID */
  id: number;
  /** 应用ID */
  appId: number;
  /** 排序号 */
  dictSort: number;
  /** 字典标签 */
  dictLabel: string;
  /** 字典值 */
  dictValue: string;
  /** 字典类型标识 */
  dictType: string;
  /** CSS样式类名 */
  cssClass: string;
  /** 列表样式类名 */
  listClass: string;
  /** 是否默认（0-否, 1-是） */
  isDefault: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 备注 */
  remark: string;
}

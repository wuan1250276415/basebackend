/**
 * 通用 API 响应类型定义
 * 对应后端 com.basebackend.common.model.Result<T> 和分页结构
 */

/** 统一响应结果，对应后端 Result<T> */
export interface Result<T = unknown> {
  /** 状态码 */
  code: number;
  /** 响应消息 */
  message: string;
  /** 响应数据 */
  data: T;
  /** 是否成功 */
  success: boolean;
}

/** 分页结果，对应后端 MyBatis-Plus Page<T> */
export interface PageResult<T> {
  /** 数据列表 */
  records: T[];
  /** 总记录数 */
  total: number;
  /** 每页大小 */
  size: number;
  /** 当前页码 */
  current: number;
  /** 总页数 */
  pages: number;
}

/** 简单分页结果，对应后端 com.basebackend.common.dto.PageResult<T> */
export interface SimplePageResult<T> {
  /** 数据列表 */
  list: T[];
  /** 总记录数 */
  total: number;
}

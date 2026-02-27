export * from './enums';
export * from './message';
export * from './conversation';
export * from './contact';
export * from './group';
export * from './websocket';

/** 通用 API 响应 */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/** 分页响应 */
export interface PageResult<T> {
  current: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}

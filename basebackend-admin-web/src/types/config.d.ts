/**
 * 系统配置类型定义
 * 对应后端 ConfigDTO
 */

/** 系统配置参数数据传输对象 */
export interface ConfigDTO {
  /** 配置ID */
  id: number;
  /** 参数名称 */
  configName: string;
  /** 参数键名 */
  configKey: string;
  /** 参数键值 */
  configValue: string;
  /** 配置类型（0-系统内置, 1-自定义） */
  configType: number;
  /** 备注 */
  remark: string;
}

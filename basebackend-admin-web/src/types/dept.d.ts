/**
 * 部门管理类型定义
 * 对应后端 DeptDTO
 */

/** 部门数据传输对象，对应后端 DeptDTO */
export interface DeptDTO {
  /** 部门ID */
  id: number;
  /** 部门名称 */
  deptName: string;
  /** 父部门ID */
  parentId: number;
  /** 排序号 */
  orderNum: number;
  /** 负责人 */
  leader: string;
  /** 联系电话 */
  phone: string;
  /** 邮箱 */
  email: string;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 备注 */
  remark: string;
  /** 子部门 */
  children?: DeptDTO[];
}

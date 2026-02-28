import request from '@/api/request';
import type { ProfileDetail, UpdateProfileParams, ChangePasswordParams } from '@/types';

/**
 * 个人中心 API
 * 对接后端 /api/user/profile/* 接口
 */
export const profileApi = {
  /** 获取个人资料详情 */
  getProfile: (): Promise<ProfileDetail> => request.get('/basebackend-user-api/api/user/profile/info'),

  /** 更新个人资料 */
  updateProfile: (params: UpdateProfileParams): Promise<void> =>
    request.put('/basebackend-user-api/api/user/profile/info', params),

  /** 修改密码 */
  changePassword: (params: ChangePasswordParams): Promise<void> =>
    request.put('/basebackend-user-api/api/user/profile/password', params),
};

import request from '@/api/request';
import type {
  FeatureCheckResponse,
  FeatureBatchCheckRequest,
  VariantResponse,
  FeatureToggleStatus,
} from '@/types/featureToggle';

const API_BASE_URL = '/api/feature-toggles';

/**
 * 特性开关API服务
 */
export const featureToggleApi = {
  /**
   * 检查单个特性是否启用
   */
  async checkFeature(
    featureName: string,
    context?: { userId?: string; username?: string; email?: string }
  ): Promise<FeatureCheckResponse> {
    return request.get<FeatureCheckResponse, FeatureCheckResponse>(`${API_BASE_URL}/check/${featureName}`, {
      params: context,
    });
  },

  /**
   * 批量检查多个特性
   */
  async checkFeaturesBatch(
    payload: FeatureBatchCheckRequest
  ): Promise<Record<string, boolean>> {
    return request.post<Record<string, boolean>, Record<string, boolean>>(`${API_BASE_URL}/check-batch`, payload);
  },

  /**
   * 获取所有特性开关状态
   */
  async getAllFeatures(context?: {
    userId?: string;
    username?: string;
    email?: string;
  }): Promise<Record<string, boolean>> {
    return request.get<Record<string, boolean>, Record<string, boolean>>(`${API_BASE_URL}/all`, {
      params: context,
    });
  },

  /**
   * 获取变体信息（用于AB测试）
   */
  async getVariant(
    featureName: string,
    context?: { userId?: string; username?: string; email?: string }
  ): Promise<VariantResponse> {
    return request.get<VariantResponse, VariantResponse>(`${API_BASE_URL}/variant/${featureName}`, {
      params: context,
    });
  },

  /**
   * 获取服务状态
   */
  async getStatus(): Promise<FeatureToggleStatus> {
    return request.get<FeatureToggleStatus, FeatureToggleStatus>(`${API_BASE_URL}/status`);
  },

  /**
   * 刷新特性开关配置
   */
  async refresh(): Promise<void> {
    await request.post<void, void>(`${API_BASE_URL}/refresh`);
  },
};

export default featureToggleApi;

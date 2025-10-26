import axios from 'axios';
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
    const params = new URLSearchParams();
    if (context?.userId) params.append('userId', context.userId);
    if (context?.username) params.append('username', context.username);
    if (context?.email) params.append('email', context.email);

    const response = await axios.get<{ data: FeatureCheckResponse }>(
      `${API_BASE_URL}/check/${featureName}?${params.toString()}`
    );
    return response.data.data;
  },

  /**
   * 批量检查多个特性
   */
  async checkFeaturesBatch(
    request: FeatureBatchCheckRequest
  ): Promise<Record<string, boolean>> {
    const response = await axios.post<{ data: Record<string, boolean> }>(
      `${API_BASE_URL}/check-batch`,
      request
    );
    return response.data.data;
  },

  /**
   * 获取所有特性开关状态
   */
  async getAllFeatures(context?: {
    userId?: string;
    username?: string;
    email?: string;
  }): Promise<Record<string, boolean>> {
    const params = new URLSearchParams();
    if (context?.userId) params.append('userId', context.userId);
    if (context?.username) params.append('username', context.username);
    if (context?.email) params.append('email', context.email);

    const response = await axios.get<{ data: Record<string, boolean> }>(
      `${API_BASE_URL}/all?${params.toString()}`
    );
    return response.data.data;
  },

  /**
   * 获取变体信息（用于AB测试）
   */
  async getVariant(
    featureName: string,
    context?: { userId?: string; username?: string; email?: string }
  ): Promise<VariantResponse> {
    const params = new URLSearchParams();
    if (context?.userId) params.append('userId', context.userId);
    if (context?.username) params.append('username', context.username);
    if (context?.email) params.append('email', context.email);

    const response = await axios.get<{ data: VariantResponse }>(
      `${API_BASE_URL}/variant/${featureName}?${params.toString()}`
    );
    return response.data.data;
  },

  /**
   * 获取服务状态
   */
  async getStatus(): Promise<FeatureToggleStatus> {
    const response = await axios.get<{ data: FeatureToggleStatus }>(
      `${API_BASE_URL}/status`
    );
    return response.data.data;
  },

  /**
   * 刷新特性开关配置
   */
  async refresh(): Promise<void> {
    await axios.post(`${API_BASE_URL}/refresh`);
  },
};

export default featureToggleApi;

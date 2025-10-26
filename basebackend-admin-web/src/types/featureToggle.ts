/**
 * 特性开关相关类型定义
 */

export interface FeatureCheckResponse {
  featureName: string;
  enabled: boolean;
  provider: string;
}

export interface FeatureBatchCheckRequest {
  featureNames: string[];
  userId?: string;
  username?: string;
  email?: string;
}

export interface VariantResponse {
  featureName: string;
  variantName: string;
  enabled: boolean;
  payload?: string;
}

export interface FeatureToggleStatus {
  available: boolean;
  provider: string;
  message: string;
}

export interface FeatureContext {
  userId?: string;
  username?: string;
  email?: string;
  properties?: Record<string, string>;
}

export interface FeatureToggle {
  name: string;
  enabled: boolean;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

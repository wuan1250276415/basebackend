//package com.basebackend.admin.controller;
//
//import com.basebackend.common.web.ResponseResult;
//import com.basebackend.featuretoggle.model.FeatureContext;
//import com.basebackend.featuretoggle.model.Variant;
//import com.basebackend.featuretoggle.service.FeatureToggleService;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 特性开关管理控制器
// * 提供特性开关查询和管理接口
// *
// * @author BaseBackend
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/feature-toggles")
//@ConditionalOnBean(FeatureToggleService.class)
//public class FeatureToggleController {
//
//    @Autowired(required = false)
//    private FeatureToggleService featureToggleService;
//
//    /**
//     * 检查单个特性是否启用
//     */
//    @GetMapping("/check/{featureName}")
//    public ResponseResult<FeatureCheckResponse> checkFeature(
//            @PathVariable String featureName,
//            @RequestParam(required = false) String userId,
//            @RequestParam(required = false) String username,
//            @RequestParam(required = false) String email) {
//
//        if (featureToggleService == null) {
//            return ResponseResult.error("Feature toggle service is not available");
//        }
//
//        FeatureContext context = buildContext(userId, username, email);
//        boolean enabled = featureToggleService.isEnabled(featureName, context, false);
//
//        FeatureCheckResponse response = new FeatureCheckResponse();
//        response.setFeatureName(featureName);
//        response.setEnabled(enabled);
//        response.setProvider(featureToggleService.getProviderName());
//
//        return ResponseResult.success(response);
//    }
//
//    /**
//     * 批量检查多个特性
//     */
//    @PostMapping("/check-batch")
//    public ResponseResult<Map<String, Boolean>> checkFeaturesBatch(
//            @RequestBody FeatureBatchCheckRequest request) {
//
//        if (featureToggleService == null) {
//            return ResponseResult.error("Feature toggle service is not available");
//        }
//
//        FeatureContext context = buildContext(
//                request.getUserId(),
//                request.getUsername(),
//                request.getEmail()
//        );
//
//        Map<String, Boolean> result = new HashMap<>();
//        for (String featureName : request.getFeatureNames()) {
//            boolean enabled = featureToggleService.isEnabled(featureName, context, false);
//            result.put(featureName, enabled);
//        }
//
//        return ResponseResult.success(result);
//    }
//
//    /**
//     * 获取所有特性开关状态
//     */
//    @GetMapping("/all")
//    public ResponseResult<Map<String, Boolean>> getAllFeatures(
//            @RequestParam(required = false) String userId,
//            @RequestParam(required = false) String username,
//            @RequestParam(required = false) String email) {
//
//        if (featureToggleService == null) {
//            return ResponseResult.error("Feature toggle service is not available");
//        }
//
//        FeatureContext context = buildContext(userId, username, email);
//        Map<String, Boolean> features = featureToggleService.getAllFeatureStates(context);
//
//        return ResponseResult.success(features);
//    }
//
//    /**
//     * 获取变体信息（用于AB测试）
//     */
//    @GetMapping("/variant/{featureName}")
//    public ResponseResult<VariantResponse> getVariant(
//            @PathVariable String featureName,
//            @RequestParam(required = false) String userId,
//            @RequestParam(required = false) String username,
//            @RequestParam(required = false) String email) {
//
//        if (featureToggleService == null) {
//            return ResponseResult.error("Feature toggle service is not available");
//        }
//
//        FeatureContext context = buildContext(userId, username, email);
//        Variant variant = featureToggleService.getVariant(featureName, context);
//
//        VariantResponse response = new VariantResponse();
//        response.setFeatureName(featureName);
//        response.setVariantName(variant.getName());
//        response.setEnabled(variant.getEnabled());
//        response.setPayload(variant.getPayload());
//
//        return ResponseResult.success(response);
//    }
//
//    /**
//     * 获取服务状态
//     */
//    @GetMapping("/status")
//    public ResponseResult<FeatureToggleStatus> getStatus() {
//        FeatureToggleStatus status = new FeatureToggleStatus();
//
//        if (featureToggleService == null) {
//            status.setAvailable(false);
//            status.setProvider("None");
//            status.setMessage("Feature toggle service is not configured");
//        } else {
//            status.setAvailable(featureToggleService.isAvailable());
//            status.setProvider(featureToggleService.getProviderName());
//            status.setMessage("Feature toggle service is running");
//        }
//
//        return ResponseResult.success(status);
//    }
//
//    /**
//     * 刷新特性开关配置
//     */
//    @PostMapping("/refresh")
//    public ResponseResult<Void> refresh() {
//        if (featureToggleService == null) {
//            return ResponseResult.error("Feature toggle service is not available");
//        }
//
//        try {
//            featureToggleService.refresh();
//            return ResponseResult.success();
//        } catch (Exception e) {
//            log.error("Failed to refresh feature toggles", e);
//            return ResponseResult.error("Failed to refresh: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 构建特性上下文
//     */
//    private FeatureContext buildContext(String userId, String username, String email) {
//        return FeatureContext.builder()
//                .userId(userId)
//                .username(username)
//                .email(email)
//                .build();
//    }
//
//    // ========== DTO类 ==========
//
//    @Data
//    public static class FeatureCheckResponse {
//        private String featureName;
//        private Boolean enabled;
//        private String provider;
//    }
//
//    @Data
//    public static class FeatureBatchCheckRequest {
//        private List<String> featureNames;
//        private String userId;
//        private String username;
//        private String email;
//    }
//
//    @Data
//    public static class VariantResponse {
//        private String featureName;
//        private String variantName;
//        private Boolean enabled;
//        private String payload;
//    }
//
//    @Data
//    public static class FeatureToggleStatus {
//        private Boolean available;
//        private String provider;
//        private String message;
//    }
//}

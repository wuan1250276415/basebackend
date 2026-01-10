package com.basebackend.scheduler.camunda.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微服务调用服务任务委托
 *
 * <p>
 * 功能说明：
 * <ul>
 * <li>通过HTTP调用外部微服务</li>
 * <li>支持配置化的重试机制</li>
 * <li>记录调用结果和响应时间</li>
 * <li>失败时抛出业务异常（BpmnError）</li>
 * </ul>
 *
 * <p>
 * 输入变量：
 * <ul>
 * <li>remoteService (String, required): 服务名称或基础URL</li>
 * <li>remotePath (String, required): 接口路径</li>
 * <li>remoteMethod (String, optional): HTTP方法，默认POST</li>
 * <li>remoteBody (Map, optional): 请求体</li>
 * <li>remoteHeaders (Map, optional): 自定义请求头</li>
 * <li>remoteMaxAttempts (Integer, optional): 最大重试次数，默认3次</li>
 * <li>remoteRetryDelaySeconds (Integer, optional): 重试延迟秒数，默认2秒</li>
 * </ul>
 *
 * <p>
 * 输出变量：
 * <ul>
 * <li>remoteCallStatus (String): 调用状态 SUCCESS/FAILED</li>
 * <li>remoteCallResult (Map): 调用结果</li>
 * <li>remoteCallError (String): 错误消息（失败时）</li>
 * <li>remoteCallAttempts (Integer): 实际尝试次数</li>
 * <li>remoteCallDurationMs (Long): 调用耗时（毫秒）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component("microserviceCallDelegate")
@RequiredArgsConstructor
public class MicroserviceCallDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        log.info("MicroserviceCallDelegate started, processInstanceId={}, activityId={}",
                processInstanceId, activityId);

        long startTime = System.currentTimeMillis();

        try {
            // 获取并验证必需的流程变量
            String serviceName = (String) execution.getVariable("remoteService");
            String path = (String) execution.getVariable("remotePath");
            String method = (String) execution.getVariable("remoteMethod");

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) execution.getVariable("remoteBody");

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) execution.getVariable("remoteHeaders");

            validateRequiredVariables(serviceName, path);

            // 获取重试配置
            Boolean useNativeRetry = (Boolean) execution.getVariable("remoteUseNativeRetry");
            if (useNativeRetry == null) {
                useNativeRetry = false; // 默认为 false，保持向后兼容
            }

            Integer maxAttempts = (Integer) execution.getVariable("remoteMaxAttempts");
            Integer retryDelaySeconds = (Integer) execution.getVariable("remoteRetryDelaySeconds");

            // 如果使用原生重试，强制内部重试次数为 1，交由引擎处理
            int maxRetries = useNativeRetry ? 1 : (maxAttempts != null ? maxAttempts : 3);
            long retryDelay = retryDelaySeconds != null ? retryDelaySeconds * 1000L : 2000L;
            HttpMethod httpMethod = parseHttpMethod(method);

            // 执行微服务调用（带重试）
            Map<String, Object> result = callServiceWithRetry(
                    serviceName,
                    path,
                    httpMethod,
                    body,
                    headers,
                    maxRetries,
                    retryDelay,
                    execution);

            // 记录成功状态
            long duration = System.currentTimeMillis() - startTime;
            execution.setVariable("remoteCallStatus", "SUCCESS");
            execution.setVariable("remoteCallResult", result);
            execution.setVariable("remoteCallDurationMs", duration);

            log.info(
                    "Microservice call completed successfully, processInstanceId={}, service={}, path={}, duration={}ms",
                    processInstanceId, serviceName, path, duration);

        } catch (IllegalArgumentException ex) {
            // 参数验证失败
            log.error("Microservice call validation failed, processInstanceId={}, error={}",
                    processInstanceId, ex.getMessage());

            execution.setVariable("remoteCallStatus", "FAILED");
            execution.setVariable("remoteCallError", ex.getMessage());
            execution.setVariable("remoteCallAttempts", 0);

            throw new BpmnError("REMOTE_CALL_VALIDATION_ERROR", ex.getMessage());

        } catch (Exception ex) {
            // 调用失败
            log.error("Microservice call failed, processInstanceId={}", processInstanceId, ex);

            execution.setVariable("remoteCallStatus", "FAILED");
            execution.setVariable("remoteCallError", "微服务调用失败: " + ex.getMessage());

            // 检查是否启用了原生重试
            Boolean useNativeRetry = (Boolean) execution.getVariable("remoteUseNativeRetry");
            if (Boolean.TRUE.equals(useNativeRetry)) {
                // 原生重试模式：抛出 RuntimeException 以触发外部 Job 重试
                // (注意：这里不应抛出 BpmnError，否则会直接进入错误处理流程而不是重试)
                throw new RuntimeException("Microservice call failed (Native Retry): " + ex.getMessage(), ex);
            } else {
                // 传统模式：捕获所有异常并转换为 BpmnError，由流程捕获或结束
                throw new BpmnError("REMOTE_CALL_ERROR", "微服务调用失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 验证必需的流程变量
     */
    private void validateRequiredVariables(String serviceName, String path) {
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalArgumentException("服务名称不能为空（remoteService）");
        }
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("接口路径不能为空（remotePath）");
        }
    }

    /**
     * 解析HTTP方法
     */
    private HttpMethod parseHttpMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return HttpMethod.POST;
        }

        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的HTTP方法: " + method);
        }
    }

    /**
     * 带重试机制的微服务调用
     */
    private Map<String, Object> callServiceWithRetry(
            String serviceName,
            String path,
            HttpMethod method,
            Map<String, Object> body,
            Map<String, String> customHeaders,
            int maxAttempts,
            long retryDelay,
            DelegateExecution execution) throws InterruptedException {

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            execution.setVariable("remoteCallAttempts", attempt);

            try {
                log.info("Microservice call attempt {}/{}, service={}, path={}, method={}, processInstanceId={}",
                        attempt, maxAttempts, serviceName, path, method, execution.getProcessInstanceId());

                // 执行实际的HTTP调用
                Map<String, Object> result = performHttpCall(
                        serviceName, path, method, body, customHeaders);

                // 构建成功结果
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("attempts", attempt);
                response.put("data", result);
                response.put("service", serviceName);
                response.put("path", path);
                response.put("method", method.name());

                return response;

            } catch (Exception ex) {
                lastException = ex;
                log.warn("Microservice call attempt {}/{} failed, service={}, path={}, error={}",
                        attempt, maxAttempts, serviceName, path, ex.getMessage());

                if (attempt < maxAttempts) {
                    log.info("Retrying after {} ms...", retryDelay);
                    Thread.sleep(retryDelay);
                }
            }
        }

        // 所有重试都失败
        throw new RuntimeException("微服务调用在 " + maxAttempts + " 次尝试后失败", lastException);
    }

    /**
     * 执行实际的HTTP调用
     */
    private Map<String, Object> performHttpCall(
            String serviceName,
            String path,
            HttpMethod method,
            Map<String, Object> body,
            Map<String, String> customHeaders) {

        // 构建完整URL
        String url = buildUrl(serviceName, path);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (customHeaders != null) {
            customHeaders.forEach(headers::add);
        }

        // 构建请求实体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 执行HTTP调用
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                method,
                requestEntity,
                Map.class);

        // 构建结果
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCodeValue());
        // Spring 6.x 中 HttpStatusCode 没有 getReasonPhrase()，改用 toString()
        result.put("statusText", response.getStatusCode().toString());
        result.put("body", response.getBody());
        result.put("timestamp", LocalDateTime.now().toString());

        return result;
    }

    /**
     * 构建完整URL
     */
    private String buildUrl(String serviceName, String path) {
        // 如果serviceName是完整URL，直接拼接path
        if (serviceName.startsWith("http://") || serviceName.startsWith("https://")) {
            return serviceName + (path.startsWith("/") ? path : "/" + path);
        }

        // 否则假设是服务名，构建内部服务URL
        // 实际项目中应该从服务发现获取服务地址
        return "http://" + serviceName + (path.startsWith("/") ? path : "/" + path);
    }
}

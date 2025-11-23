package com.basebackend.scheduler.camunda.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据同步服务任务委托
 *
 * <p>功能说明：
 * <ul>
 *   <li>支持多种数据源类型的同步操作</li>
 *   <li>支持配置化的失败重试机制</li>
 *   <li>记录同步状态和详细结果</li>
 *   <li>失败时抛出业务异常（BpmnError）</li>
 * </ul>
 *
 * <p>输入变量：
 * <ul>
 *   <li>syncSourceType (String, required): 数据源类型（database/api/file）</li>
 *   <li>syncPayload (Map, required): 同步数据负载</li>
 *   <li>syncMaxRetries (Integer, optional): 最大重试次数，默认3次</li>
 *   <li>syncRetryDelaySeconds (Integer, optional): 重试延迟秒数，默认5秒</li>
 * </ul>
 *
 * <p>输出变量：
 * <ul>
 *   <li>syncStatus (String): 同步状态 SUCCESS/FAILED</li>
 *   <li>syncResult (Map): 同步结果数据</li>
 *   <li>syncErrorMessage (String): 错误消息（失败时）</li>
 *   <li>syncAttempts (Integer): 实际尝试次数</li>
 *   <li>syncCompletedTime (String): 完成时间</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component("dataSyncDelegate")
@RequiredArgsConstructor
public class DataSyncDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        log.info("DataSyncDelegate started, processInstanceId={}, activityId={}",
                processInstanceId, activityId);

        try {
            // 获取并验证必需的流程变量
            String sourceType = (String) execution.getVariable("syncSourceType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) execution.getVariable("syncPayload");

            validateRequiredVariables(sourceType, payload);

            // 获取可选的重试配置
            Integer maxRetries = (Integer) execution.getVariable("syncMaxRetries");
            Integer retryDelaySeconds = (Integer) execution.getVariable("syncRetryDelaySeconds");

            int maxAttempts = maxRetries != null ? maxRetries : 3;
            long retryDelay = retryDelaySeconds != null ? retryDelaySeconds * 1000L : 5000L;

            // 执行数据同步（带重试）
            Map<String, Object> result = syncDataWithRetry(
                    sourceType,
                    payload,
                    maxAttempts,
                    retryDelay,
                    execution
            );

            // 记录成功状态
            String completedTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            execution.setVariable("syncStatus", "SUCCESS");
            execution.setVariable("syncResult", result);
            execution.setVariable("syncCompletedTime", completedTime);

            log.info("Data sync completed successfully, processInstanceId={}, sourceType={}, attempts={}",
                    processInstanceId, sourceType, result.get("attempts"));

        } catch (IllegalArgumentException ex) {
            // 参数验证失败
            log.error("Data sync validation failed, processInstanceId={}, error={}",
                    processInstanceId, ex.getMessage());

            execution.setVariable("syncStatus", "FAILED");
            execution.setVariable("syncErrorMessage", ex.getMessage());
            execution.setVariable("syncAttempts", 0);

            throw new BpmnError("SYNC_VALIDATION_ERROR", ex.getMessage());

        } catch (Exception ex) {
            // 同步失败
            log.error("Data sync failed, processInstanceId={}", processInstanceId, ex);

            execution.setVariable("syncStatus", "FAILED");
            execution.setVariable("syncErrorMessage", "数据同步失败: " + ex.getMessage());

            throw new BpmnError("SYNC_ERROR", "数据同步失败: " + ex.getMessage());
        }
    }

    /**
     * 验证必需的流程变量
     */
    private void validateRequiredVariables(String sourceType, Map<String, Object> payload) {
        if (!StringUtils.hasText(sourceType)) {
            throw new IllegalArgumentException("数据源类型不能为空（syncSourceType）");
        }
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("同步数据负载不能为空（syncPayload）");
        }

        // 验证支持的数据源类型
        if (!sourceType.matches("(?i)(database|api|file)")) {
            throw new IllegalArgumentException("不支持的数据源类型: " + sourceType +
                    "，支持的类型：database, api, file");
        }
    }

    /**
     * 带重试机制的数据同步
     */
    private Map<String, Object> syncDataWithRetry(
            String sourceType,
            Map<String, Object> payload,
            int maxAttempts,
            long retryDelay,
            DelegateExecution execution) throws InterruptedException {

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            execution.setVariable("syncAttempts", attempt);

            try {
                log.info("Data sync attempt {}/{}, sourceType={}, processInstanceId={}",
                        attempt, maxAttempts, sourceType, execution.getProcessInstanceId());

                // 执行实际的同步操作
                Map<String, Object> syncResult = performSync(sourceType, payload);

                // 构建成功结果
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("attempts", attempt);
                result.put("data", syncResult);
                result.put("sourceType", sourceType);

                return result;

            } catch (Exception ex) {
                lastException = ex;
                log.warn("Data sync attempt {}/{} failed, sourceType={}, error={}",
                        attempt, maxAttempts, sourceType, ex.getMessage());

                if (attempt < maxAttempts) {
                    log.info("Retrying after {} ms...", retryDelay);
                    Thread.sleep(retryDelay);
                }
            }
        }

        // 所有重试都失败
        throw new RuntimeException("数据同步在 " + maxAttempts + " 次尝试后失败", lastException);
    }

    /**
     * 执行实际的数据同步操作
     *
     * 这是一个示例实现，实际项目中应该注入具体的数据同步服务
     */
    private Map<String, Object> performSync(String sourceType, Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();

        switch (sourceType.toLowerCase()) {
            case "database":
                result.put("type", "database");
                result.put("recordsProcessed", payload.size());
                result.put("message", "数据库同步完成");
                break;

            case "api":
                result.put("type", "api");
                result.put("endpoint", payload.get("endpoint"));
                result.put("message", "API调用完成");
                break;

            case "file":
                result.put("type", "file");
                result.put("fileName", payload.get("fileName"));
                result.put("message", "文件同步完成");
                break;

            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
        }

        // 模拟数据同步操作
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("payload", payload);

        return result;
    }
}

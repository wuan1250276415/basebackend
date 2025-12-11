package com.basebackend.scheduler.exception;

import com.basebackend.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 调度器模块错误码枚举
 * <p>
 * 定义调度器模块的业务错误码，范围：3000-3999（业务模块自定义段）。
 * 所有调度相关的错误都应使用此枚举，确保错误码唯一性和可追溯性。
 * </p>
 *
 * <h3>错误码段划分：</h3>
 * <ul>
 *   <li><b>3000-3099</b>: 通用调度错误（参数错误、资源不存在、状态冲突等）</li>
 *   <li><b>3100-3199</b>: Camunda工作流引擎错误（流程定义、流程实例、任务等）</li>
 *   <li><b>3200-3299</b>: PowerJob调度引擎错误（任务分发、Worker管理等）</li>
 *   <li><b>3300-3399</b>: DAG工作流引擎错误（DAG解析、节点执行等）</li>
 *   <li><b>3400-3499</b>: 表单引擎错误（表单模板、表单渲染等）</li>
 *   <li><b>3500-3599</b>: 调度策略错误（时间表达式、重试策略、幂等性等）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public enum SchedulerErrorCode implements ErrorCode {

    // ========== 通用调度错误（3000-3099）==========

    /**
     * 请求参数无效
     */
    INVALID_ARGUMENT(3000, "请求参数无效", 400),

    /**
     * 资源不存在
     */
    NOT_FOUND(3001, "请求的资源不存在", 404),

    /**
     * 资源状态冲突
     */
    CONFLICT(3002, "资源状态冲突，操作不允许", 409),

    /**
     * 并发操作冲突
     */
    CONCURRENT_CONFLICT(3003, "并发操作冲突，请重试", 409),

    /**
     * 幂等性键冲突
     */
    IDEMPOTENCY_VIOLATION(3004, "重复请求，操作已执行", 409),

    /**
     * 资源已存在
     */
    ALREADY_EXISTS(3005, "资源已存在", 409),

    // ========== Camunda工作流引擎错误（3100-3199）==========

    /**
     * 工作流引擎内部错误
     */
    ENGINE_ERROR(3100, "工作流引擎内部错误", 500),

    /**
     * 流程定义不存在
     */
    PROCESS_DEFINITION_NOT_FOUND(3101, "流程定义不存在", 404),

    /**
     * 流程定义已存在
     */
    PROCESS_DEFINITION_ALREADY_EXISTS(3102, "流程定义已存在", 409),

    /**
     * 流程定义部署失败
     */
    PROCESS_DEFINITION_DEPLOY_FAILED(3103, "流程定义部署失败", 500),

    /**
     * 流程定义解析失败
     */
    PROCESS_DEFINITION_PARSE_ERROR(3104, "流程定义解析失败", 400),

    /**
     * 流程实例不存在
     */
    PROCESS_INSTANCE_NOT_FOUND(3110, "流程实例不存在", 404),

    /**
     * 流程实例启动失败
     */
    PROCESS_INSTANCE_START_FAILED(3111, "流程实例启动失败", 500),

    /**
     * 流程实例状态异常
     */
    PROCESS_INSTANCE_STATE_ERROR(3112, "流程实例状态异常", 409),

    /**
     * 任务不存在
     */
    TASK_NOT_FOUND(3120, "任务不存在", 404),

    /**
     * 任务已完成
     */
    TASK_ALREADY_COMPLETED(3121, "任务已完成，无法操作", 409),

    /**
     * 任务认领失败
     */
    TASK_CLAIM_FAILED(3122, "任务认领失败", 409),

    /**
     * 任务委派失败
     */
    TASK_DELEGATE_FAILED(3123, "任务委派失败", 409),

    /**
     * 任务分配失败
     */
    TASK_ASSIGN_FAILED(3124, "任务分配失败", 409),

    /**
     * 任务超时
     */
    TASK_TIMEOUT(3125, "任务执行超时", 504),

    /**
     * 流程定义版本冲突
     */
    PROCESS_DEFINITION_VERSION_CONFLICT(3105, "流程定义版本冲突", 409),

    /**
     * 流程实例暂停失败
     */
    PROCESS_INSTANCE_SUSPEND_FAILED(3113, "流程实例暂停失败", 500),

    /**
     * 流程实例激活失败
     */
    PROCESS_INSTANCE_ACTIVATE_FAILED(3114, "流程实例激活失败", 500),

    /**
     * 流程变量操作失败
     */
    PROCESS_VARIABLE_ERROR(3115, "流程变量操作失败", 500),

    /**
     * 历史数据查询失败
     */
    HISTORY_QUERY_FAILED(3116, "历史数据查询失败", 500),

    // ========== PowerJob调度引擎错误（3200-3299）==========

    /**
     * 任务分发失败
     */
    DISPATCH_ERROR(3200, "任务分发失败", 500),

    /**
     * Worker节点不可用
     */
    WORKER_UNAVAILABLE(3201, "Worker节点不可用", 503),

    /**
     * 任务执行超时
     */
    TASK_EXECUTION_TIMEOUT(3202, "任务执行超时", 504),

    /**
     * 任务执行失败
     */
    TASK_EXECUTION_FAILED(3203, "任务执行失败", 500),

    /**
     * 任务取消失败
     */
    TASK_CANCEL_FAILED(3204, "任务取消失败", 500),

    /**
     * Job注册失败
     */
    JOB_REGISTER_FAILED(3205, "Job注册失败", 500),

    /**
     * Job更新失败
     */
    JOB_UPDATE_FAILED(3206, "Job更新失败", 500),

    /**
     * Job删除失败
     */
    JOB_DELETE_FAILED(3207, "Job删除失败", 500),

    /**
     * Job不存在
     */
    JOB_NOT_FOUND(3208, "Job不存在", 404),

    /**
     * Worker不在线
     */
    WORKER_OFFLINE(3209, "Worker节点不在线", 503),

    /**
     * 任务重试耗尽
     */
    TASK_RETRY_EXHAUSTED(3210, "任务重试次数已用尽", 500),

    /**
     * 任务结果查询失败
     */
    TASK_RESULT_QUERY_FAILED(3211, "任务结果查询失败", 500),

    /**
     * 任务实例不存在
     */
    TASK_INSTANCE_NOT_FOUND(3212, "任务实例不存在", 404),

    /**
     * 调度规则无效
     */
    SCHEDULE_RULE_INVALID(3213, "调度规则无效", 400),

    // ========== DAG工作流引擎错误（3300-3399）==========

    /**
     * DAG定义解析失败
     */
    DAG_PARSE_ERROR(3300, "DAG定义解析失败", 400),

    /**
     * DAG存在环形依赖
     */
    DAG_CIRCULAR_DEPENDENCY(3301, "DAG存在环形依赖", 400),

    /**
     * DAG节点不存在
     */
    DAG_NODE_NOT_FOUND(3302, "DAG节点不存在", 404),

    /**
     * DAG节点执行失败
     */
    DAG_NODE_EXECUTION_FAILED(3303, "DAG节点执行失败", 500),

    // ========== 表单引擎错误（3400-3499）==========

    /**
     * 表单模板不存在
     */
    FORM_TEMPLATE_NOT_FOUND(3400, "表单模板不存在", 404),

    /**
     * 表单模板已存在
     */
    FORM_TEMPLATE_ALREADY_EXISTS(3401, "表单模板已存在", 409),

    /**
     * 表单模板解析失败
     */
    FORM_TEMPLATE_PARSE_ERROR(3402, "表单模板解析失败", 400),

    /**
     * 表单数据校验失败
     */
    FORM_DATA_VALIDATION_FAILED(3403, "表单数据校验失败", 400),

    /**
     * 表单渲染失败
     */
    FORM_RENDER_FAILED(3404, "表单渲染失败", 500),

    // ========== 调度策略错误（3500-3599）==========

    /**
     * 时间表达式无效
     */
    TIME_EXPRESSION_ERROR(3500, "时间表达式无效或不支持", 400),

    /**
     * Cron表达式解析失败
     */
    CRON_PARSE_ERROR(3501, "Cron表达式解析失败", 400),

    /**
     * 状态转换非法
     */
    STATE_TRANSITION_ERROR(3502, "状态转换非法", 409),

    /**
     * 重试策略无效
     */
    RETRY_POLICY_INVALID(3503, "重试策略无效", 400),

    /**
     * 序列化失败
     */
    SERIALIZATION_ERROR(3504, "数据序列化失败", 500),

    /**
     * 反序列化失败
     */
    DESERIALIZATION_ERROR(3505, "数据反序列化失败", 500);

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * HTTP状态码
     */
    private final Integer httpStatus;

    /**
     * 构造函数（使用默认HTTP状态码）
     *
     * @param code    错误码
     * @param message 错误消息
     */
    SchedulerErrorCode(Integer code, String message) {
        this(code, message, null);
    }

    /**
     * 构造函数（指定HTTP状态码）
     *
     * @param code       错误码
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    SchedulerErrorCode(Integer code, String message, Integer httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 获取HTTP状态码
     * <p>
     * 如果构造时指定了HTTP状态码，则直接返回；
     * 否则使用父接口的默认推导规则。
     * </p>
     *
     * @return HTTP状态码
     */
    @Override
    public int getHttpStatus() {
        if (httpStatus != null) {
            return httpStatus;
        }
        return ErrorCode.super.getHttpStatus();
    }

    /**
     * 获取错误所属模块
     *
     * @return 模块标识 "scheduler"
     */
    @Override
    public String getModule() {
        return "scheduler";
    }
}

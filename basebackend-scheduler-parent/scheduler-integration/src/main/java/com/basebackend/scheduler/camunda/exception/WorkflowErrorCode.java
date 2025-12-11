package com.basebackend.scheduler.camunda.exception;

/**
 * 工作流错误码枚举
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public enum WorkflowErrorCode {

    /**
     * 流程定义不存在
     */
    PROCESS_DEFINITION_NOT_FOUND("PROCESS_DEFINITION_404", "流程定义不存在"),

    /**
     * 流程实例不存在
     */
    PROCESS_INSTANCE_NOT_FOUND("PROCESS_INSTANCE_404", "流程实例不存在"),

    /**
     * 任务不存在
     */
    TASK_NOT_FOUND("TASK_404", "任务不存在"),

    /**
     * 表单模板不存在
     */
    FORM_TEMPLATE_NOT_FOUND("FORM_TEMPLATE_404", "表单模板不存在"),

    /**
     * 表单模板编码已存在
     */
    FORM_TEMPLATE_CODE_EXISTS("FORM_TEMPLATE_409", "表单模板编码已存在"),

    /**
     * 工作流引擎异常
     */
    WORKFLOW_ENGINE_ERROR("WORKFLOW_ENGINE_500", "工作流引擎异常"),

    /**
     * BPMN 部署失败
     */
    BPMN_DEPLOYMENT_FAILED("BPMN_DEPLOY_500", "BPMN 部署失败"),

    /**
     * 参数验证失败
     */
    VALIDATION_ERROR("VALIDATION_400", "参数验证失败"),

    /**
     * 数据库异常
     */
    DATABASE_ERROR("DATABASE_500", "数据库操作异常"),

    /**
     * 网络请求异常
     */
    NETWORK_ERROR("NETWORK_500", "网络请求异常"),

    /**
     * 未授权访问
     */
    UNAUTHORIZED("UNAUTHORIZED_401", "未授权访问"),

    /**
     * 禁止访问
     */
    FORBIDDEN("FORBIDDEN_403", "禁止访问"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_500", "服务器内部错误");

    /**
     * 错误代码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param code    错误代码
     * @param message 错误消息
     */
    WorkflowErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 根据错误代码获取错误消息
     *
     * @param code 错误代码
     * @return 错误消息
     */
    public static String getMessageByCode(String code) {
        for (WorkflowErrorCode errorCode : WorkflowErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode.getMessage();
            }
        }
        return "未知错误";
    }
}

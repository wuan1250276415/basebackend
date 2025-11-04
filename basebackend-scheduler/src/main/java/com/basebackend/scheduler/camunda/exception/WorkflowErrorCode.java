package com.basebackend.scheduler.camunda.exception;

/**
 * 工作流错误码枚举
 */
public enum WorkflowErrorCode {

    // 流程定义相关错误 (8000-8099)
    PROCESS_DEFINITION_NOT_FOUND(8000, "流程定义不存在"),
    PROCESS_DEFINITION_DEPLOYMENT_FAILED(8001, "流程定义部署失败"),
    PROCESS_DEFINITION_SUSPEND_FAILED(8002, "流程定义挂起失败"),
    PROCESS_DEFINITION_ACTIVATE_FAILED(8003, "流程定义激活失败"),
    PROCESS_DEFINITION_DELETE_FAILED(8004, "流程定义删除失败"),

    // 流程实例相关错误 (8100-8199)
    PROCESS_INSTANCE_NOT_FOUND(8100, "流程实例不存在"),
    PROCESS_INSTANCE_START_FAILED(8101, "流程实例启动失败"),
    PROCESS_INSTANCE_SUSPEND_FAILED(8102, "流程实例挂起失败"),
    PROCESS_INSTANCE_ACTIVATE_FAILED(8103, "流程实例激活失败"),
    PROCESS_INSTANCE_DELETE_FAILED(8104, "流程实例删除失败"),

    // 任务相关错误 (8200-8299)
    TASK_NOT_FOUND(8200, "任务不存在"),
    TASK_COMPLETE_FAILED(8201, "任务完成失败"),
    TASK_CLAIM_FAILED(8202, "任务认领失败"),
    TASK_UNCLAIM_FAILED(8203, "任务取消认领失败"),
    TASK_DELEGATE_FAILED(8204, "任务委派失败"),
    TASK_ASSIGN_FAILED(8205, "任务转办失败"),

    // 表单模板相关错误 (8300-8399)
    FORM_TEMPLATE_NOT_FOUND(8300, "表单模板不存在"),
    FORM_TEMPLATE_ALREADY_EXISTS(8301, "表单模板已存在"),
    FORM_TEMPLATE_CREATE_FAILED(8302, "表单模板创建失败"),
    FORM_TEMPLATE_UPDATE_FAILED(8303, "表单模板更新失败"),
    FORM_TEMPLATE_DELETE_FAILED(8304, "表单模板删除失败"),
    FORM_TEMPLATE_INVALID_SCHEMA(8305, "表单模板Schema无效"),

    // 通用错误 (8900-8999)
    WORKFLOW_INTERNAL_ERROR(8900, "工作流内部错误"),
    WORKFLOW_INVALID_PARAMETER(8901, "无效的参数"),
    WORKFLOW_PERMISSION_DENIED(8902, "权限不足"),
    WORKFLOW_OPERATION_NOT_ALLOWED(8903, "不允许的操作");

    private final int code;
    private final String message;

    WorkflowErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.AttachmentDTO;
import com.basebackend.scheduler.camunda.dto.AttachmentRequest;
import com.basebackend.scheduler.camunda.dto.ClaimTaskRequest;
import com.basebackend.scheduler.camunda.dto.CommentDTO;
import com.basebackend.scheduler.camunda.dto.CommentRequest;
import com.basebackend.scheduler.camunda.dto.CompleteTaskRequest;
import com.basebackend.scheduler.camunda.dto.DelegateTaskRequest;
import com.basebackend.scheduler.camunda.dto.TaskDetailDTO;
import com.basebackend.scheduler.camunda.dto.TaskPageQuery;
import com.basebackend.scheduler.camunda.dto.TaskSummaryDTO;
import com.basebackend.scheduler.camunda.dto.VariableUpsertRequest;

import java.util.List;
import java.util.Map;

/**
 * 任务管理业务逻辑接口
 *
 * <p>
 * 提供任务相关的业务逻辑封装，包括：
 * <ul>
 * <li>任务查询（分页、详情）</li>
 * <li>任务操作（认领、释放、完成任务、委托）</li>
 * <li>任务变量管理（获取、设置、删除）</li>
 * <li>任务附件管理（查询、添加）</li>
 * <li>任务评论管理（查询、添加）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface TaskManagementService {

    /**
     * 分页查询任务
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<TaskSummaryDTO> page(TaskPageQuery query);

    /**
     * 获取任务详情
     *
     * @param taskId        任务 ID
     * @param withVariables 是否包含变量
     * @return 任务详情
     */
    TaskDetailDTO detail(String taskId, boolean withVariables);

    /**
     * 认领任务
     *
     * @param taskId  任务 ID
     * @param request 认领请求参数
     */
    void claim(String taskId, ClaimTaskRequest request);

    /**
     * 释放任务
     *
     * @param taskId 任务 ID
     */
    void unclaim(String taskId);

    /**
     * 完成任务
     *
     * @param taskId  任务 ID
     * @param request 完成任务请求参数
     */
    void complete(String taskId, CompleteTaskRequest request);

    /**
     * 委托任务
     *
     * @param taskId  任务 ID
     * @param request 委托请求参数
     */
    void delegate(String taskId, DelegateTaskRequest request);

    /**
     * 获取任务变量
     *
     * @param taskId 任务 ID
     * @param local  是否本地变量
     * @return 变量集合
     */
    Map<String, Object> variables(String taskId, boolean local);

    /**
     * 设置任务变量
     *
     * @param taskId  任务 ID
     * @param request 变量设置请求
     */
    void upsertVariable(String taskId, VariableUpsertRequest request);

    /**
     * 删除任务变量
     *
     * @param taskId 任务 ID
     * @param key    变量名
     * @param local  是否本地变量
     */
    void deleteVariable(String taskId, String key, boolean local);

    /**
     * 查询任务附件
     *
     * @param taskId 任务 ID
     * @return 附件列表
     */
    List<AttachmentDTO> attachments(String taskId);

    /**
     * 添加任务附件
     *
     * @param taskId  任务 ID
     * @param request 附件添加请求
     * @return 附件信息
     */
    AttachmentDTO addAttachment(String taskId, AttachmentRequest request);

    /**
     * 查询任务评论
     *
     * @param taskId 任务 ID
     * @return 评论列表
     */
    List<CommentDTO> comments(String taskId);

    /**
     * 添加任务评论
     *
     * @param taskId  任务 ID
     * @param request 评论添加请求
     * @return 评论信息
     */
    CommentDTO addComment(String taskId, CommentRequest request);

    /**
     * 查询任务评论（兼容性方法，getComments的别名）
     *
     * @param taskId 任务 ID
     * @return 评论列表
     */
    default List<CommentDTO> getComments(String taskId) {
        return comments(taskId);
    }

    // ========== 超时任务查询 ==========

    /**
     * 查询已超时任务
     *
     * <p>
     * 查询截止时间已过的任务列表
     *
     * @param query 分页查询参数
     * @return 超时任务分页结果
     */
    PageResult<TaskSummaryDTO> listOverdueTasks(TaskPageQuery query);

    /**
     * 查询即将超时任务
     *
     * <p>
     * 查询截止时间在指定时间范围内的任务
     *
     * @param query         分页查询参数
     * @param hoursUntilDue 距离超时的小时数
     * @return 即将超时任务分页结果
     */
    PageResult<TaskSummaryDTO> listDueSoonTasks(TaskPageQuery query, int hoursUntilDue);

    /**
     * 统计超时任务数量
     *
     * @return 超时任务数量
     */
    long countOverdueTasks();

    /**
     * 统计即将超时任务数量
     *
     * @param hoursUntilDue 距离超时的小时数
     * @return 即将超时任务数量
     */
    long countDueSoonTasks(int hoursUntilDue);

    // ========== 批量任务操作 ==========

    /**
     * 批量完成任务
     *
     * @param taskIds   任务 ID 列表
     * @param variables 流程变量（可选）
     * @return 成功完成的任务数量
     */
    int batchComplete(List<String> taskIds, Map<String, Object> variables);

    /**
     * 批量认领任务
     *
     * @param taskIds 任务 ID 列表
     * @param userId  用户 ID
     * @return 成功认领的任务数量
     */
    int batchClaim(List<String> taskIds, String userId);

    /**
     * 批量委派任务
     *
     * @param taskIds 任务 ID 列表
     * @param userId  被委派人 ID
     * @return 成功委派的任务数量
     */
    int batchDelegate(List<String> taskIds, String userId);

    // ========== 扩展任务操作 ==========

    /**
     * 解决任务（委托任务处理完成后归还）
     *
     * @param taskId 任务 ID
     */
    void resolve(String taskId);

    /**
     * 抄送任务
     *
     * @param taskId      任务 ID
     * @param userIds     接收人 ID 列表(逗号分隔)
     * @param initiatorId 发起人 ID
     */
    void cc(String taskId, String userIds, String initiatorId);

    /**
     * 标记抄送已读
     *
     * @param ccId 抄送记录 ID
     */
    void markCCAsRead(Long ccId);

    // ========== 扩展查询接口 ==========

    /**
     * 分页查询已办任务（历史任务）
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<TaskSummaryDTO> pageHistoric(TaskPageQuery query);

    /**
     * 分页查询抄送给我的任务
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<com.basebackend.scheduler.camunda.dto.TaskCCDTO> pageCC(TaskPageQuery query);

    /**
     * 分页查询我发起的流程实例
     *
     * @param query 分页查询参数（复用 TaskPageQuery 或新建 ProcessInstancePageQuery，这里暂时复用
     *              assignee 字段作为 initiator）
     * @return 分页结果
     */
    PageResult<com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO> pageInitiated(TaskPageQuery query);

    /**
     * 转办任务（变更负责人）
     *
     * @param taskId 任务 ID
     * @param userId 新负责人 ID
     */
    void transfer(String taskId, String userId);

    /**
     * 回退任务（驳回到上一个节点）
     *
     * @param taskId 任务 ID
     * @param reason 回退原因
     */
    void rollback(String taskId, String reason);
}

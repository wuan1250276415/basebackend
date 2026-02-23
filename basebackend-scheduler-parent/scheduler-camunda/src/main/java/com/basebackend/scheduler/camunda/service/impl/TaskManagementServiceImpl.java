package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
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
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.TaskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Attachment;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务管理服务实现类
 *
 * <p>
 * 提供任务的查询、认领、完成、委托、变量管理、附件和评论管理等功能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskService taskService;
    private final org.camunda.bpm.engine.HistoryService historyService;
    private final org.camunda.bpm.engine.RuntimeService runtimeService;
    private final com.basebackend.scheduler.camunda.service.TaskCCService taskCCService;
    @Qualifier("camundaAuditLogService")
    private final com.basebackend.scheduler.camunda.service.AuditLogService auditLogService;
    private final org.camunda.bpm.engine.IdentityService identityService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskSummaryDTO> page(TaskPageQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying tasks, page={}, size={}", pageNum, pageSize);

            TaskQuery taskQuery = taskService.createTaskQuery();

            // 应用查询条件
            applyQueryFilters(taskQuery, query);

            // 统计总数
            long total = taskQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<Task> tasks = taskQuery
                    .orderByTaskCreateTime().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<TaskSummaryDTO> dtoList = tasks.stream()
                    .map(this::convertToSummaryDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query tasks", ex);
            throw new CamundaServiceException("查询任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailDTO detail(String taskId, boolean withVariables) {
        try {
            log.info("Getting task detail, taskId={}, withVariables={}", taskId, withVariables);

            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (task == null) {
                throw new CamundaServiceException("任务不存在: " + taskId);
            }

            TaskDetailDTO dto = convertToDetailDTO(task);

            // 获取变量
            if (withVariables) {
                Map<String, Object> variables = taskService.getVariables(taskId);
                dto.setVariables(variables);
            }

            return dto;
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get task detail, taskId={}", taskId, ex);
            throw new CamundaServiceException("获取任务详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(String taskId, ClaimTaskRequest request) {
        try {
            log.info("Claiming task, taskId={}, userId={}", taskId, request.getUserId());

            taskService.claim(taskId, request.getUserId());

            log.info("Task claimed successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to claim task, taskId={}", taskId, ex);
            throw new CamundaServiceException("认领任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unclaim(String taskId) {
        try {
            log.info("Unclaiming task, taskId={}", taskId);

            taskService.setAssignee(taskId, null);

            log.info("Task unclaimed successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to unclaim task, taskId={}", taskId, ex);
            throw new CamundaServiceException("释放任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskId, CompleteTaskRequest request) {
        try {
            log.info("Completing task, taskId={}", taskId);

            if (request.getVariables() != null && !request.getVariables().isEmpty()) {
                taskService.complete(taskId, request.getVariables());
            } else {
                taskService.complete(taskId);
            }

            log.info("Task completed successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to complete task, taskId={}", taskId, ex);
            throw new CamundaServiceException("完成任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, DelegateTaskRequest request) {
        try {
            log.info("Delegating task, taskId={}, userId={}", taskId, request.getUserId());

            taskService.delegateTask(taskId, request.getUserId());

            log.info("Task delegated successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to delegate task, taskId={}", taskId, ex);
            throw new CamundaServiceException("委托任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> variables(String taskId, boolean local) {
        try {
            log.info("Getting task variables, taskId={}, local={}", taskId, local);

            if (local) {
                return taskService.getVariablesLocal(taskId);
            } else {
                return taskService.getVariables(taskId);
            }
        } catch (Exception ex) {
            log.error("Failed to get task variables, taskId={}", taskId, ex);
            throw new CamundaServiceException("获取任务变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertVariable(String taskId, VariableUpsertRequest request) {
        try {
            log.info("Upserting task variable, taskId={}, key={}", taskId, request.getKey());

            if (request.isLocal()) {
                taskService.setVariableLocal(taskId, request.getKey(), request.getValue());
            } else {
                taskService.setVariable(taskId, request.getKey(), request.getValue());
            }

            log.info("Task variable upserted successfully, taskId={}, key={}", taskId, request.getKey());
        } catch (Exception ex) {
            log.error("Failed to upsert task variable, taskId={}, key={}", taskId, request.getKey(), ex);
            throw new CamundaServiceException("设置任务变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVariable(String taskId, String key, boolean local) {
        try {
            log.info("Deleting task variable, taskId={}, key={}", taskId, key);

            if (local) {
                taskService.removeVariableLocal(taskId, key);
            } else {
                taskService.removeVariable(taskId, key);
            }

            log.info("Task variable deleted successfully, taskId={}, key={}", taskId, key);
        } catch (Exception ex) {
            log.error("Failed to delete task variable, taskId={}, key={}", taskId, key, ex);
            throw new CamundaServiceException("删除任务变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDTO> attachments(String taskId) {
        try {
            log.info("Getting task attachments, taskId={}", taskId);

            List<Attachment> attachments = taskService.getTaskAttachments(taskId);

            return attachments.stream()
                    .map(this::convertAttachmentToDTO)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to get task attachments, taskId={}", taskId, ex);
            throw new CamundaServiceException("获取任务附件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentDTO addAttachment(String taskId, AttachmentRequest request) {
        try {
            log.info("Adding task attachment, taskId={}, name={}", taskId, request.getName());

            // 获取任务获取流程实例ID
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new CamundaServiceException("任务不存在: " + taskId);
            }

            Attachment attachment;
            if (request.getContent() != null) {
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent())) {
                    attachment = taskService.createAttachment(
                            request.getType(),
                            taskId,
                            task.getProcessInstanceId(),
                            request.getName(),
                            request.getDescription(),
                            inputStream);
                }
            } else {
                attachment = taskService.createAttachment(
                        request.getType(),
                        taskId,
                        task.getProcessInstanceId(),
                        request.getName(),
                        request.getDescription(),
                        request.getUrl());
            }

            log.info("Task attachment added successfully, taskId={}, attachmentId={}",
                    taskId, attachment.getId());

            return convertAttachmentToDTO(attachment);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to add task attachment, taskId={}", taskId, ex);
            throw new CamundaServiceException("添加任务附件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> comments(String taskId) {
        try {
            log.info("Getting task comments, taskId={}", taskId);

            List<Comment> comments = taskService.getTaskComments(taskId);

            return comments.stream()
                    .map(this::convertCommentToDTO)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to get task comments, taskId={}", taskId, ex);
            throw new CamundaServiceException("获取任务评论失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO addComment(String taskId, CommentRequest request) {
        try {
            log.info("Adding task comment, taskId={}", taskId);

            // 获取任务获取流程实例ID
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new CamundaServiceException("任务不存在: " + taskId);
            }

            Comment comment = taskService.createComment(
                    taskId,
                    task.getProcessInstanceId(),
                    request.getMessage());

            log.info("Task comment added successfully, taskId={}, commentId={}",
                    taskId, comment.getId());

            return convertCommentToDTO(comment);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to add task comment, taskId={}", taskId, ex);
            throw new CamundaServiceException("添加任务评论失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 超时任务查询实现 ==========

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskSummaryDTO> listOverdueTasks(TaskPageQuery query) {
        try {
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying overdue tasks, page={}, size={}", pageNum, pageSize);

            TaskQuery taskQuery = taskService.createTaskQuery()
                    .dueBefore(new Date()); // 截止时间早于当前时间

            // 应用其他查询条件
            applyQueryFilters(taskQuery, query);

            long total = taskQuery.count();
            int firstResult = (pageNum - 1) * pageSize;
            List<Task> tasks = taskQuery
                    .orderByDueDate().asc()
                    .listPage(firstResult, pageSize);

            List<TaskSummaryDTO> dtoList = tasks.stream()
                    .map(this::convertToSummaryDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query overdue tasks", ex);
            throw new CamundaServiceException("查询超时任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskSummaryDTO> listDueSoonTasks(TaskPageQuery query, int hoursUntilDue) {
        try {
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying due soon tasks, page={}, size={}, hoursUntilDue={}", pageNum, pageSize, hoursUntilDue);

            Date now = new Date();
            Date dueSoonLimit = new Date(now.getTime() + (long) hoursUntilDue * 60 * 60 * 1000);

            TaskQuery taskQuery = taskService.createTaskQuery()
                    .dueAfter(now) // 截止时间晚于当前时间
                    .dueBefore(dueSoonLimit); // 但早于指定时间范围

            // 应用其他查询条件
            applyQueryFilters(taskQuery, query);

            long total = taskQuery.count();
            int firstResult = (pageNum - 1) * pageSize;
            List<Task> tasks = taskQuery
                    .orderByDueDate().asc()
                    .listPage(firstResult, pageSize);

            List<TaskSummaryDTO> dtoList = tasks.stream()
                    .map(this::convertToSummaryDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query due soon tasks", ex);
            throw new CamundaServiceException("查询即将超时任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdueTasks() {
        return taskService.createTaskQuery()
                .dueBefore(new Date())
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDueSoonTasks(int hoursUntilDue) {
        Date now = new Date();
        Date dueSoonLimit = new Date(now.getTime() + (long) hoursUntilDue * 60 * 60 * 1000);
        return taskService.createTaskQuery()
                .dueAfter(now)
                .dueBefore(dueSoonLimit)
                .count();
    }

    // ========== 批量任务操作实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchComplete(List<String> taskIds, Map<String, Object> variables) {
        log.info("Batch completing {} tasks", taskIds.size());
        int successCount = 0;
        for (String taskId : taskIds) {
            try {
                if (variables != null && !variables.isEmpty()) {
                    taskService.complete(taskId, variables);
                } else {
                    taskService.complete(taskId);
                }
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to complete task: {}, error: {}", taskId, ex.getMessage());
            }
        }
        log.info("Batch complete finished, success={}/{}", successCount, taskIds.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchClaim(List<String> taskIds, String userId) {
        log.info("Batch claiming {} tasks for user: {}", taskIds.size(), userId);
        int successCount = 0;
        for (String taskId : taskIds) {
            try {
                taskService.claim(taskId, userId);
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to claim task: {}, error: {}", taskId, ex.getMessage());
            }
        }
        log.info("Batch claim finished, success={}/{}", successCount, taskIds.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelegate(List<String> taskIds, String userId) {
        log.info("Batch delegating {} tasks to user: {}", taskIds.size(), userId);
        int successCount = 0;
        for (String taskId : taskIds) {
            try {
                taskService.delegateTask(taskId, userId);
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to delegate task: {}, error: {}", taskId, ex.getMessage());
            }
        }
        log.info("Batch delegate finished, success={}/{}", successCount, taskIds.size());
        return successCount;
    }

    // ========== 扩展任务操作实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(String taskId) {
        try {
            log.info("Resolving task, taskId={}", taskId);
            taskService.resolveTask(taskId);
            log.info("Task resolved successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to resolve task, taskId={}", taskId, ex);
            throw new CamundaServiceException("解决任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cc(String taskId, String userIds, String initiatorId) {
        try {
            log.info("CC task, taskId={}, userIds={}, initiatorId={}", taskId, userIds, initiatorId);
            taskCCService.createCC(taskId, initiatorId, userIds);
            log.info("Task CC successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to CC task, taskId={}", taskId, ex);
            throw new CamundaServiceException("抄送任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCCAsRead(Long ccId) {
        taskCCService.markAsRead(ccId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String taskId, String userId) {
        try {
            log.info("Transferring task (change assignee), taskId={}, newUserId={}", taskId, userId);

            // 验证用户是否存在 (可选，依赖 IdentityService)

            // 转办即修改 Assignee
            taskService.setAssignee(taskId, userId);

            // 记录审计日志
            String currentUserId = null; // Ideally fetch from SecurityContext or IdentityService
            try {
                org.camunda.bpm.engine.impl.identity.Authentication auth = identityService.getCurrentAuthentication();
                if (auth != null) {
                    currentUserId = auth.getUserId();
                }
            } catch (Exception e) {
                // ignore
            }

            auditLogService.log(
                    "DELEGATE", // type
                    taskId,
                    taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId(),
                    currentUserId, // operatorId
                    "Task Transferred", // comment
                    userId // targetUser
            );

            log.info("Task transferred successfully, taskId={}", taskId);
        } catch (Exception ex) {
            log.error("Failed to transfer task, taskId={}", taskId, ex);
            throw new CamundaServiceException("转办任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String taskId, String reason) {
        try {
            log.info("Rolling back task, taskId={}, reason={}", taskId, reason);

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new CamundaServiceException("任务不存在: " + taskId);
            }

            // 1. 查找上一个用户任务节点
            // 简单策略：查找历史活动实例中，最近的一个 Completed UserTask，且不是当前节点
            org.camunda.bpm.engine.history.HistoricActivityInstance lastUserTask = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .activityType("userTask")
                    .finished()
                    .orderByHistoricActivityInstanceEndTime().desc()
                    .list()
                    .stream()
                    .filter(instance -> !instance.getActivityId().equals(task.getTaskDefinitionKey())) // 避开当前节点类型的历史(如果是循环)
                    // 上面的过滤有问题，如果是自循环(Loop)，可能需要回退到上一次实例。
                    // 更加稳健的逻辑是：找到 activityId != currentTaskDefinitionKey 的最近节点。
                    // 或者允许回退到同一个节点（重做）。
                    .findFirst()
                    .orElse(null);

            if (lastUserTask == null) {
                // 如果没有上一个节点（例如这是第一个节点），则无法回退
                throw new CamundaServiceException("无法回退：找不到上一个历史任务节点");
            }

            log.info("Found target activity for rollback: {} ({})", lastUserTask.getActivityName(),
                    lastUserTask.getActivityId());

            // 2. 执行跳转（Modification）
            // 取消当前节点，在目标节点之前启动
            org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder builder = runtimeService
                    .createProcessInstanceModification(task.getProcessInstanceId());

            builder.cancelAllForActivity(task.getTaskDefinitionKey());
            builder.startBeforeActivity(lastUserTask.getActivityId());

            // 可以设置一些变量标识回退
            // builder.setVariable is not available, use runtimeService
            runtimeService.setVariable(task.getProcessInstanceId(), "rollbackReason", reason);

            builder.execute(false, false);

            // 记录审计日志
            String currentUserId = null;
            try {
                org.camunda.bpm.engine.impl.identity.Authentication auth = identityService.getCurrentAuthentication();
                if (auth != null) {
                    currentUserId = auth.getUserId();
                }
            } catch (Exception e) {
                // ignore
            }

            auditLogService.log(
                    "REJECT", // type
                    taskId,
                    task.getProcessInstanceId(),
                    currentUserId, // operatorId
                    String.format("Task rolled back to activity: %s (%s). Reason: %s",
                            lastUserTask.getActivityName(), lastUserTask.getActivityId(), reason), // comment
                    null // targetUser
            );

            log.info("Task rollback executed successfully, moved from {} to {}", task.getName(),
                    lastUserTask.getActivityName());

        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to rollback task, taskId={}", taskId, ex);
            throw new CamundaServiceException("任务回退失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 扩展查询接口实现 ==========

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskSummaryDTO> pageHistoric(TaskPageQuery query) {
        try {
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying historic tasks, page={}, size={}", pageNum, pageSize);

            org.camunda.bpm.engine.history.HistoricTaskInstanceQuery historicQuery = historyService
                    .createHistoricTaskInstanceQuery()
                    .finished(); // 只查询已完成的任务

            // 应用查询条件
            if (StringUtils.hasText(query.getAssignee())) {
                historicQuery.taskAssignee(query.getAssignee());
            }
            if (StringUtils.hasText(query.getProcessInstanceId())) {
                historicQuery.processInstanceId(query.getProcessInstanceId());
            }
            if (StringUtils.hasText(query.getProcessDefinitionKey())) {
                historicQuery.processDefinitionKey(query.getProcessDefinitionKey());
            }
            if (StringUtils.hasText(query.getNameLike())) {
                historicQuery.taskNameLike("%" + query.getNameLike() + "%");
            }
            if (query.getCreatedAfter() != null) {
                historicQuery.startedAfter(
                        Date.from(query.getCreatedAfter().atZone(java.time.ZoneId.systemDefault()).toInstant()));
            }
            if (query.getCreatedBefore() != null) {
                historicQuery.startedBefore(
                        Date.from(query.getCreatedBefore().atZone(java.time.ZoneId.systemDefault()).toInstant()));
            }

            long total = historicQuery.count();
            int firstResult = (pageNum - 1) * pageSize;
            List<org.camunda.bpm.engine.history.HistoricTaskInstance> tasks = historicQuery
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .listPage(firstResult, pageSize);

            List<TaskSummaryDTO> dtoList = tasks.stream()
                    .map(this::convertHistoricToSummaryDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query historic tasks", ex);
            throw new CamundaServiceException("查询已办任务失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.basebackend.scheduler.camunda.dto.TaskCCDTO> pageCC(TaskPageQuery query) {
        return taskCCService.pageMyCC(query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO> pageInitiated(TaskPageQuery query) {
        try {
            // 复用 TaskPageQuery，assignee 作为 initiator
            // 注意: TaskPageQuery 包含了很多任务特有的字段，这里只用通用的
            // 或者是 ProcessInstancePageQuery? 接口定义用了 TaskPageQuery
            // 我们假设 query.getAssignee() 是当前用户 ID

            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying initiated process instances, page={}, size={}, initiator={}", pageNum, pageSize,
                    query.getAssignee());

            org.camunda.bpm.engine.history.HistoricProcessInstanceQuery instanceQuery = historyService
                    .createHistoricProcessInstanceQuery();

            if (StringUtils.hasText(query.getAssignee())) {
                instanceQuery.startedBy(query.getAssignee());
            }
            if (StringUtils.hasText(query.getProcessDefinitionKey())) {
                instanceQuery.processDefinitionKey(query.getProcessDefinitionKey());
            }

            // 排除子流程? 通常 query 会返回所有
            // instanceQuery.rootProcessInstances(); // 可选

            long total = instanceQuery.count();
            int firstResult = (pageNum - 1) * pageSize;
            List<org.camunda.bpm.engine.history.HistoricProcessInstance> instances = instanceQuery
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(firstResult, pageSize);

            List<com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO> dtoList = instances.stream()
                    .map(this::convertHistoricProcessToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);

        } catch (Exception ex) {
            log.error("Failed to query initiated process instances", ex);
            throw new CamundaServiceException("查询我发起的流程失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(TaskQuery query, TaskPageQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getAssignee())) {
            query.taskAssignee(pageQuery.getAssignee());
        }
        if (StringUtils.hasText(pageQuery.getCandidateUser())) {
            query.taskCandidateUser(pageQuery.getCandidateUser());
        }
        if (StringUtils.hasText(pageQuery.getCandidateGroup())) {
            query.taskCandidateGroup(pageQuery.getCandidateGroup());
        }
        if (StringUtils.hasText(pageQuery.getProcessInstanceId())) {
            query.processInstanceId(pageQuery.getProcessInstanceId());
        }
        if (StringUtils.hasText(pageQuery.getProcessDefinitionKey())) {
            query.processDefinitionKey(pageQuery.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(pageQuery.getTenantId())) {
            query.tenantIdIn(pageQuery.getTenantId());
        }
        if (pageQuery.getCreatedAfter() != null) {
            query.taskCreatedAfter(
                    Date.from(pageQuery.getCreatedAfter().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        if (pageQuery.getCreatedBefore() != null) {
            query.taskCreatedBefore(
                    Date.from(pageQuery.getCreatedBefore().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
    }

    private TaskSummaryDTO convertToSummaryDTO(Task task) {
        if (task == null) {
            return null;
        }
        return TaskSummaryDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .suspended(task.isSuspended())
                .tenantId(task.getTenantId())
                .build();
    }

    private TaskDetailDTO convertToDetailDTO(Task task) {
        if (task == null) {
            return null;
        }
        return TaskDetailDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .executionId(task.getExecutionId())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .suspended(task.isSuspended())
                .tenantId(task.getTenantId())
                .formKey(task.getFormKey())
                .delegationState(task.getDelegationState() != null
                        ? task.getDelegationState().name()
                        : null)
                .build();
    }

    private AttachmentDTO convertAttachmentToDTO(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setName(attachment.getName());
        dto.setDescription(attachment.getDescription());
        dto.setType(attachment.getType());
        dto.setUrl(attachment.getUrl());
        dto.setTaskId(attachment.getTaskId());
        dto.setProcessInstanceId(attachment.getProcessInstanceId());
        dto.setCreateTime(attachment.getCreateTime());
        return dto;
    }

    private CommentDTO convertCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setMessage(comment.getFullMessage());
        dto.setUserId(comment.getUserId());
        dto.setTime(comment.getTime());
        dto.setTaskId(comment.getTaskId());
        dto.setProcessInstanceId(comment.getProcessInstanceId());
        return dto;
    }

    // ========== 扩展私有辅助方法 ==========

    private TaskSummaryDTO convertHistoricToSummaryDTO(org.camunda.bpm.engine.history.HistoricTaskInstance task) {
        if (task == null) {
            return null;
        }
        return TaskSummaryDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .createTime(task.getStartTime()) // Historic uses StartTime
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .tenantId(task.getTenantId())
                // .suspended() // Historic doesn't explicitly have suspended state usually, or
                // check status
                .build();
    }

    private com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO convertHistoricProcessToDTO(
            org.camunda.bpm.engine.history.HistoricProcessInstance instance) {
        return com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO.builder()
                .id(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .businessKey(instance.getBusinessKey())
                .tenantId(instance.getTenantId())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime()) // 如果已结束
                .state(instance.getState()) // ACTIVE, COMPLETED...
                // .suspended(...)
                .build();
    }
}

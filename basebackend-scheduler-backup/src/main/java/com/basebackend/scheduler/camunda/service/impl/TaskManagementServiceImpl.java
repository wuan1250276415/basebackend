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
 * <p>提供任务的查询、认领、完成、委托、变量管理、附件和评论管理等功能。
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

            return PageResult.of(dtoList, total, (long)pageNum, (long) pageSize);
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
                            inputStream
                    );
                }
            } else {
                attachment = taskService.createAttachment(
                        request.getType(),
                        taskId,
                        task.getProcessInstanceId(),
                        request.getName(),
                        request.getDescription(),
                        request.getUrl()
                );
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
                    request.getMessage()
            );

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
            query.taskCreatedAfter(Date.from(pageQuery.getCreatedAfter().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        if (pageQuery.getCreatedBefore() != null) {
            query.taskCreatedBefore(Date.from(pageQuery.getCreatedBefore().atZone(java.time.ZoneId.systemDefault()).toInstant()));
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
                        ? task.getDelegationState().name() : null)
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
}

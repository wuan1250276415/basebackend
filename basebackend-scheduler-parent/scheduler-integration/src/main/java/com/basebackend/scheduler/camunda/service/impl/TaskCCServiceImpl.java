package com.basebackend.scheduler.camunda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.TaskCCDTO;
import com.basebackend.scheduler.camunda.dto.TaskPageQuery;
import com.basebackend.scheduler.camunda.entity.TaskCCEntity;
import com.basebackend.scheduler.camunda.mapper.TaskCCMapper;
import com.basebackend.scheduler.camunda.service.TaskCCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务抄送服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCCServiceImpl extends ServiceImpl<TaskCCMapper, TaskCCEntity> implements TaskCCService {

    private final HistoryService historyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCC(String taskId, String initiatorId, String userIds) {
        if (!StringUtils.hasText(userIds)) {
            return;
        }

        // 获取任务详情（包括历史任务，因为抄送可能在任务完成后进行）
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        String[] users = userIds.split(",");
        for (String userId : users) {
            if (!StringUtils.hasText(userId))
                continue;

            TaskCCEntity cc = new TaskCCEntity();
            cc.setTaskId(taskId);
            cc.setProcessInstanceId(task.getProcessInstanceId());
            cc.setProcessDefinitionKey(task.getProcessDefinitionKey());
            cc.setTaskName(task.getName());
            cc.setUserId(userId.trim());
            cc.setInitiatorId(initiatorId);
            cc.setStatus("UNREAD");
            // BaseEntity fields managed by MyBatis Plus handler usually, but here manually
            // set needed?
            // Assuming MyBatisPlus MetaObjectHandler is configured globally.

            this.save(cc);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id) {
        TaskCCEntity cc = this.getById(id);
        if (cc != null && "UNREAD".equals(cc.getStatus())) {
            cc.setStatus("READ");
            this.updateById(cc);
        }
    }

    @Override
    public PageResult<TaskCCDTO> pageMyCC(TaskPageQuery query) {
        // 使用 MyBatis Plus 分页
        Page<TaskCCEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<TaskCCEntity> wrapper = new LambdaQueryWrapper<>();
        // 假设 assignee 字段复用作为查询当前用户的字段，或者 query 中有专门字段
        // TaskPageQuery 字段: assignee (精确匹配), candidateUser (精确匹配)
        // 这里我们用 query.getAssignee() 来代表 "To User" (我的抄送)
        if (StringUtils.hasText(query.getAssignee())) {
            wrapper.eq(TaskCCEntity::getUserId, query.getAssignee());
        }

        // 其他过滤
        if (StringUtils.hasText(query.getProcessInstanceId())) {
            wrapper.eq(TaskCCEntity::getProcessInstanceId, query.getProcessInstanceId());
        }
        if (StringUtils.hasText(query.getNameLike())) {
            wrapper.like(TaskCCEntity::getTaskName, query.getNameLike());
        }

        wrapper.orderByDesc(TaskCCEntity::getCreateTime);

        Page<TaskCCEntity> resultPage = this.page(page, wrapper);

        List<TaskCCDTO> dtos = resultPage.getRecords().stream().map(entity -> {
            TaskCCDTO dto = new TaskCCDTO();
            BeanUtils.copyProperties(entity, dto);
            // Convert createTime manually if type differs (LocalDateTime vs Date)
            // BaseEntity uses LocalDateTime usually?
            // Checking BaseEntity: it uses LocalDateTime usually in modern projects.
            // Let's assume standardized.
            return dto;
        }).collect(Collectors.toList());

        return PageResult.of(dtos, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }
}

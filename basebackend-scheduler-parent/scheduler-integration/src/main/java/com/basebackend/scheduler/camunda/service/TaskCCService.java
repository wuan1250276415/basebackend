package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.TaskCCDTO;
import com.basebackend.scheduler.camunda.dto.TaskPageQuery;
import com.basebackend.scheduler.camunda.entity.TaskCCEntity;

/**
 * 任务抄送服务接口
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface TaskCCService extends IService<TaskCCEntity> {

    /**
     * 创建抄送
     *
     * @param taskId 任务ID
     * @param userId 接收人ID列表 (逗号分隔) or single ID? Let's assume generic logic handles
     *               splitting in controller or here.
     *               Let's make it single for this low-level service, or list.
     *               Actually, business logic says "CC this task to these users".
     *               Let's use a method that takes a single user and one that takes
     *               multiple.
     */
    void createCC(String taskId, String initiatorId, String userIds);

    /**
     * 标记为已读
     *
     * @param id 抄送记录ID
     */
    void markAsRead(Long id);

    /**
     * 分页查询我的抄送
     *
     * @param query 查询条件 (currentUser in query)
     * @return 分页结果
     */
    PageResult<TaskCCDTO> pageMyCC(TaskPageQuery query);
}

package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.*;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业管理服务实现类
 *
 * <p>
 * 提供 Camunda 作业的查询、重试、删除等管理功能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final ManagementService managementService;
    private final RepositoryService repositoryService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<JobDTO> page(JobPageQuery query) {
        try {
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying jobs, page={}, size={}, processDefinitionKey={}, failedOnly={}",
                    pageNum, pageSize, query.getProcessDefinitionKey(), query.getFailedOnly());

            JobQuery jobQuery = managementService.createJobQuery();

            // 应用查询条件
            applyQueryFilters(jobQuery, query);

            // 应用排序
            applySorting(jobQuery, query);

            // 统计总数
            long total = jobQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<Job> jobs = jobQuery.listPage(firstResult, pageSize);

            // 转换为 DTO
            List<JobDTO> dtoList = jobs.stream()
                    .map(JobDTO::from)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query jobs", ex);
            throw new CamundaServiceException("查询作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailDTO detail(String jobId) {
        try {
            log.info("Getting job detail, jobId={}", jobId);

            Job job = managementService.createJobQuery()
                    .jobId(jobId)
                    .singleResult();

            if (job == null) {
                throw new CamundaServiceException("作业不存在: " + jobId);
            }

            JobDetailDTO dto = new JobDetailDTO();
            dto.setId(job.getId());
            dto.setProcessInstanceId(job.getProcessInstanceId());
            dto.setProcessDefinitionId(job.getProcessDefinitionId());
            dto.setProcessDefinitionKey(job.getProcessDefinitionKey());
            dto.setExecutionId(job.getExecutionId());
            // Job 接口没有 getJobHandlerType() 方法
            dto.setJobHandlerType(null);
            dto.setDuedate(job.getDuedate());
            dto.setCreateTime(job.getCreateTime());
            dto.setRetries(job.getRetries());
            dto.setExceptionMessage(job.getExceptionMessage());
            dto.setPriority(job.getPriority());
            dto.setTenantId(job.getTenantId());
            dto.setSuspended(job.isSuspended());
            dto.setDeploymentId(job.getDeploymentId());

            // Camunda Job.getRetries() 返回 int 原始类型，不会为 null
            int retries = job.getRetries();
            boolean isFailed = retries == 0 && job.getExceptionMessage() != null;
            dto.setFailed(isFailed);

            // 判断是否可执行
            boolean isExecutable = job.getDuedate() != null
                    && job.getDuedate().before(new Date())
                    && retries > 0;
            dto.setExecutable(isExecutable);

            // 获取异常堆栈
            if (job.getExceptionMessage() != null) {
                try {
                    String stacktrace = managementService.getJobExceptionStacktrace(jobId);
                    dto.setExceptionStacktrace(stacktrace);
                } catch (Exception e) {
                    log.warn("Failed to get exception stacktrace for job: {}", jobId);
                }
            }

            // 获取活动名称
            if (job.getProcessDefinitionId() != null) {
                try {
                    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionId(job.getProcessDefinitionId())
                            .singleResult();
                    if (definition != null) {
                        dto.setActivityName(definition.getName());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get process definition for job: {}", jobId);
                }
            }

            return dto;
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get job detail, jobId={}", jobId, ex);
            throw new CamundaServiceException("获取作业详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getExceptionStacktrace(String jobId) {
        try {
            log.info("Getting exception stacktrace, jobId={}", jobId);
            return managementService.getJobExceptionStacktrace(jobId);
        } catch (Exception ex) {
            log.error("Failed to get exception stacktrace, jobId={}", jobId, ex);
            throw new CamundaServiceException("获取异常堆栈失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retry(String jobId, JobRetryRequest request) {
        try {
            log.info("Retrying job, jobId={}, retries={}", jobId, request.getRetries());

            Job job = managementService.createJobQuery()
                    .jobId(jobId)
                    .singleResult();

            if (job == null) {
                throw new CamundaServiceException("作业不存在: " + jobId);
            }

            // 设置重试次数
            managementService.setJobRetries(jobId, request.getRetries());

            // 设置截止时间（可选）
            if (request.getDuedate() != null) {
                managementService.setJobDuedate(jobId, request.getDuedate());
            }

            log.info("Job retry configured successfully, jobId={}", jobId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retry job, jobId={}", jobId, ex);
            throw new CamundaServiceException("重试作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeNow(String jobId) {
        try {
            log.info("Executing job immediately, jobId={}", jobId);

            Job job = managementService.createJobQuery()
                    .jobId(jobId)
                    .singleResult();

            if (job == null) {
                throw new CamundaServiceException("作业不存在: " + jobId);
            }

            // 执行作业
            managementService.executeJob(jobId);

            log.info("Job executed successfully, jobId={}", jobId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to execute job, jobId={}", jobId, ex);
            throw new CamundaServiceException("执行作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String jobId) {
        try {
            log.info("Deleting job, jobId={}", jobId);

            Job job = managementService.createJobQuery()
                    .jobId(jobId)
                    .singleResult();

            if (job == null) {
                throw new CamundaServiceException("作业不存在: " + jobId);
            }

            managementService.deleteJob(jobId);

            log.info("Job deleted successfully, jobId={}", jobId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to delete job, jobId={}", jobId, ex);
            throw new CamundaServiceException("删除作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDuedate(String jobId, Date duedate) {
        try {
            log.info("Setting job duedate, jobId={}, duedate={}", jobId, duedate);
            managementService.setJobDuedate(jobId, duedate);
            log.info("Job duedate set successfully, jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Failed to set job duedate, jobId={}", jobId, ex);
            throw new CamundaServiceException("设置作业截止时间失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPriority(String jobId, long priority) {
        try {
            log.info("Setting job priority, jobId={}, priority={}", jobId, priority);
            managementService.setJobPriority(jobId, priority);
            log.info("Job priority set successfully, jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Failed to set job priority, jobId={}", jobId, ex);
            throw new CamundaServiceException("设置作业优先级失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRetries(String jobId, int retries) {
        try {
            log.info("Setting job retries, jobId={}, retries={}", jobId, retries);
            managementService.setJobRetries(jobId, retries);
            log.info("Job retries set successfully, jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Failed to set job retries, jobId={}", jobId, ex);
            throw new CamundaServiceException("设置作业重试次数失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspend(String jobId) {
        try {
            log.info("Suspending job, jobId={}", jobId);
            managementService.suspendJobById(jobId);
            log.info("Job suspended successfully, jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Failed to suspend job, jobId={}", jobId, ex);
            throw new CamundaServiceException("挂起作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(String jobId) {
        try {
            log.info("Activating job, jobId={}", jobId);
            managementService.activateJobById(jobId);
            log.info("Job activated successfully, jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Failed to activate job, jobId={}", jobId, ex);
            throw new CamundaServiceException("激活作业失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRetry(List<String> jobIds, Integer retries) {
        int successCount = 0;
        int actualRetries = retries != null ? retries : 3;

        log.info("Batch retrying jobs, count={}, retries={}", jobIds.size(), actualRetries);

        for (String jobId : jobIds) {
            try {
                managementService.setJobRetries(jobId, actualRetries);
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to retry job: {}, error: {}", jobId, ex.getMessage());
            }
        }

        log.info("Batch retry completed, success={}, total={}", successCount, jobIds.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<String> jobIds) {
        int successCount = 0;

        log.info("Batch deleting jobs, count={}", jobIds.size());

        for (String jobId : jobIds) {
            try {
                managementService.deleteJob(jobId);
                successCount++;
            } catch (Exception ex) {
                log.warn("Failed to delete job: {}, error: {}", jobId, ex.getMessage());
            }
        }

        log.info("Batch delete completed, success={}, total={}", successCount, jobIds.size());
        return successCount;
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedJobs() {
        return managementService.createJobQuery()
                .noRetriesLeft()
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countExecutableJobs() {
        return managementService.createJobQuery()
                .executable()
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> listFailedJobs(int maxResults) {
        List<Job> jobs = managementService.createJobQuery()
                .noRetriesLeft()
                .orderByJobDuedate().asc()
                .listPage(0, maxResults);

        return jobs.stream()
                .map(JobDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countFailedJobsByProcessDefinition() {
        List<Job> failedJobs = managementService.createJobQuery()
                .noRetriesLeft()
                .list();

        return failedJobs.stream()
                .filter(job -> job.getProcessDefinitionKey() != null)
                .collect(Collectors.groupingBy(
                        Job::getProcessDefinitionKey,
                        Collectors.counting()));
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(JobQuery query, JobPageQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getProcessInstanceId())) {
            query.processInstanceId(pageQuery.getProcessInstanceId());
        }
        if (StringUtils.hasText(pageQuery.getProcessDefinitionId())) {
            query.processDefinitionId(pageQuery.getProcessDefinitionId());
        }
        if (StringUtils.hasText(pageQuery.getProcessDefinitionKey())) {
            query.processDefinitionKey(pageQuery.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(pageQuery.getActivityId())) {
            query.activityId(pageQuery.getActivityId());
        }
        if (StringUtils.hasText(pageQuery.getExecutionId())) {
            query.executionId(pageQuery.getExecutionId());
        }
        if (StringUtils.hasText(pageQuery.getTenantId())) {
            query.tenantIdIn(pageQuery.getTenantId());
        }
        if (Boolean.TRUE.equals(pageQuery.getFailedOnly())) {
            query.noRetriesLeft();
        }
        if (Boolean.TRUE.equals(pageQuery.getSuspendedOnly())) {
            query.suspended();
        }
        if (Boolean.TRUE.equals(pageQuery.getExecutableOnly())) {
            query.executable();
        }
        if (Boolean.TRUE.equals(pageQuery.getTimersOnly())) {
            query.timers();
        }
        if (Boolean.TRUE.equals(pageQuery.getMessagesOnly())) {
            query.messages();
        }
        if (Boolean.TRUE.equals(pageQuery.getWithException())) {
            query.withException();
        }
        if (Boolean.TRUE.equals(pageQuery.getNoException())) {
            query.noRetriesLeft();
        }
        if (pageQuery.getDuedateBefore() != null) {
            query.duedateLowerThan(pageQuery.getDuedateBefore());
        }
        if (pageQuery.getDuedateAfter() != null) {
            query.duedateHigherThan(pageQuery.getDuedateAfter());
        }
    }

    private void applySorting(JobQuery query, JobPageQuery pageQuery) {
        String sortBy = pageQuery.getSortBy();
        boolean asc = !"desc".equalsIgnoreCase(pageQuery.getSortOrder());

        if (sortBy == null) {
            // 默认按截止时间排序
            if (asc) {
                query.orderByJobDuedate().asc();
            } else {
                query.orderByJobDuedate().desc();
            }
            return;
        }

        switch (sortBy.toLowerCase()) {
            case "jobid":
                if (asc)
                    query.orderByJobId().asc();
                else
                    query.orderByJobId().desc();
                break;
            case "executionid":
                if (asc)
                    query.orderByExecutionId().asc();
                else
                    query.orderByExecutionId().desc();
                break;
            case "processinstanceid":
                if (asc)
                    query.orderByProcessInstanceId().asc();
                else
                    query.orderByProcessInstanceId().desc();
                break;
            case "processdefinitionid":
                if (asc)
                    query.orderByProcessDefinitionId().asc();
                else
                    query.orderByProcessDefinitionId().desc();
                break;
            case "processdefinitionkey":
                if (asc)
                    query.orderByProcessDefinitionKey().asc();
                else
                    query.orderByProcessDefinitionKey().desc();
                break;
            case "jobretries":
                if (asc)
                    query.orderByJobRetries().asc();
                else
                    query.orderByJobRetries().desc();
                break;
            case "jobduedate":
                if (asc)
                    query.orderByJobDuedate().asc();
                else
                    query.orderByJobDuedate().desc();
                break;
            case "jobpriority":
                if (asc)
                    query.orderByJobPriority().asc();
                else
                    query.orderByJobPriority().desc();
                break;
            default:
                if (asc)
                    query.orderByJobDuedate().asc();
                else
                    query.orderByJobDuedate().desc();
        }
    }
}

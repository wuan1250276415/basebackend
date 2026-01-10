package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.JobDTO;
import com.basebackend.scheduler.camunda.dto.JobDetailDTO;
import com.basebackend.scheduler.camunda.dto.JobPageQuery;
import com.basebackend.scheduler.camunda.dto.JobRetryRequest;

import java.util.Date;
import java.util.List;

/**
 * 作业管理业务逻辑接口
 *
 * <p>
 * 提供 Camunda 作业（Job）相关的业务逻辑封装，包括：
 * <ul>
 * <li>作业查询（分页、详情）</li>
 * <li>作业操作（重试、删除、修改截止时间）</li>
 * <li>失败作业统计</li>
 * <li>批量作业操作</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface JobService {

    /**
     * 分页查询作业
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<JobDTO> page(JobPageQuery query);

    /**
     * 获取作业详情
     *
     * @param jobId 作业 ID
     * @return 作业详情
     */
    JobDetailDTO detail(String jobId);

    /**
     * 获取作业异常堆栈
     *
     * @param jobId 作业 ID
     * @return 异常堆栈字符串
     */
    String getExceptionStacktrace(String jobId);

    /**
     * 重试作业
     *
     * @param jobId   作业 ID
     * @param request 重试请求参数
     */
    void retry(String jobId, JobRetryRequest request);

    /**
     * 立即执行作业
     *
     * @param jobId 作业 ID
     */
    void executeNow(String jobId);

    /**
     * 删除作业
     *
     * @param jobId 作业 ID
     */
    void delete(String jobId);

    /**
     * 修改作业截止时间
     *
     * @param jobId   作业 ID
     * @param duedate 新的截止时间
     */
    void setDuedate(String jobId, Date duedate);

    /**
     * 修改作业优先级
     *
     * @param jobId    作业 ID
     * @param priority 新的优先级
     */
    void setPriority(String jobId, long priority);

    /**
     * 修改作业重试次数
     *
     * @param jobId   作业 ID
     * @param retries 新的重试次数
     */
    void setRetries(String jobId, int retries);

    /**
     * 挂起作业
     *
     * @param jobId 作业 ID
     */
    void suspend(String jobId);

    /**
     * 激活作业
     *
     * @param jobId 作业 ID
     */
    void activate(String jobId);

    /**
     * 批量重试失败作业
     *
     * @param jobIds  作业 ID 列表
     * @param retries 重试次数（可选，默认3）
     * @return 成功重试的数量
     */
    int batchRetry(List<String> jobIds, Integer retries);

    /**
     * 批量删除作业
     *
     * @param jobIds 作业 ID 列表
     * @return 成功删除的数量
     */
    int batchDelete(List<String> jobIds);

    /**
     * 统计失败作业数量
     *
     * @return 失败作业数量
     */
    long countFailedJobs();

    /**
     * 统计可执行作业数量
     *
     * @return 可执行作业数量
     */
    long countExecutableJobs();

    /**
     * 获取失败作业列表（快捷方法）
     *
     * @param maxResults 最大结果数
     * @return 失败作业列表
     */
    List<JobDTO> listFailedJobs(int maxResults);

    /**
     * 按流程定义统计失败作业
     *
     * @return 按流程定义 Key 分组的失败作业数量
     */
    java.util.Map<String, Long> countFailedJobsByProcessDefinition();
}

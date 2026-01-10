package com.basebackend.scheduler.camunda.listener;

import com.basebackend.scheduler.camunda.entity.NodeConfigEntity;
import com.basebackend.scheduler.camunda.entity.ProcessTemplateEntity;
import com.basebackend.scheduler.camunda.service.CandidateRuleResolver;
import com.basebackend.scheduler.camunda.service.NodeConfigService;
import com.basebackend.scheduler.camunda.service.ProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 全局任务候选人监听器
 *
 * <p>
 * 在任务创建时触发，根据节点配置动态设置候选人/组。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalTaskCandidateListener implements TaskListener {

    private final ProcessTemplateService processTemplateService;
    private final NodeConfigService nodeConfigService;
    private final CandidateRuleResolver candidateRuleResolver;

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String processDefinitionId = delegateTask.getProcessDefinitionId();
            String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

            // 获取流程定义Key
            // 注意: 这里假设 RepositoryService 可以通过 delegateTask 获取，或者通过 processEngineServices
            String processDefinitionKey = delegateTask.getProcessEngineServices()
                    .getRepositoryService()
                    .getProcessDefinition(processDefinitionId)
                    .getKey();

            String tenantId = delegateTask.getTenantId();

            log.debug("Processing task candidate rules for task: {}, process: {}", taskDefinitionKey,
                    processDefinitionKey);

            // 1. 获取流程模版
            ProcessTemplateEntity template = processTemplateService.getByKey(processDefinitionKey, tenantId);
            if (template == null) {
                return; // 没有模版配置，跳过
            }

            // 2. 获取节点配置
            NodeConfigEntity nodeConfig = nodeConfigService.getByNodeKey(template.getId(), taskDefinitionKey);
            if (nodeConfig == null || !StringUtils.hasText(nodeConfig.getCandidateRule())) {
                return; // 没有节点配置或规则，跳过
            }

            // 3. 解析规则
            String rule = nodeConfig.getCandidateRule();
            if (candidateRuleResolver.supports(rule)) {
                List<String> candidates = candidateRuleResolver.resolve(rule, delegateTask.getVariables());

                if (!candidates.isEmpty()) {
                    log.info("Applying candidate rule '{}' to task '{}': {}", rule, delegateTask.getId(), candidates);
                    // 这里简单处理：添加候选组或用户
                    // 实际策略可能区分 USER: 和 GROUP:，当前 Resolver 返回的是 ID 列表
                    // 我们假设 Resolver 实现已经处理了前缀，或者我们需要在这里处理
                    // 为了简化，假设 Resolver 返回的是裸 ID，且我们需要知道是用户还是组
                    // 更好的方式是 Resolver 返回带类型的对象，或者我们在 Rule 中约定

                    // 重新审视 Resolver 实现: USER:userId -> List("userId")
                    // 我们在这里无法区分是用户还是组，除非 Rule 本身包含信息
                    // 让我们改进 Resolver 接口? 或者在这里再次判断 Rule 前缀?
                    // 为了解耦，最好 Listener 处理类型

                    if (rule.startsWith("USER:")) {
                        delegateTask.addCandidateUsers(candidates);
                    } else if (rule.startsWith("GROUP:")) {
                        delegateTask.addCandidateGroups(candidates);
                    } else {
                        // 默认作为用户处理，或者根据 ID 格式判断
                        delegateTask.addCandidateUsers(candidates);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to apply global task candidate rules", e);
            // 不抛出异常，以免影响流程正常运行
        }
    }
}

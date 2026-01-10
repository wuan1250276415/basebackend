package com.basebackend.scheduler.camunda.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.springframework.stereotype.Component;

/**
 * 全局 BPMN 解析监听器
 *
 * <p>
 * 用于在流程解析阶段，向所有 UserTask 注入全局监听器。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalBpmnParseListener extends AbstractBpmnParseListener {

    private final GlobalTaskCandidateListener globalTaskCandidateListener;

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        ActivityBehavior activityBehavior = activity.getActivityBehavior();
        if (activityBehavior instanceof UserTaskActivityBehavior) {
            UserTaskActivityBehavior userTaskBehavior = (UserTaskActivityBehavior) activityBehavior;

            // 添加任务创建监听器
            userTaskBehavior.getTaskDefinition()
                    .addTaskListener(TaskListener.EVENTNAME_CREATE, globalTaskCandidateListener);

            log.debug("Added global candidate listener to user task: {}", activity.getId());
        }
    }
}

package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 数据同步任务委托
 */
@Slf4j
@Component("dataSyncDelegate")
public class DataSyncDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sourceSystem = (String) execution.getVariable("sourceSystem");
        String targetSystem = (String) execution.getVariable("targetSystem");
        String dataType = (String) execution.getVariable("dataType");

        log.info("开始数据同步: source={}, target={}, dataType={}",
                sourceSystem, targetSystem, dataType);

        try {
            // TODO: 实现实际的数据同步逻辑
            // dataSyncService.sync(sourceSystem, targetSystem, dataType);

            // 模拟数据同步
            Thread.sleep(2000);

            // 设置同步结果
            execution.setVariable("syncSuccess", true);
            execution.setVariable("syncRecordCount", 100);
            execution.setVariable("syncTime", System.currentTimeMillis());

            log.info("数据同步成功: recordCount=100");
        } catch (Exception e) {
            log.error("数据同步失败", e);
            execution.setVariable("syncSuccess", false);
            execution.setVariable("syncError", e.getMessage());
            throw e;
        }
    }
}

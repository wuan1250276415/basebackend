package com.basebackend.common.export;

import lombok.Builder;
import lombok.Data;

/**
 * 异步导出任务状态
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@Builder
public class ExportTaskStatus {

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    private String taskId;
    private Status status;
    private String message;
    private ExportResult result;
}

package com.basebackend.common.export;

/**
 * 不支持的导出格式异常
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class UnsupportedExportFormatException extends RuntimeException {

    public UnsupportedExportFormatException(ExportFormat format) {
        super("Unsupported export format: " + format);
    }
}

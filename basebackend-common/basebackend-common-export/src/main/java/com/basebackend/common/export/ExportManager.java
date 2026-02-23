package com.basebackend.common.export;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一导出入口
 * <p>
 * 根据 {@link ExportFormat} 路由到对应的 {@link ExportService} 实现。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ExportManager {

    private final Map<ExportFormat, ExportService> exportServiceMap;

    public ExportManager(List<ExportService> exportServices) {
        this.exportServiceMap = exportServices.stream()
                .collect(Collectors.toMap(ExportService::supportedFormat, Function.identity(),
                        (existing, replacement) -> replacement));
    }

    /**
     * 根据格式导出数据
     *
     * @param data   数据列表
     * @param clazz  数据类型
     * @param format 导出格式
     * @return 导出结果
     * @throws UnsupportedExportFormatException 如果请求的格式没有对应实现
     */
    public <T> ExportResult export(List<T> data, Class<T> clazz, ExportFormat format) {
        ExportService service = exportServiceMap.get(format);
        if (service == null) {
            throw new UnsupportedExportFormatException(format);
        }
        return service.export(data, clazz);
    }

    /**
     * 检查是否支持指定格式
     */
    public boolean supports(ExportFormat format) {
        return exportServiceMap.containsKey(format);
    }
}

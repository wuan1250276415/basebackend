package com.basebackend.common.export;

import java.util.List;

public interface ExportService {

    ExportFormat supportedFormat();

    <T> ExportResult export(List<T> data, Class<T> clazz);
}

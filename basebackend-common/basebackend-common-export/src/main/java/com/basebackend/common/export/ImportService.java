package com.basebackend.common.export;

import java.io.InputStream;

public interface ImportService {

    ExportFormat supportedFormat();

    <T> ImportResult<T> importData(InputStream input, Class<T> clazz);
}

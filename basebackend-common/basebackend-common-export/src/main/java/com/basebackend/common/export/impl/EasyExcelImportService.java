package com.basebackend.common.export.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.basebackend.common.export.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * EasyExcel 导入服务
 * <p>
 * 基于 Alibaba EasyExcel 实现 XLSX 格式导入。
 * 读取 Excel 文件解析为 POJO 列表，支持基本类型转换。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class EasyExcelImportService implements ImportService {

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.XLSX;
    }

    @Override
    public <T> ImportResult<T> importData(InputStream input, Class<T> clazz) {
        Map<String, Field> labelToField = buildLabelMap(clazz);
        List<T> data = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int[] counters = {0, 0}; // [total, success]

        try {
            EasyExcel.read(input, new ReadListener<Map<Integer, String>>() {
                private Map<Integer, String> headerMap;

                @Override
                public void invokeHead(Map<Integer, com.alibaba.excel.metadata.data.ReadCellData<?>> headMap,
                                       AnalysisContext context) {
                    headerMap = new LinkedHashMap<>();
                    headMap.forEach((index, cellData) -> headerMap.put(index, cellData.getStringValue()));
                }

                @Override
                public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                    counters[0]++;
                    try {
                        T instance = clazz.getDeclaredConstructor().newInstance();
                        for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                            String header = headerMap != null ? headerMap.get(entry.getKey()) : null;
                            if (header != null) {
                                Field field = labelToField.get(header);
                                if (field != null) {
                                    field.setAccessible(true);
                                    String value = entry.getValue();
                                    if (value != null && !value.isEmpty()) {
                                        field.set(instance, convertValue(value, field.getType()));
                                    }
                                }
                            }
                        }
                        data.add(instance);
                        counters[1]++;
                    } catch (Exception e) {
                        errors.add("Row " + counters[0] + ": " + e.getMessage());
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // no-op
                }
            }).sheet().doRead();
        } catch (Exception e) {
            errors.add("Excel parse error: " + e.getMessage());
        }

        return ImportResult.<T>builder()
                .totalRows(counters[0])
                .successRows(counters[1])
                .failedRows(counters[0] - counters[1])
                .errors(errors)
                .data(data)
                .build();
    }

    private Map<String, Field> buildLabelMap(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            ExportField annotation = field.getAnnotation(ExportField.class);
            if (annotation != null) {
                map.put(annotation.label(), field);
            }
        }
        return map;
    }

    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        return value;
    }
}

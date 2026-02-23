package com.basebackend.common.export.impl;

import com.basebackend.common.export.ExportField;
import com.basebackend.common.export.ExportFormat;
import com.basebackend.common.export.ImportResult;
import com.basebackend.common.export.ImportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvImportService implements ImportService {

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public <T> ImportResult<T> importData(InputStream input, Class<T> clazz) {
        Map<String, Field> labelToField = buildLabelMap(clazz);
        List<T> data = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (CSVParser parser = new CSVParser(
                new InputStreamReader(input, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            for (CSVRecord record : parser) {
                total++;
                try {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (Map.Entry<String, Field> entry : labelToField.entrySet()) {
                        String value = record.isMapped(entry.getKey()) ? record.get(entry.getKey()) : null;
                        if (value != null && !value.isEmpty()) {
                            Field field = entry.getValue();
                            field.setAccessible(true);
                            field.set(instance, convertValue(value, field.getType()));
                        }
                    }
                    data.add(instance);
                    success++;
                } catch (Exception e) {
                    errors.add("Row " + total + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("CSV parse error: " + e.getMessage());
        }

        return ImportResult.<T>builder()
                .totalRows(total)
                .successRows(success)
                .failedRows(total - success)
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
        return value;
    }
}

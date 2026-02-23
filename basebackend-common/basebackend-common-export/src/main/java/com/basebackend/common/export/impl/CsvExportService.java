package com.basebackend.common.export.impl;

import com.basebackend.common.export.ExportField;
import com.basebackend.common.export.ExportFormat;
import com.basebackend.common.export.ExportResult;
import com.basebackend.common.export.ExportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CsvExportService implements ExportService {

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public <T> ExportResult export(List<T> data, Class<T> clazz) {
        List<FieldMeta> fields = resolveFields(clazz);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(fields.stream().map(f -> f.label).toArray(String[]::new))
                             .build())) {

            for (T item : data) {
                List<Object> values = new ArrayList<>(fields.size());
                for (FieldMeta fm : fields) {
                    fm.field.setAccessible(true);
                    Object val = fm.field.get(item);
                    values.add(val != null ? val.toString() : "");
                }
                printer.printRecord(values);
            }
            printer.flush();
            byte[] content = baos.toByteArray();

            return ExportResult.builder()
                    .fileName(clazz.getSimpleName() + ".csv")
                    .contentType("text/csv")
                    .content(content)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("CSV export failed", e);
        }
    }

    static List<FieldMeta> resolveFields(Class<?> clazz) {
        List<FieldMeta> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            ExportField annotation = field.getAnnotation(ExportField.class);
            if (annotation != null) {
                result.add(new FieldMeta(field, annotation.label(), annotation.order()));
            }
        }
        result.sort(Comparator.comparingInt(f -> f.order));
        return result;
    }

    static class FieldMeta {
        final Field field;
        final String label;
        final int order;

        FieldMeta(Field field, String label, int order) {
            this.field = field;
            this.label = label;
            this.order = order;
        }
    }
}

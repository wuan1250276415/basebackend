package com.basebackend.common.export.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.basebackend.common.export.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * EasyExcel 导出服务
 * <p>
 * 基于 Alibaba EasyExcel 实现 XLSX 格式导出。
 * 支持 {@link ExportField} 注解控制列名、顺序、宽度、格式和自定义转换器。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class EasyExcelExportService implements ExportService {

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.XLSX;
    }

    @Override
    public <T> ExportResult export(List<T> data, Class<T> clazz) {
        List<ExcelFieldMeta> fields = resolveFields(clazz);

        List<List<String>> headList = new ArrayList<>();
        for (ExcelFieldMeta fm : fields) {
            headList.add(Collections.singletonList(fm.label));
        }

        List<List<Object>> dataList = new ArrayList<>();
        for (T item : data) {
            List<Object> row = new ArrayList<>(fields.size());
            for (ExcelFieldMeta fm : fields) {
                try {
                    fm.field.setAccessible(true);
                    Object val = fm.field.get(item);
                    row.add(convertValue(val, fm));
                } catch (IllegalAccessException e) {
                    row.add("");
                }
            }
            dataList.add(row);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            WriteCellStyle headStyle = new WriteCellStyle();
            WriteFont headFont = new WriteFont();
            headFont.setBold(true);
            headStyle.setWriteFont(headFont);
            headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

            WriteCellStyle contentStyle = new WriteCellStyle();
            HorizontalCellStyleStrategy styleStrategy =
                    new HorizontalCellStyleStrategy(headStyle, contentStyle);

            EasyExcel.write(baos)
                    .inMemory(Boolean.TRUE)
                    .head(headList)
                    .registerWriteHandler(styleStrategy)
                    .sheet("Sheet1")
                    .doWrite(dataList);

            return ExportResult.builder()
                    .fileName(clazz.getSimpleName() + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .content(baos.toByteArray())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }

    private Object convertValue(Object value, ExcelFieldMeta meta) {
        if (value == null) {
            return "";
        }

        // 自定义转换器优先
        if (meta.converterClass != null && meta.converterClass != FieldConverter.class) {
            try {
                FieldConverter converter = meta.converterClass.getDeclaredConstructor().newInstance();
                return converter.convert(value);
            } catch (Exception e) {
                // 转换器实例化失败，使用默认转换
            }
        }

        // 日期格式化
        if (!meta.format.isEmpty()) {
            if (value instanceof LocalDateTime ldt) {
                return ldt.format(DateTimeFormatter.ofPattern(meta.format));
            } else if (value instanceof LocalDate ld) {
                return ld.format(DateTimeFormatter.ofPattern(meta.format));
            } else if (value instanceof Date date) {
                return new SimpleDateFormat(meta.format).format(date);
            }
        }

        return value.toString();
    }

    public static List<ExcelFieldMeta> resolveFields(Class<?> clazz) {
        List<ExcelFieldMeta> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            ExportField annotation = field.getAnnotation(ExportField.class);
            if (annotation != null) {
                result.add(new ExcelFieldMeta(
                        field, annotation.label(), annotation.order(),
                        annotation.width(), annotation.format(), annotation.converter()
                ));
            }
        }
        result.sort(Comparator.comparingInt(f -> f.order));
        return result;
    }

    public static class ExcelFieldMeta {
        public final Field field;
        public final String label;
        public final int order;
        public final int width;
        public final String format;
        public final Class<? extends FieldConverter> converterClass;

        public ExcelFieldMeta(Field field, String label, int order, int width,
                              String format, Class<? extends FieldConverter> converterClass) {
            this.field = field;
            this.label = label;
            this.order = order;
            this.width = width;
            this.format = format;
            this.converterClass = converterClass;
        }
    }
}

package com.basebackend.file.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Office 文档预览服务（简化版）
 *
 * 支持 CSV 文件的预览，Word/Excel/PowerPoint 返回提示信息
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficePreviewService {

    private static final long MAX_INPUT_SIZE = 100 * 1024 * 1024; // 100MB 最大输入限制

    /**
     * 提取 Office 文档内容为 HTML
     *
     * @param documentData 文档数据
     * @param fileExtension 文件扩展名
     * @return HTML 格式的预览内容
     */
    @Cacheable(value = "officePreview", key = "T(java.util.Arrays).hashCode(#documentData) + ':' + #fileExtension")
    public String extractToHtml(byte[] documentData, String fileExtension) throws IOException {
        log.debug("提取 Office 文档预览: {}, 大小: {} bytes", fileExtension, documentData.length);

        // 验证输入
        validateInput(documentData);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(documentData)) {
            String extension = fileExtension.toLowerCase();
            return switch (extension) {
                case "csv" -> extractCsvToHtml(inputStream);
                case "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> {
                    // 提示用户使用 PDF 转换
                    StringBuilder html = new StringBuilder();
                    html.append("<div class='office-preview'>");
                    html.append("<h3>").append(getFileTypeName(extension)).append(" 文档</h3>");
                    html.append("<div class='notice'>");
                    html.append("<p>⚠️ 此版本暂不支持此类文档预览</p>");
                    html.append("<p>💡 建议将文档转换为 PDF 后上传，可获得更好的预览体验</p>");
                    html.append("</div>");
                    html.append("</div>");
                    yield html.toString();
                }
                default -> throw new IllegalArgumentException("不支持的文档类型: " + fileExtension);
            };
        }
    }

    /**
     * 提取 CSV 文件内容
     */
    private String extractCsvToHtml(InputStream inputStream) throws IOException {
        log.debug("提取 CSV 文件");

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            StringBuilder html = new StringBuilder();
            html.append("<div class='csv-preview'>");
            html.append("<h3>CSV 文件预览</h3>");
            html.append("<table class='csv-table'>");

            List<String[]> allRows = new ArrayList<>();
            String[] row;

            // 读取前 1000 行
            while ((row = csvReader.readNext()) != null && allRows.size() < 1000) {
                allRows.add(row);
            }

            for (String[] data : allRows) {
                html.append("<tr>");
                for (String cell : data) {
                    html.append("<td>").append(escapeHtml(cell != null ? cell : "")).append("</td>");
                }
                html.append("</tr>");
            }

            html.append("</table>");
            html.append("</div>");

            return html.toString();
        } catch (Exception e) {
            log.error("提取 CSV 文件失败", e);
            throw new IOException("提取 CSV 文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件类型中文名称
     */
    private String getFileTypeName(String extension) {
        return switch (extension) {
            case "doc", "docx" -> "Word";
            case "xls", "xlsx" -> "Excel";
            case "ppt", "pptx" -> "PowerPoint";
            default -> extension.toUpperCase();
        };
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * 验证输入
     */
    private void validateInput(byte[] documentData) {
        if (documentData == null) {
            throw new IllegalArgumentException("文档数据不能为 null");
        }
        if (documentData.length == 0) {
            throw new IllegalArgumentException("文档数据不能为空");
        }
        if (documentData.length > MAX_INPUT_SIZE) {
            throw new IllegalArgumentException(
                    "文档数据过大: " + (documentData.length / 1024 / 1024) + " MB，最大允许 " +
                    (MAX_INPUT_SIZE / 1024 / 1024) + " MB");
        }
    }

    /**
     * 检查是否为支持的 Office 文档类型
     */
    public boolean isSupported(String fileExtension) {
        if (fileExtension == null) return false;
        String ext = fileExtension.toLowerCase();
        return ext.equals("doc") || ext.equals("docx") ||
               ext.equals("xls") || ext.equals("xlsx") ||
               ext.equals("ppt") || ext.equals("pptx") ||
               ext.equals("csv");
    }
}

package com.basebackend.common.export.config;

import com.basebackend.common.export.AsyncExportService;
import com.basebackend.common.export.ExportManager;
import com.basebackend.common.export.ExportService;
import com.basebackend.common.export.ImportService;
import com.basebackend.common.export.impl.CsvExportService;
import com.basebackend.common.export.impl.CsvImportService;
import com.basebackend.common.export.impl.EasyExcelExportService;
import com.basebackend.common.export.impl.EasyExcelImportService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(ExportProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.export", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "csvExportService")
    @ConditionalOnClass(name = "org.apache.commons.csv.CSVFormat")
    public ExportService csvExportService() {
        return new CsvExportService();
    }

    @Bean
    @ConditionalOnMissingBean(name = "csvImportService")
    @ConditionalOnClass(name = "org.apache.commons.csv.CSVFormat")
    public ImportService csvImportService() {
        return new CsvImportService();
    }

    @Bean
    @ConditionalOnMissingBean(name = "excelExportService")
    @ConditionalOnClass(name = "com.alibaba.excel.EasyExcel")
    public ExportService excelExportService() {
        return new EasyExcelExportService();
    }

    @Bean
    @ConditionalOnMissingBean(name = "excelImportService")
    @ConditionalOnClass(name = "com.alibaba.excel.EasyExcel")
    public ImportService excelImportService() {
        return new EasyExcelImportService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportManager exportManager(List<ExportService> exportServices) {
        return new ExportManager(exportServices);
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExportService asyncExportService(ExportManager exportManager, ExportProperties properties) {
        return new AsyncExportService(exportManager,
                properties.getAsync().getThreadPoolSize(),
                properties.getAsync().getTaskTtlHours());
    }
}

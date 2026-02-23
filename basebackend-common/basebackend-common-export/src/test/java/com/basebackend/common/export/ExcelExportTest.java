package com.basebackend.common.export;

import com.basebackend.common.export.impl.CsvExportService;
import com.basebackend.common.export.impl.EasyExcelExportService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Excel 导出功能单元测试
 */
class ExcelExportTest {

    // ========== EasyExcel 导出 ==========

    @Test
    void excelExport_producesValidXlsx() {
        EasyExcelExportService service = new EasyExcelExportService();
        List<TestUser> data = List.of(
                new TestUser("Alice", 30),
                new TestUser("Bob", 25)
        );

        ExportResult result = service.export(data, TestUser.class);

        assertNotNull(result);
        assertEquals("TestUser.xlsx", result.getFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.getContentType());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().length > 0);
    }

    @Test
    void excelExport_emptyList_producesFile() {
        EasyExcelExportService service = new EasyExcelExportService();
        ExportResult result = service.export(List.of(), TestUser.class);

        assertNotNull(result);
        assertEquals("TestUser.xlsx", result.getFileName());
        assertTrue(result.getContent().length > 0);
    }

    // ========== @ExportField 注解解析 ==========

    @Test
    void exportField_resolvesFieldsInOrder() {
        var fields = EasyExcelExportService.resolveFields(TestUser.class);
        assertEquals(2, fields.size());
        assertEquals("Name", fields.get(0).label);
        assertEquals("Age", fields.get(1).label);
    }

    @Test
    void exportField_widthAttribute() {
        var fields = EasyExcelExportService.resolveFields(TestUserWithWidth.class);
        assertEquals(1, fields.size());
        assertEquals(30, fields.get(0).width);
    }

    // ========== ExportManager 格式路由 ==========

    @Test
    void exportManager_routesToCorrectService() {
        ExportService csvService = new CsvExportService();
        ExportService excelService = new EasyExcelExportService();
        ExportManager manager = new ExportManager(List.of(csvService, excelService));

        assertTrue(manager.supports(ExportFormat.CSV));
        assertTrue(manager.supports(ExportFormat.XLSX));

        ExportResult csvResult = manager.export(List.of(new TestUser("Alice", 30)), TestUser.class, ExportFormat.CSV);
        assertEquals("text/csv", csvResult.getContentType());

        ExportResult xlsxResult = manager.export(List.of(new TestUser("Alice", 30)), TestUser.class, ExportFormat.XLSX);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxResult.getContentType());
    }

    @Test
    void exportManager_throwsForUnsupportedFormat() {
        ExportManager manager = new ExportManager(List.of(new CsvExportService()));
        assertThrows(UnsupportedExportFormatException.class,
                () -> manager.export(List.of(), TestUser.class, ExportFormat.XLSX));
    }

    // ========== Test Data Classes ==========

    public static class TestUser {
        @ExportField(label = "Name", order = 1)
        private String name;

        @ExportField(label = "Age", order = 2)
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public static class TestUserWithWidth {
        @ExportField(label = "Email", order = 1, width = 30)
        private String email;

        public TestUserWithWidth() {}
    }
}

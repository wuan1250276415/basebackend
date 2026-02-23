package com.basebackend.common.export;

import com.basebackend.common.export.impl.CsvExportService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportServiceTest {

    private final CsvExportService service = new CsvExportService();

    @Test
    void export_producesCorrectCsv() {
        List<TestUser> data = List.of(
                new TestUser("Alice", 30),
                new TestUser("Bob", 25)
        );

        ExportResult result = service.export(data, TestUser.class);

        assertNotNull(result);
        assertEquals("TestUser.csv", result.getFileName());
        assertEquals("text/csv", result.getContentType());

        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Name"));
        assertTrue(csv.contains("Age"));
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("30"));
        assertTrue(csv.contains("Bob"));
        assertTrue(csv.contains("25"));
    }

    @Test
    void export_emptyList_producesHeaderOnly() {
        ExportResult result = service.export(List.of(), TestUser.class);
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Name"));
        assertFalse(csv.contains("Alice"));
    }

    @Test
    void export_fieldsOrderedCorrectly() {
        ExportResult result = service.export(List.of(new TestUser("X", 1)), TestUser.class);
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        int nameIdx = csv.indexOf("Name");
        int ageIdx = csv.indexOf("Age");
        assertTrue(nameIdx < ageIdx, "Name (order=1) should appear before Age (order=2)");
    }

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
}

package com.basebackend.database.migration.service.impl;

import com.basebackend.database.exception.MigrationException;
import com.basebackend.database.migration.model.MigrationBackup;
import com.basebackend.database.migration.service.MigrationBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 迁移备份服务实现
 * 
 * @author basebackend
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class MigrationBackupServiceImpl implements MigrationBackupService {

    private final DataSource dataSource;

    @Value("${database.enhanced.migration.backup-dir:./backups}")
    private String backupDir;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public MigrationBackup createBackup(String migrationVersion, List<String> tables) {
        String backupId = generateBackupId(migrationVersion);
        String backupPath = getBackupPath(backupId);

        try {
            log.info("开始创建迁移备份，版本: {}, 备份ID: {}", migrationVersion, backupId);

            // 确保备份目录存在
            ensureBackupDirectoryExists();

            // 获取需要备份的表
            List<String> tablesToBackup = tables != null && !tables.isEmpty() 
                ? tables 
                : getAllTables();

            // 创建备份文件
            long backupSize = createBackupFile(backupPath, tablesToBackup);

            MigrationBackup backup = MigrationBackup.builder()
                .backupId(backupId)
                .migrationVersion(migrationVersion)
                .backupPath(backupPath)
                .backupTime(LocalDateTime.now())
                .backupSize(backupSize)
                .status("SUCCESS")
                .description(String.format("备份 %d 个表", tablesToBackup.size()))
                .restored(false)
                .build();

            log.info("迁移备份创建成功，备份ID: {}, 大小: {} bytes", backupId, backupSize);
            return backup;

        } catch (Exception e) {
            log.error("创建迁移备份失败", e);
            throw new MigrationException("创建迁移备份失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String restoreBackup(String backupId) {
        try {
            log.info("开始恢复备份，备份ID: {}", backupId);

            String backupPath = getBackupPath(backupId);
            File backupFile = new File(backupPath);

            if (!backupFile.exists()) {
                throw new MigrationException("备份文件不存在: " + backupPath);
            }

            // 读取并执行备份文件中的SQL语句
            String sql = new String(Files.readAllBytes(backupFile.toPath()));
            executeBackupSql(sql);

            log.info("备份恢复成功，备份ID: {}", backupId);
            return "备份恢复成功";

        } catch (Exception e) {
            log.error("恢复备份失败", e);
            throw new MigrationException("恢复备份失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MigrationBackup> listBackups() {
        try {
            File backupDirectory = new File(backupDir);
            if (!backupDirectory.exists()) {
                return Collections.emptyList();
            }

            File[] backupFiles = backupDirectory.listFiles((dir, name) -> name.endsWith(".sql"));
            if (backupFiles == null || backupFiles.length == 0) {
                return Collections.emptyList();
            }

            return Arrays.stream(backupFiles)
                .map(this::fileToBackupInfo)
                .sorted(Comparator.comparing(MigrationBackup::getBackupTime).reversed())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String deleteBackup(String backupId) {
        try {
            String backupPath = getBackupPath(backupId);
            File backupFile = new File(backupPath);

            if (!backupFile.exists()) {
                throw new MigrationException("备份文件不存在: " + backupPath);
            }

            if (backupFile.delete()) {
                log.info("备份删除成功，备份ID: {}", backupId);
                return "备份删除成功";
            } else {
                throw new MigrationException("备份文件删除失败");
            }

        } catch (Exception e) {
            log.error("删除备份失败", e);
            throw new MigrationException("删除备份失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MigrationBackup getBackup(String backupId) {
        String backupPath = getBackupPath(backupId);
        File backupFile = new File(backupPath);

        if (!backupFile.exists()) {
            throw new MigrationException("备份文件不存在: " + backupPath);
        }

        return fileToBackupInfo(backupFile);
    }

    /**
     * 生成备份ID
     */
    private String generateBackupId(String migrationVersion) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String versionPart = migrationVersion != null ? migrationVersion.replace(".", "_") : "unknown";
        return String.format("backup_%s_%s", versionPart, timestamp);
    }

    /**
     * 获取备份文件路径
     */
    private String getBackupPath(String backupId) {
        return Paths.get(backupDir, backupId + ".sql").toString();
    }

    /**
     * 确保备份目录存在
     */
    private void ensureBackupDirectoryExists() throws IOException {
        Path path = Paths.get(backupDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("创建备份目录: {}", backupDir);
        }
    }

    /**
     * 获取所有表名
     */
    private List<String> getAllTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    // 排除Flyway的历史表
                    if (!tableName.equalsIgnoreCase("flyway_schema_history")) {
                        tables.add(tableName);
                    }
                }
            }
        }
        
        return tables;
    }

    /**
     * 创建备份文件
     */
    private long createBackupFile(String backupPath, List<String> tables) throws SQLException, IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupPath));
             Connection conn = dataSource.getConnection()) {

            // 写入备份头信息
            writer.write("-- Database Backup\n");
            writer.write("-- Created: " + LocalDateTime.now() + "\n");
            writer.write("-- Tables: " + String.join(", ", tables) + "\n");
            writer.write("\n");

            // 备份每个表
            for (String table : tables) {
                backupTable(writer, conn, table);
            }

            writer.flush();
        }

        // 返回文件大小
        return new File(backupPath).length();
    }

    /**
     * 备份单个表
     */
    private void backupTable(BufferedWriter writer, Connection conn, String tableName) throws SQLException, IOException {
        writer.write("-- Table: " + tableName + "\n");
        writer.write("DROP TABLE IF EXISTS `" + tableName + "_backup`;\n");
        writer.write("CREATE TABLE `" + tableName + "_backup` AS SELECT * FROM `" + tableName + "`;\n");
        writer.write("\n");

        // 获取表数据
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tableName + "`")) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 如果表有数据，生成INSERT语句
            if (rs.next()) {
                writer.write("-- Data for table: " + tableName + "\n");
                
                do {
                    writer.write("INSERT INTO `" + tableName + "` VALUES (");
                    
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        
                        if (value == null) {
                            writer.write("NULL");
                        } else if (value instanceof String || value instanceof java.sql.Date || 
                                   value instanceof java.sql.Timestamp || value instanceof java.util.Date) {
                            writer.write("'" + value.toString().replace("'", "''") + "'");
                        } else {
                            writer.write(value.toString());
                        }
                        
                        if (i < columnCount) {
                            writer.write(", ");
                        }
                    }
                    
                    writer.write(");\n");
                } while (rs.next());
                
                writer.write("\n");
            }
        }
    }

    /**
     * 执行备份SQL
     */
    private void executeBackupSql(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 分割SQL语句并执行
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    /**
     * 将文件转换为备份信息
     */
    private MigrationBackup fileToBackupInfo(File file) {
        String fileName = file.getName().replace(".sql", "");
        
        return MigrationBackup.builder()
            .backupId(fileName)
            .backupPath(file.getAbsolutePath())
            .backupTime(LocalDateTime.ofEpochSecond(
                file.lastModified() / 1000, 0, 
                java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now())
            ))
            .backupSize(file.length())
            .status("SUCCESS")
            .restored(false)
            .build();
    }
}

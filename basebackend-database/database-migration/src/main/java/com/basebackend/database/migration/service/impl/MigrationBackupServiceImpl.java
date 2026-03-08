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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern BACKUP_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern INSERT_INTO_PATTERN = Pattern.compile("(?i)^INSERT\\s+INTO\\s+`?([a-zA-Z0-9_]+)`?.*");
    private static final Pattern DELETE_FROM_PATTERN = Pattern.compile("(?i)^DELETE\\s+FROM\\s+`?([a-zA-Z0-9_]+)`?.*");
    private static final Pattern TRUNCATE_TABLE_PATTERN = Pattern.compile("(?i)^TRUNCATE\\s+TABLE\\s+`?([a-zA-Z0-9_]+)`?.*");
    private static final Pattern LEGACY_BACKUP_TABLE_PATTERN =
            Pattern.compile("(?i)^(DROP|CREATE)\\s+TABLE\\s+(IF\\s+EXISTS\\s+)?`?([a-zA-Z0-9_]+_backup)`?.*");
    private static final String RESTORE_MARK_FILE_SUFFIX = ".restored";

    @Override
    public MigrationBackup createBackup(String migrationVersion, List<String> tables) {
        String backupId = generateBackupId(migrationVersion);
        String backupPath = getBackupPath(backupId);

        try {
            log.info("开始创建迁移备份，版本: {}, 备份ID: {}", migrationVersion, backupId);

            // 确保备份目录存在
            ensureBackupDirectoryExists();

            // 获取并校验需要备份的表
            List<String> tablesToBackup = resolveTablesToBackup(tables);

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
            String sql = Files.readString(backupFile.toPath(), StandardCharsets.UTF_8);
            int executedStatements = executeBackupSql(sql);
            markBackupAsRestored(backupId, executedStatements);

            log.info("备份恢复成功，备份ID: {}, 执行语句数: {}", backupId, executedStatements);
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
                Files.deleteIfExists(getRestoreMarkerPath(backupId));
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
        String normalizedBackupId = normalizeBackupId(backupId);
        return Paths.get(backupDir, normalizedBackupId + ".sql").toString();
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
        writer.write("DELETE FROM " + quoteIdentifier(tableName) + ";\n");
        writer.write("\n");

        // 获取表数据
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + quoteIdentifier(tableName))) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columnNames = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            // 如果表有数据，生成INSERT语句
            if (rs.next()) {
                writer.write("-- Data for table: " + tableName + "\n");
                String columnClause = columnNames.stream()
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));
                
                do {
                    writer.write("INSERT INTO " + quoteIdentifier(tableName) + " (" + columnClause + ") VALUES (");
                    
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        writer.write(formatSqlLiteral(value));
                        
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
    private int executeBackupSql(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            List<String> statements = splitSqlStatements(sql);
            if (statements.isEmpty()) {
                throw new MigrationException("备份脚本为空或无可执行语句");
            }

            boolean originalAutoCommit = conn.getAutoCommit();
            boolean isMySql = isMySqlConnection(conn);

            conn.setAutoCommit(false);
            try {
                if (isMySql) {
                    stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                }

                int executedCount = 0;
                Set<String> cleanedTables = new HashSet<>();
                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (trimmedStatement.isEmpty()) {
                        continue;
                    }

                    if (shouldSkipLegacyBackupTableStatement(trimmedStatement)) {
                        continue;
                    }

                    String explicitlyCleanedTable = extractTableName(trimmedStatement, DELETE_FROM_PATTERN);
                    if (explicitlyCleanedTable == null) {
                        explicitlyCleanedTable = extractTableName(trimmedStatement, TRUNCATE_TABLE_PATTERN);
                    }
                    if (explicitlyCleanedTable != null) {
                        cleanedTables.add(explicitlyCleanedTable.toLowerCase(Locale.ROOT));
                    }

                    String insertTable = extractTableName(trimmedStatement, INSERT_INTO_PATTERN);
                    if (insertTable != null && cleanedTables.add(insertTable.toLowerCase(Locale.ROOT))) {
                        stmt.execute("DELETE FROM " + quoteIdentifier(insertTable));
                        executedCount++;
                    }

                    stmt.execute(trimmedStatement);
                    executedCount++;
                }

                conn.commit();
                return executedCount;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                if (isMySql) {
                    try {
                        stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                    } catch (SQLException e) {
                        log.warn("恢复外键检查失败", e);
                    }
                }
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private List<String> resolveTablesToBackup(List<String> tables) throws SQLException {
        List<String> availableTables = getAllTables();
        if (tables == null || tables.isEmpty()) {
            return availableTables;
        }

        Map<String, String> normalizedTableMap = new HashMap<>();
        for (String table : availableTables) {
            normalizedTableMap.put(table.toLowerCase(Locale.ROOT), table);
        }

        LinkedHashSet<String> resolvedTables = new LinkedHashSet<>();
        for (String table : tables) {
            if (table == null || table.trim().isEmpty()) {
                throw new MigrationException("表名不能为空");
            }
            String normalizedTable = table.trim().toLowerCase(Locale.ROOT);
            String actualTable = normalizedTableMap.get(normalizedTable);
            if (actualTable == null) {
                throw new MigrationException("待备份表不存在或不允许访问: " + table);
            }
            resolvedTables.add(actualTable);
        }

        return new ArrayList<>(resolvedTables);
    }

    private String normalizeBackupId(String backupId) {
        if (backupId == null || backupId.trim().isEmpty()) {
            throw new MigrationException("备份ID不能为空");
        }

        String normalizedBackupId = backupId.trim();
        if (!BACKUP_ID_PATTERN.matcher(normalizedBackupId).matches()) {
            throw new MigrationException("备份ID格式非法: " + backupId);
        }
        return normalizedBackupId;
    }

    private List<String> splitSqlStatements(String sqlScript) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sqlScript.length(); i++) {
            char currentChar = sqlScript.charAt(i);
            char nextChar = i + 1 < sqlScript.length() ? sqlScript.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (currentChar == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (currentChar == '-' && nextChar == '-') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (currentChar == '#') {
                    inLineComment = true;
                    continue;
                }
                if (currentChar == '/' && nextChar == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
            }

            if (currentChar == '\'' && !inDoubleQuote && !inBacktick) {
                currentStatement.append(currentChar);
                if (inSingleQuote) {
                    if (nextChar == '\'') {
                        currentStatement.append('\'');
                        i++;
                    } else if (!isEscaped(sqlScript, i)) {
                        inSingleQuote = false;
                    }
                } else {
                    inSingleQuote = true;
                }
                continue;
            }

            if (currentChar == '"' && !inSingleQuote && !inBacktick) {
                currentStatement.append(currentChar);
                if (!isEscaped(sqlScript, i)) {
                    inDoubleQuote = !inDoubleQuote;
                }
                continue;
            }

            if (currentChar == '`' && !inSingleQuote && !inDoubleQuote) {
                currentStatement.append(currentChar);
                inBacktick = !inBacktick;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick && currentChar == ';') {
                String statement = currentStatement.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                currentStatement.setLength(0);
                continue;
            }

            currentStatement.append(currentChar);
        }

        String tailStatement = currentStatement.toString().trim();
        if (!tailStatement.isEmpty()) {
            statements.add(tailStatement);
        }

        return statements;
    }

    private boolean shouldSkipLegacyBackupTableStatement(String sql) {
        Matcher matcher = LEGACY_BACKUP_TABLE_PATTERN.matcher(sql);
        if (matcher.matches()) {
            log.debug("跳过旧版备份临时表语句: {}", sql);
            return true;
        }
        return false;
    }

    private String extractTableName(String statement, Pattern pattern) {
        Matcher matcher = pattern.matcher(statement);
        if (!matcher.matches()) {
            return null;
        }
        String tableName = matcher.group(1);
        return tableName == null ? null : tableName.trim();
    }

    private boolean isEscaped(String text, int index) {
        int slashCount = 0;
        int cursor = index - 1;
        while (cursor >= 0 && text.charAt(cursor) == '\\') {
            slashCount++;
            cursor--;
        }
        return slashCount % 2 != 0;
    }

    private Path getRestoreMarkerPath(String backupId) {
        String normalizedBackupId = normalizeBackupId(backupId);
        return Paths.get(backupDir, normalizedBackupId + RESTORE_MARK_FILE_SUFFIX);
    }

    private void markBackupAsRestored(String backupId, int executedStatements) throws IOException {
        Path markerPath = getRestoreMarkerPath(backupId);
        String markerContent = "restoredAt=" + LocalDateTime.now() + System.lineSeparator()
                + "executedStatements=" + executedStatements;
        Files.writeString(
                markerPath,
                markerContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private LocalDateTime parseRestoreTime(String backupId) {
        Path markerPath = getRestoreMarkerPath(backupId);
        if (!Files.exists(markerPath)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(markerPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("restoredAt=")) {
                    return LocalDateTime.parse(line.substring("restoredAt=".length()).trim());
                }
            }
        } catch (Exception e) {
            log.warn("解析恢复标记文件失败: {}", markerPath, e);
        }
        return null;
    }

    private boolean isBackupRestored(String backupId) {
        return Files.exists(getRestoreMarkerPath(backupId));
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String formatSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "1" : "0";
        }
        if (value instanceof byte[] binary) {
            return "0x" + toHex(binary);
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private String toHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte datum : data) {
            builder.append(String.format("%02x", datum));
        }
        return builder.toString();
    }

    private boolean isMySqlConnection(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
    }

    /**
     * 将文件转换为备份信息
     */
    private MigrationBackup fileToBackupInfo(File file) {
        String fileName = file.getName().replace(".sql", "");
        LocalDateTime restoreTime = parseRestoreTime(fileName);
        boolean restored = isBackupRestored(fileName);

        return MigrationBackup.builder()
            .backupId(fileName)
            .backupPath(file.getAbsolutePath())
            .backupTime(LocalDateTime.ofEpochSecond(
                file.lastModified() / 1000, 0,
                java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now())
            ))
            .backupSize(file.length())
            .status("SUCCESS")
            .restored(restored)
            .restoreTime(restoreTime)
            .build();
    }

}

package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.infrastructure.executor.*;
import com.basebackend.backup.infrastructure.monitoring.BackupMetricsRegistrar;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MySQL备份执行器
 * <p>
 * 支持全量备份（mysqldump）和增量备份（binlog）两种备份模式：
 * <ul>
 * <li>全量备份：使用mysqldump工具导出整个数据库</li>
 * <li>增量备份：通过解析MySQL binlog实现增量数据捕获（需要mysql-binlog-connector-java）</li>
 * </ul>
 * <p>
 * 安全性考虑：
 * <ul>
 * <li>密码通过MYSQL_PWD环境变量传递，避免命令行暴露</li>
 * <li>使用ProcessBuilder参数列表方式执行命令，防止命令注入</li>
 * </ul>
 *
 * @author BaseBackend
 * @see AbstractBackupExecutor
 * @see DataSourceBackupExecutor
 */
@Slf4j
@Component
public class MySqlBackupExecutor extends AbstractBackupExecutor
        implements DataSourceBackupExecutor<BackupRequest> {

    private final BackupProperties backupProperties;
    @Nullable
    private final MySqlBinlogParser binlogParser;

    /**
     * 构造MySQL备份执行器
     * <p>
     * 通过构造器注入所有依赖，提高可测试性和代码可维护性
     *
     * @param lockManager         分布式锁管理器，用于防止并发备份
     * @param retryTemplate       重试模板，用于处理临时性失败
     * @param storageProvider     存储提供者，用于上传备份文件
     * @param checksumService     校验和服务，用于验证文件完整性
     * @param backupHistoryMapper 备份历史Mapper，用于持久化备份记录
     * @param backupProperties    备份配置属性
     * @param binlogParser        MySQL binlog解析器（可选，仅增量备份需要）
     */
    public MySqlBackupExecutor(
            com.basebackend.backup.infrastructure.reliability.LockManager lockManager,
            com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate retryTemplate,
            com.basebackend.backup.infrastructure.storage.StorageProvider storageProvider,
            com.basebackend.backup.infrastructure.reliability.impl.ChecksumService checksumService,
            com.basebackend.backup.domain.mapper.BackupHistoryMapper backupHistoryMapper,
            @Nullable BackupMetricsRegistrar backupMetricsRegistrar,
            BackupProperties backupProperties,
            @Nullable MySqlBinlogParser binlogParser) {
        super(lockManager, retryTemplate, storageProvider, checksumService, backupHistoryMapper, backupMetricsRegistrar);
        this.backupProperties = backupProperties;
        this.binlogParser = binlogParser;
    }

    @Override
    public BackupArtifact executeFull(BackupRequest request) throws Exception {
        log.info("执行MySQL全量备份: {}", request.getTaskId());

        // 获取输出文件路径
        String outputFile = getOutputFilePath(request, "full");
        File backupFile = new File(outputFile);
        BinlogPosition startPosition = null;
        BinlogPosition endPosition = null;

        try {
            BackupRequest.DatabaseConfig dbConfig = request.getDatabaseConfig();
            if (dbConfig != null) {
                try {
                    startPosition = queryCurrentBinlogPosition(dbConfig);
                } catch (Exception e) {
                    log.warn("获取全量备份起始binlog位点失败，将继续执行全量备份: taskId={}", request.getTaskId(), e);
                }
            }

            // 构建mysqldump命令参数（分离式，避免shell注入）
            List<String> command = buildMysqldumpCommandArgs(request);

            // 使用ProcessBuilder执行命令，输出重定向到文件
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            applyMysqlPassword(processBuilder, request.getDatabaseConfig().getPassword());
            processBuilder.redirectOutput(backupFile);
            processBuilder.redirectErrorStream(true);

            log.info("执行mysqldump命令: {}", String.join(" ", command));

            Process process = processBuilder.start();

            // 消费输出流（避免进程阻塞）
            consumeProcessOutput(process.getInputStream(), "INFO");
            consumeProcessOutput(process.getErrorStream(), "ERROR");

            // 等待进程完成（设置超时）
            boolean completed = process.waitFor(backupProperties.getRetry().getMaxAttempts() * 60, TimeUnit.SECONDS);

            if (!completed) {
                log.error("mysqldump执行超时，强制终止进程");
                process.destroyForcibly();
                throw new RuntimeException("mysqldump执行超时");
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                // 读取错误信息
                String errorMsg = new String(Files.readAllBytes(backupFile.toPath()));
                throw new RuntimeException("mysqldump执行失败, 退出码: " + exitCode + ", 错误: " + errorMsg);
            }

            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件未生成: " + outputFile);
            }

            if (dbConfig != null) {
                try {
                    endPosition = queryCurrentBinlogPosition(dbConfig);
                } catch (Exception e) {
                    log.warn("获取全量备份结束binlog位点失败: taskId={}", request.getTaskId(), e);
                }
            }

            // 构建备份产物
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(backupFile)
                    .backupType("full")
                    .fileSize(backupFile.length())
                    .startTime(request.getStartTime())
                    .endTime(LocalDateTime.now())
                    .binlogStartPosition(startPosition == null ? null : startPosition.toString())
                    .binlogEndPosition(endPosition == null ? null : endPosition.toString())
                    .durationSeconds(
                            (System.currentTimeMillis() - request.getStartTime().toEpochSecond(ZoneOffset.UTC) * 1000)
                                    / 1000)
                    .build();

            log.info("MySQL全量备份完成: {}, 大小: {} bytes", outputFile, backupFile.length());

            return artifact;

        } catch (Exception e) {
            // 异常时清理临时文件
            if (backupFile.exists()) {
                try {
                    Files.delete(backupFile.toPath());
                    log.info("已清理临时备份文件: {}", outputFile);
                } catch (IOException cleanupEx) {
                    log.warn("清理临时备份文件失败: {}", outputFile, cleanupEx);
                }
            }
            throw e;
        }
    }

    @Override
    public BackupArtifact executeIncremental(IncrementalBackupRequest request) throws Exception {
        BackupRequest.DatabaseConfig dbConfig = request.getDatabaseConfig();
        if (dbConfig == null) {
            throw new IllegalArgumentException("增量备份数据库配置不能为空");
        }

        BinlogPosition startPosition = resolveStartPosition(request);
        BinlogPosition endPosition = queryCurrentBinlogPosition(dbConfig);

        if (startPosition.compareTo(endPosition) > 0) {
            throw new IllegalStateException("增量位点非法: start > end, start=" + startPosition + ", end=" + endPosition);
        }

        String outputFile = getOutputFilePath(request, "incremental");
        File backupFile = new File(outputFile);

        try {
            if (startPosition.compareTo(endPosition) == 0) {
                Files.writeString(backupFile.toPath(),
                        "-- MySQL incremental backup: no changes\n-- position: " + startPosition + "\n");
                return buildIncrementalArtifact(request, backupFile, startPosition, endPosition);
            }

            exportBinlogRange(dbConfig, startPosition, endPosition, backupFile);

            if (!backupFile.exists()) {
                throw new RuntimeException("增量备份文件未生成: " + outputFile);
            }

            return buildIncrementalArtifact(request, backupFile, startPosition, endPosition);
        } catch (Exception e) {
            if (backupFile.exists()) {
                try {
                    Files.delete(backupFile.toPath());
                } catch (IOException cleanupEx) {
                    log.warn("清理临时增量备份文件失败: {}", outputFile, cleanupEx);
                }
            }
            throw e;
        }
    }

    @Override
    public boolean restore(BackupArtifact artifact, String targetDatabase) throws Exception {
        log.info("开始恢复MySQL备份: {}", artifact.getFile());

        // 构建mysql命令参数（分离式，避免shell注入）
        List<String> command = buildMysqlCommandArgs(artifact, targetDatabase);

        // 使用ProcessBuilder执行命令，输入重定向到文件
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyMysqlPassword(processBuilder, backupProperties.getDatabase().getPassword());
        processBuilder.redirectInput(artifact.getFile());
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();

        // 消费输出流（避免进程阻塞）
        consumeProcessOutput(process.getInputStream(), "INFO");
        consumeProcessOutput(process.getErrorStream(), "ERROR");

        // 等待进程完成（设置超时）
        boolean completed = process.waitFor(backupProperties.getRetry().getMaxAttempts() * 60, TimeUnit.SECONDS);

        if (!completed) {
            log.error("MySQL恢复执行超时，强制终止进程");
            process.destroyForcibly();
            return false;
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {
            log.error("MySQL恢复失败, 退出码: {}", exitCode);
            return false;
        }

        log.info("MySQL恢复成功: {}", artifact.getFile());
        return true;
    }

    @Override
    public String getCurrentIncrementalPosition(BackupRequest request) throws Exception {
        return queryCurrentBinlogPosition(request.getDatabaseConfig()).toString();
    }

    @Override
    public boolean verifyBackup(BackupArtifact artifact) throws Exception {
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
            return false;
        }

        // 验证文件完整性
        return artifact.getFile().length() > 0;
    }

    @Override
    public String getSupportedDatasourceType() {
        return "mysql";
    }

    @Override
    public String[] getSupportedFeatures() {
        return new String[] { "full_backup", "incremental_backup", "restore", "checksum_verification" };
    }

    /**
     * 构建mysqldump命令参数列表（避免命令注入）
     */
    private List<String> buildMysqldumpCommandArgs(BackupRequest request) {
        BackupRequest.DatabaseConfig dbConfig = request.getDatabaseConfig();

        List<String> command = new ArrayList<>();
        command.add(backupProperties.getMysqldumpPath());
        command.add("-h");
        command.add(dbConfig.getHost());
        command.add("-P");
        command.add(String.valueOf(dbConfig.getPort()));
        command.add("-u");
        command.add(dbConfig.getUsername());
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add(dbConfig.getDatabase());

        return command;
    }

    /**
     * 消费进程输出（避免进程阻塞）
     */
    private void consumeProcessOutput(InputStream inputStream, String logLevel) {
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("ERROR".equals(logLevel)) {
                        log.error("MySQL进程错误: {}", line);
                    } else {
                        log.debug("MySQL进程输出: {}", line);
                    }
                }
            } catch (IOException e) {
                log.debug("读取进程输出失败", e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
    }

    /**
     * 构建mysql恢复命令参数（避免命令注入）
     */
    private List<String> buildMysqlCommandArgs(BackupArtifact artifact, String targetDatabase) {
        List<String> command = new ArrayList<>();

        // 基础配置
        command.add(backupProperties.getMysqlPath());
        command.add("-h");
        command.add(backupProperties.getDatabase().getHost());
        command.add("-P");
        command.add(String.valueOf(backupProperties.getDatabase().getPort()));
        command.add("-u");
        command.add(backupProperties.getDatabase().getUsername());

        // 指定字符集
        command.add("--default-character-set=utf8mb4");

        // 目标数据库（为空时回退到配置库名）
        String database = (targetDatabase == null || targetDatabase.isBlank())
                ? backupProperties.getDatabase().getDatabase()
                : targetDatabase;
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("目标数据库不能为空");
        }
        command.add(database);

        return command;
    }

    private List<String> buildMysqlBinlogCommandArgs(BackupRequest.DatabaseConfig dbConfig,
                                                      String binlogFile,
                                                      Long startPosition,
                                                      Long stopPosition) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getMysqlbinlogPath());
        command.add("--read-from-remote-server");
        command.add("--host=" + dbConfig.getHost());
        command.add("--port=" + dbConfig.getPort());
        command.add("--user=" + dbConfig.getUsername());
        command.add("--database=" + dbConfig.getDatabase());
        if (startPosition != null) {
            command.add("--start-position=" + startPosition);
        }
        if (stopPosition != null) {
            command.add("--stop-position=" + stopPosition);
        }
        command.add(binlogFile);
        return command;
    }

    private BackupArtifact buildIncrementalArtifact(IncrementalBackupRequest request,
                                                    File backupFile,
                                                    BinlogPosition startPosition,
                                                    BinlogPosition endPosition) {
        LocalDateTime startTime = request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();
        return BackupArtifact.builder()
                .file(backupFile)
                .backupType("incremental")
                .fileSize(backupFile.length())
                .startTime(startTime)
                .endTime(endTime)
                .binlogStartPosition(startPosition.toString())
                .binlogEndPosition(endPosition.toString())
                .durationSeconds(Math.max(
                        (System.currentTimeMillis() - startTime.toEpochSecond(ZoneOffset.UTC) * 1000) / 1000,
                        0))
                .build();
    }

    private BinlogPosition resolveStartPosition(IncrementalBackupRequest request) {
        String rawStart = request.getBinlogStartPosition();
        if (rawStart == null || rawStart.isBlank()) {
            rawStart = request.getStartPosition();
        }
        if (rawStart == null || rawStart.isBlank()) {
            throw new IllegalArgumentException("增量备份起始位点不能为空");
        }
        BinlogPosition startPosition = BinlogPosition.fromString(rawStart);
        if (!startPosition.isValid()) {
            throw new IllegalArgumentException("增量备份起始位点格式非法: " + rawStart);
        }
        return startPosition;
    }

    private BinlogPosition queryCurrentBinlogPosition(BackupRequest.DatabaseConfig dbConfig) throws Exception {
        if (dbConfig == null) {
            throw new IllegalArgumentException("数据库配置不能为空");
        }
        try (Connection connection = openMysqlConnection(dbConfig);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW MASTER STATUS")) {
            if (!resultSet.next()) {
                throw new IllegalStateException("SHOW MASTER STATUS 返回空结果，无法获取binlog位点");
            }
            String file = resultSet.getString("File");
            long position = resultSet.getLong("Position");
            if (file == null || file.isBlank() || position <= 0) {
                throw new IllegalStateException("获取到非法binlog位点: file=" + file + ", position=" + position);
            }
            return BinlogPosition.of(file, position);
        }
    }

    private List<String> queryAvailableBinlogFiles(BackupRequest.DatabaseConfig dbConfig) throws Exception {
        if (dbConfig == null) {
            throw new IllegalArgumentException("数据库配置不能为空");
        }
        try (Connection connection = openMysqlConnection(dbConfig);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW BINARY LOGS")) {
            List<String> files = new ArrayList<>();
            while (resultSet.next()) {
                String file = resultSet.getString("Log_name");
                if (file != null && !file.isBlank()) {
                    files.add(file);
                }
            }
            if (files.isEmpty()) {
                throw new IllegalStateException("SHOW BINARY LOGS 返回空结果，无法解析增量区间");
            }
            return files;
        }
    }

    List<String> resolveBinlogFilesInRange(List<String> orderedFiles,
                                           BinlogPosition startPosition,
                                           BinlogPosition endPosition) {
        if (orderedFiles == null || orderedFiles.isEmpty()) {
            throw new IllegalArgumentException("binlog文件列表不能为空");
        }
        if (startPosition == null || endPosition == null) {
            throw new IllegalArgumentException("增量位点不能为空");
        }

        int startIndex = orderedFiles.indexOf(startPosition.getFilename());
        int endIndex = orderedFiles.indexOf(endPosition.getFilename());
        if (startIndex < 0) {
            throw new IllegalStateException("起始binlog文件不存在: " + startPosition.getFilename());
        }
        if (endIndex < 0) {
            throw new IllegalStateException("结束binlog文件不存在: " + endPosition.getFilename());
        }
        if (startIndex > endIndex) {
            throw new IllegalStateException("增量区间非法: startFile在endFile之后, start="
                    + startPosition.getFilename() + ", end=" + endPosition.getFilename());
        }
        return new ArrayList<>(orderedFiles.subList(startIndex, endIndex + 1));
    }

    private void exportBinlogRange(BackupRequest.DatabaseConfig dbConfig,
                                   BinlogPosition startPosition,
                                   BinlogPosition endPosition,
                                   File outputFile) throws Exception {
        List<String> files = resolveBinlogFilesInRange(
                queryAvailableBinlogFiles(dbConfig), startPosition, endPosition);

        boolean append = false;
        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i);
            Long start = i == 0 ? startPosition.getPosition() : null;
            Long stop = i == files.size() - 1 ? endPosition.getPosition() : null;
            executeMysqlBinlogCommand(dbConfig, file, start, stop, outputFile, append);
            append = true;
        }
    }

    private void executeMysqlBinlogCommand(BackupRequest.DatabaseConfig dbConfig,
                                           String binlogFile,
                                           Long startPosition,
                                           Long stopPosition,
                                           File outputFile,
                                           boolean append) throws Exception {
        List<String> command = buildMysqlBinlogCommandArgs(dbConfig, binlogFile, startPosition, stopPosition);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyMysqlPassword(processBuilder, dbConfig.getPassword());
        processBuilder.redirectOutput(append
                ? ProcessBuilder.Redirect.appendTo(outputFile)
                : ProcessBuilder.Redirect.to(outputFile));
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();
        consumeProcessOutput(process.getInputStream(), "INFO");
        consumeProcessOutput(process.getErrorStream(), "ERROR");

        boolean completed = process.waitFor(backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("mysqlbinlog执行超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorMsg = outputFile.exists() ? Files.readString(outputFile.toPath()) : "unknown";
            throw new RuntimeException("mysqlbinlog执行失败, 退出码: " + exitCode + ", 错误: " + errorMsg);
        }
    }

    private Connection openMysqlConnection(BackupRequest.DatabaseConfig dbConfig) throws Exception {
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                dbConfig.getHost(), dbConfig.getPort());
        return DriverManager.getConnection(jdbcUrl, dbConfig.getUsername(), dbConfig.getPassword());
    }

    private void applyMysqlPassword(ProcessBuilder processBuilder, String password) {
        if (password != null && !password.isEmpty()) {
            processBuilder.environment().put("MYSQL_PWD", password);
        }
    }

    /**
     * 获取输出文件路径
     */
    private String getOutputFilePath(BackupRequest request, String type) {
        String basePath = backupProperties.getStorage().getLocal().getBasePath();
        String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        String filename = String.format("%s_%s_%s.sql", request.getTaskId(), type, timestamp);

        Path outputPath = Paths.get(basePath, "mysql", filename);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            log.error("创建目录失败: {}", outputPath.getParent(), e);
        }

        return outputPath.toString();
    }

    @Override
    protected BackupArtifact doBackup(BackupRequest request, BackupHistory history) throws Exception {
        String backupType = request.getBackupType();
        if (backupType == null) {
            throw new IllegalArgumentException("备份类型不能为空");
        }

        if ("full".equalsIgnoreCase(backupType)) {
            return executeFull(request);
        }

        if ("incremental".equalsIgnoreCase(backupType)) {
            IncrementalBackupRequest incrementalRequest;
            if (request instanceof IncrementalBackupRequest existingRequest) {
                incrementalRequest = existingRequest;
            } else {
                incrementalRequest = new IncrementalBackupRequest();
                incrementalRequest.setTaskId(request.getTaskId());
                incrementalRequest.setDatasourceType(request.getDatasourceType());
                incrementalRequest.setBackupType(request.getBackupType());
                incrementalRequest.setStrategyJson(request.getStrategyJson());
                incrementalRequest.setStoragePolicyJson(request.getStoragePolicyJson());
                incrementalRequest.setDatabaseConfig(request.getDatabaseConfig());
                incrementalRequest.setStartTime(request.getStartTime());
                incrementalRequest.setStartPosition(request.getStartPosition());
                incrementalRequest.setParameters(request.getParameters());
                incrementalRequest.setBinlogStartPosition(request.getStartPosition());
            }
            return executeIncremental(incrementalRequest);
        }

        throw new IllegalArgumentException("不支持的备份类型: " + backupType);
    }
}

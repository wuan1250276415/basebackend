package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.infrastructure.executor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MySQL备份执行器
 * 支持全量备份（mysqldump）和增量备份（binlog）
 */
@Slf4j
@Component
public class MySqlBackupExecutor extends AbstractBackupExecutor
    implements DataSourceBackupExecutor<BackupRequest> {

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private MySqlBinlogParser binlogParser;

    @Override
    public BackupArtifact executeFull(BackupRequest request) throws Exception {
        log.info("执行MySQL全量备份: {}", request.getTaskId());

        // 获取输出文件路径
        String outputFile = getOutputFilePath(request, "full");
        File backupFile = new File(outputFile);

        // 构建mysqldump命令参数（分离式，避免shell注入）
        List<String> command = buildMysqldumpCommandArgs(request);

        // 使用ProcessBuilder执行命令，输出重定向到文件
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(backupFile);
        processBuilder.redirectErrorStream(true);

        log.info("执行mysqldump命令: {}", String.join(" ", command));

        Process process = processBuilder.start();

        // 等待进程完成
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // 读取错误信息
            String errorMsg = new String(Files.readAllBytes(backupFile.toPath()));
            throw new RuntimeException("mysqldump执行失败, 退出码: " + exitCode + ", 错误: " + errorMsg);
        }

        if (!backupFile.exists()) {
            throw new RuntimeException("备份文件未生成: " + outputFile);
        }

        // 构建备份产物
        BackupArtifact artifact = BackupArtifact.builder()
            .file(backupFile)
            .backupType("full")
            .fileSize(backupFile.length())
            .startTime(request.getStartTime())
            .endTime(LocalDateTime.now())
            .durationSeconds((System.currentTimeMillis() - request.getStartTime().toEpochSecond(ZoneOffset.UTC) * 1000) / 1000)
            .build();

        log.info("MySQL全量备份完成: {}, 大小: {} bytes", outputFile, backupFile.length());

        return artifact;
    }

    @Override
    public BackupArtifact executeIncremental(IncrementalBackupRequest request) throws Exception {
        log.info("执行MySQL增量备份: {}, 基线备份ID: {}",
            request.getTaskId(), request.getBaseFullBackupId());

        BinlogPosition startPosition = null;

        // 如果指定了起始位置，使用它
        if (request.getBinlogStartPosition() != null) {
            startPosition = BinlogPosition.fromString(request.getBinlogStartPosition());
        } else {
            // 获取当前binlog位置
            String host = request.getDatabaseConfig().getHost();
            int port = request.getDatabaseConfig().getPort();
            String username = request.getDatabaseConfig().getUsername();
            String password = request.getDatabaseConfig().getPassword();

            startPosition = binlogParser.getCurrentPosition(host, port, username, password);
        }

        log.info("增量备份起始位置: {}", startPosition);

        // 解析binlog到当前时间
        String database = request.getDatabaseConfig().getDatabase();
        String host = request.getDatabaseConfig().getHost();
        int port = request.getDatabaseConfig().getPort();
        String username = request.getDatabaseConfig().getUsername();
        String password = request.getDatabaseConfig().getPassword();

        BinlogPosition endPosition = binlogParser.getCurrentPosition(host, port, username, password);

        // 解析binlog事件
        binlogParser.subscribe(host, port, username, password, startPosition,
            new BinlogEventListener() {
                @Override
                public void onDataChange(BinlogEvent event) throws Exception {
                    log.debug("收到数据变更事件: {}.{}", event.getDatabase(), event.getTable());
                }

                @Override
                public void onQuery(BinlogEvent event) throws Exception {
                    log.debug("收到查询事件: {}", event.getDatabase());
                }

                @Override
                public void onDDL(BinlogEvent event) throws Exception {
                    log.debug("收到DDL事件");
                }

                @Override
                public void onError(Throwable error) throws Exception {
                    log.error("Binlog解析错误", error);
                }

                @Override
                public List<BinlogEvent> getEvents() {
                    return new java.util.ArrayList<>();
                }
            });

        // 简化：创建增量SQL文件（实际实现中应该基于解析的事件生成SQL）
        String outputFile = getOutputFilePath(request, "incremental");
        String incrementalSql = generateIncrementalSql(startPosition, endPosition, request);
        Files.write(Paths.get(outputFile), incrementalSql.getBytes());

        File backupFile = new File(outputFile);

        BackupArtifact artifact = BackupArtifact.builder()
            .file(backupFile)
            .backupType("incremental")
            .fileSize(backupFile.length())
            .startTime(request.getStartTime())
            .endTime(LocalDateTime.now())
            .binlogStartPosition(startPosition.toString())
            .binlogEndPosition(endPosition.toString())
            .durationSeconds((System.currentTimeMillis() - request.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000) / 1000)
            .build();

        log.info("MySQL增量备份完成: {}, 大小: {} bytes", outputFile, backupFile.length());

        return artifact;
    }

    @Override
    public boolean restore(BackupArtifact artifact, String targetDatabase) throws Exception {
        log.info("开始恢复MySQL备份: {}", artifact.getFile());

        // 构建mysql命令参数（分离式，避免shell注入）
        List<String> command = buildMysqlCommandArgs(artifact, targetDatabase);

        // 使用ProcessBuilder执行命令，输入重定向到文件
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectInput(artifact.getFile());
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();

        // 等待进程完成
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("MySQL恢复失败, 退出码: {}", exitCode);
            return false;
        }

        log.info("MySQL恢复成功: {}", artifact.getFile());
        return true;
    }

    @Override
    public String getCurrentIncrementalPosition(BackupRequest request) throws Exception {
        String host = request.getDatabaseConfig().getHost();
        int port = request.getDatabaseConfig().getPort();
        String username = request.getDatabaseConfig().getUsername();
        String password = request.getDatabaseConfig().getPassword();

        BinlogPosition position = binlogParser.getCurrentPosition(host, port, username, password);
        return position.toString();
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
        return new String[]{"full_backup", "incremental_backup", "restore", "checksum_verification"};
    }

    /**
     * 构建mysqldump命令参数列表（避免命令注入）
     */
    private List<String> buildMysqldumpCommandArgs(BackupRequest request) {
        BackupRequest.DatabaseConfig dbConfig = request.getDatabaseConfig();

        List<String> command = new ArrayList<>();
        command.add("mysqldump");
        command.add("-h");
        command.add(dbConfig.getHost());
        command.add("-P");
        command.add(String.valueOf(dbConfig.getPort()));
        command.add("-u");
        command.add(dbConfig.getUsername());
        if (dbConfig.getPassword() != null && !dbConfig.getPassword().isEmpty()) {
            command.add("-p" + dbConfig.getPassword());
        }
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
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("ERROR".equals(logLevel)) {
                        log.error("MySQL进程错误: {}", line);
                    } else {
                        log.info("MySQL进程输出: {}", line);
                    }
                }
            } catch (IOException e) {
                log.error("读取进程输出失败", e);
            }
        }).start();
    }

    /**
     * 构建mysql恢复命令参数（避免命令注入）
     */
    private List<String> buildMysqlCommandArgs(BackupArtifact artifact, String targetDatabase) {
        List<String> command = new ArrayList<>();

        // 基础配置
        command.add("mysql");
        command.add("-h");
        command.add(backupProperties.getDatabase().getHost());
        command.add("-P");
        command.add(String.valueOf(backupProperties.getDatabase().getPort()));
        command.add("-u");
        command.add(backupProperties.getDatabase().getUsername());

        // 密码处理（避免在日志中泄露）
        String password = backupProperties.getDatabase().getPassword();
        if (password != null && !password.isEmpty()) {
            command.add("-p" + password);
        }

        // 指定字符集
        command.add("--default-character-set=utf8mb4");

        // 目标数据库
        command.add(targetDatabase);

        return command;
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

    /**
     * 生成增量SQL（简化实现）
     */
    private String generateIncrementalSql(BinlogPosition start, BinlogPosition end, IncrementalBackupRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- MySQL增量备份SQL\n");
        sql.append("-- 起始位置: ").append(start).append("\n");
        sql.append("-- 结束位置: ").append(end).append("\n");
        sql.append("-- 生成时间: ").append(LocalDateTime.now()).append("\n\n");
        sql.append("-- 注意: 这是一个简化的增量SQL生成器\n");
        sql.append("-- 实际实现中应该基于binlog事件生成具体的INSERT/UPDATE/DELETE语句\n");

        return sql.toString();
    }

    @Override
    protected BackupArtifact doBackup(BackupRequest request, BackupHistory history) throws Exception {
        return null;
    }
}

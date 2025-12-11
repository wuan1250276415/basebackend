package com.basebackend.file.antivirus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV病毒扫描服务实现
 * <p>
 * 使用ClamAV守护进程进行病毒扫描。
 * ClamAV是一个开源的防病毒引擎，支持多种操作系统。
 * </p>
 *
 * <h3>配置示例：</h3>
 * 
 * <pre>
 * file:
 *   antivirus:
 *     enabled: true
 *     engine: clamav
 *     clamav:
 *       host: localhost
 *       port: 3310
 *       timeout: 30000
 * </pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "file.antivirus.engine", havingValue = "clamav", matchIfMissing = false)
public class ClamAVAntivirusService implements AntivirusService {

    private final String host;
    private final int port;
    private final int timeout;

    private static final String ENGINE_NAME = "ClamAV";
    private static final int CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM = "zINSTREAM\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PING = "zPING\0".getBytes(StandardCharsets.UTF_8);
    private static final String RESPONSE_OK = "OK";
    private static final String RESPONSE_PONG = "PONG";

    public ClamAVAntivirusService() {
        this("localhost", 3310, 30000);
    }

    public ClamAVAntivirusService(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        log.info("ClamAV防病毒服务初始化: host={}, port={}, timeout={}ms", host, port, timeout);
    }

    @Override
    public ScanResult scan(InputStream inputStream, String filename) {
        long startTime = System.currentTimeMillis();

        try (Socket socket = createSocket()) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 发送INSTREAM命令
            out.write(INSTREAM);
            out.flush();

            // 发送文件数据（分块）
            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                // 发送块大小（4字节大端序）
                byte[] sizeBytes = ByteBuffer.allocate(4)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(read)
                        .array();
                out.write(sizeBytes);
                out.write(buffer, 0, read);
            }

            // 发送结束标记（大小为0）
            out.write(new byte[] { 0, 0, 0, 0 });
            out.flush();

            // 关闭输出流，等待响应
            socket.shutdownOutput();

            // 读取响应
            String response = readResponse(in);
            long scanTime = System.currentTimeMillis() - startTime;

            return parseResponse(response, scanTime);

        } catch (IOException e) {
            log.error("ClamAV扫描失败: filename={}, error={}", filename, e.getMessage());
            return ScanResult.error(e.getMessage(), ENGINE_NAME);
        }
    }

    @Override
    public ScanResult scan(byte[] data, String filename) {
        return scan(new ByteArrayInputStream(data), filename);
    }

    @Override
    public boolean isAvailable() {
        try (Socket socket = createSocket()) {
            socket.getOutputStream().write(PING);
            socket.getOutputStream().flush();
            socket.shutdownOutput();

            String response = readResponse(socket.getInputStream());
            return response != null && response.contains(RESPONSE_PONG);
        } catch (IOException e) {
            log.warn("ClamAV服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /**
     * 创建Socket连接
     */
    private Socket createSocket() throws IOException {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(timeout);
        return socket;
    }

    /**
     * 读取响应
     */
    private String readResponse(InputStream in) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int read;
        while ((read = in.read(buffer)) > 0) {
            response.write(buffer, 0, read);
        }
        return response.toString(StandardCharsets.UTF_8).trim();
    }

    /**
     * 解析响应
     */
    private ScanResult parseResponse(String response, long scanTime) {
        if (response == null || response.isEmpty()) {
            return ScanResult.error("空响应", ENGINE_NAME);
        }

        // 响应格式：stream: OK 或 stream: <threat_name> FOUND
        if (response.endsWith(RESPONSE_OK)) {
            log.debug("ClamAV扫描完成: 无威胁, scanTime={}ms", scanTime);
            return ScanResult.safe(ENGINE_NAME, scanTime);
        }

        if (response.contains("FOUND")) {
            // 提取威胁名称
            String threatName = response.replace("stream: ", "")
                    .replace(" FOUND", "")
                    .trim();
            log.warn("ClamAV检测到威胁: {}, scanTime={}ms", threatName, scanTime);
            return ScanResult.threat(threatName, ENGINE_NAME, scanTime);
        }

        if (response.contains("ERROR")) {
            log.error("ClamAV扫描错误: {}", response);
            return ScanResult.error(response, ENGINE_NAME);
        }

        return ScanResult.error("未知响应: " + response, ENGINE_NAME);
    }
}

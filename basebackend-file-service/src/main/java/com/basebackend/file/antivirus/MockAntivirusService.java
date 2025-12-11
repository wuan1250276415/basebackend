package com.basebackend.file.antivirus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 模拟病毒扫描服务（仅用于开发和测试）
 * <p>
 * 基于简单的签名匹配进行模拟扫描，不具备真正的防病毒能力。
 * 生产环境请使用ClamAV或其他专业杀毒引擎。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "file.antivirus.engine", havingValue = "mock", matchIfMissing = true)
public class MockAntivirusService implements AntivirusService {

    private static final String ENGINE_NAME = "MockAV";

    /** 模拟病毒签名（EICAR测试文件） */
    private static final String EICAR_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    /** 其他模拟恶意签名 */
    private static final String[] MALICIOUS_SIGNATURES = {
            "<%eval", // PHP WebShell
            "<%execute", // ASP WebShell
            "Runtime.getRuntime().exec", // Java命令执行
            "<script>document.cookie", // XSS注入
            "SELECT * FROM", // SQL注入（简单检测）
    };

    @Override
    public ScanResult scan(InputStream inputStream, String filename) {
        long startTime = System.currentTimeMillis();

        try {
            byte[] data = readAllBytes(inputStream);
            return scan(data, filename);
        } catch (IOException e) {
            log.error("读取文件失败: {}", e.getMessage());
            return ScanResult.error(e.getMessage(), ENGINE_NAME);
        }
    }

    @Override
    public ScanResult scan(byte[] data, String filename) {
        long startTime = System.currentTimeMillis();

        try {
            String content = new String(data);

            // 检查EICAR测试签名
            if (content.contains(EICAR_SIGNATURE)) {
                long scanTime = System.currentTimeMillis() - startTime;
                log.warn("MockAV检测到EICAR测试病毒: filename={}", filename);
                return ScanResult.threat("EICAR-Test-File", ENGINE_NAME, scanTime);
            }

            // 检查其他恶意签名
            for (String signature : MALICIOUS_SIGNATURES) {
                if (content.contains(signature)) {
                    long scanTime = System.currentTimeMillis() - startTime;
                    log.warn("MockAV检测到可疑内容: filename={}, signature={}", filename, signature);
                    return ScanResult.threat("Suspicious.Content." +
                            signature.replaceAll("[^a-zA-Z0-9]", ""), ENGINE_NAME, scanTime);
                }
            }

            long scanTime = System.currentTimeMillis() - startTime;
            log.debug("MockAV扫描完成: filename={}, size={}, scanTime={}ms", filename, data.length, scanTime);
            return ScanResult.safe(ENGINE_NAME, scanTime);

        } catch (Exception e) {
            log.error("MockAV扫描异常: filename={}, error={}", filename, e.getMessage());
            return ScanResult.error(e.getMessage(), ENGINE_NAME);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /**
     * 读取所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int read;
        while ((read = inputStream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }
}

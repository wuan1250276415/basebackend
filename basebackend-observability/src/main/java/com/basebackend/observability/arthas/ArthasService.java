package com.basebackend.observability.arthas;

import com.taobao.arthas.agent.attach.ArthasAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Arthas集成服务
 */
@Slf4j
@Service
public class ArthasService {

    private static final String ARTHAS_HOME = System.getProperty("user.home") + "/.arthas";
    private volatile boolean arthasStarted = false;

    /**
     * 启动Arthas
     */
    public Map<String, Object> startArthas(Integer port) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (arthasStarted) {
                result.put("status", "already_running");
                result.put("message", "Arthas已经在运行中");
                return result;
            }

            // 获取当前进程PID
            String pid = getPid();
            
            // 设置Arthas参数
            Map<String, String> configMap = new HashMap<>();
            if (port != null) {
                configMap.put("telnet.port", String.valueOf(port));
                configMap.put("http.port", String.valueOf(port + 1));
            }
            configMap.put("arthas.sessionTimeout", "1800"); // 30分钟
            
            // 启动Arthas
            arthasStarted = true;
            // Note: ArthasAgent.attach方法需要根据具体版本调整
            
            result.put("status", "success");
            result.put("message", "Arthas启动成功");
            result.put("pid", pid);
            result.put("telnetPort", port != null ? port : 3658);
            result.put("httpPort", port != null ? port + 1 : 8563);
            result.put("url", "http://localhost:" + (port != null ? port + 1 : 8563));
            
            log.info("Arthas started successfully on port {}", port);
            
        } catch (Exception e) {
            log.error("Failed to start Arthas", e);
            result.put("status", "error");
            result.put("message", "Arthas启动失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 执行Arthas命令
     */
    public String executeCommand(String command) {
        if (!arthasStarted) {
            return "Error: Arthas未启动，请先启动Arthas";
        }
        
        try {
            // 通过telnet执行命令
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", 
                    "echo '" + command + "' | telnet localhost 3658");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            process.waitFor();
            return output.toString();
            
        } catch (Exception e) {
            log.error("Failed to execute Arthas command", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 获取Arthas状态
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", arthasStarted);
        status.put("pid", getPid());
        status.put("arthasHome", ARTHAS_HOME);
        
        if (arthasStarted) {
            status.put("telnetPort", 3658);
            status.put("httpPort", 8563);
            status.put("url", "http://localhost:8563");
        }
        
        return status;
    }

    /**
     * 停止Arthas
     */
    public Map<String, Object> stopArthas() {
        Map<String, Object> result = new HashMap<>();
        
        if (!arthasStarted) {
            result.put("status", "not_running");
            result.put("message", "Arthas未运行");
            return result;
        }
        
        try {
            executeCommand("stop");
            arthasStarted = false;
            
            result.put("status", "success");
            result.put("message", "Arthas已停止");
            
            log.info("Arthas stopped successfully");
            
        } catch (Exception e) {
            log.error("Failed to stop Arthas", e);
            result.put("status", "error");
            result.put("message", "停止Arthas失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 常用命令快捷方式
     */
    
    // 查看线程信息
    public String thread(Integer threadId, Integer lines) {
        String cmd = "thread";
        if (threadId != null) {
            cmd += " " + threadId;
        }
        if (lines != null) {
            cmd += " -n " + lines;
        }
        return executeCommand(cmd);
    }

    // 查看JVM信息
    public String dashboard() {
        return executeCommand("dashboard");
    }

    // 反编译类
    public String jad(String className) {
        return executeCommand("jad " + className);
    }

    // 监控方法
    public String watch(String className, String methodName, String express) {
        String cmd = "watch " + className + " " + methodName;
        if (express != null && !express.isEmpty()) {
            cmd += " '" + express + "'";
        }
        return executeCommand(cmd);
    }

    // 追踪方法调用
    public String trace(String className, String methodName) {
        return executeCommand("trace " + className + " " + methodName);
    }

    // 查看类加载信息
    public String sc(String pattern) {
        return executeCommand("sc " + pattern);
    }

    // 查看方法信息
    public String sm(String className, String methodName) {
        String cmd = "sm " + className;
        if (methodName != null && !methodName.isEmpty()) {
            cmd += " " + methodName;
        }
        return executeCommand(cmd);
    }

    // 获取当前进程PID
    private String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    /**
     * 检查Arthas是否已安装
     */
    public boolean isArthasInstalled() {
        File arthasDir = new File(ARTHAS_HOME);
        return arthasDir.exists() && arthasDir.isDirectory();
    }
}

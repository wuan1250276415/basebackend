package com.basebackend.logging.monitoring.exporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Grafana 仪表板导出工具
 *
 * 提供 Grafana 仪表板的导入、导出和管理功能。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class GrafanaDashboardExporter {

    private final RestTemplate restTemplate;

    public GrafanaDashboardExporter() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 导入仪表板到 Grafana
     */
    public ImportResult importDashboard(String grafanaUrl, String apiToken, String dashboardJson) {
        try {
            String payload = "{\"dashboard\":" + dashboardJson + ",\"overwrite\":true}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaUrl + "/api/dashboards/db",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("仪表板导入成功");
                return ImportResult.success("仪表板导入成功");
            } else {
                log.error("仪表板导入失败: {}", response.getStatusCode());
                return ImportResult.failure("HTTP " + response.getStatusCode().value());
            }

        } catch (ResourceAccessException e) {
            log.error("无法连接到 Grafana", e);
            return ImportResult.failure("连接失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("导入仪表板异常", e);
            return ImportResult.failure("异常: " + e.getMessage());
        }
    }

    /**
     * 导出仪表板
     */
    public ExportResult exportDashboard(String grafanaUrl, String apiToken, String uid) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaUrl + "/api/dashboards/uid/" + uid,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String dashboardJson = response.getBody();
                log.info("仪表板导出成功: {}", uid);
                return ExportResult.success(dashboardJson);
            } else {
                log.error("仪表板导出失败: {}", response.getStatusCode());
                return ExportResult.failure("HTTP " + response.getStatusCode().value());
            }

        } catch (ResourceAccessException e) {
            log.error("无法连接到 Grafana", e);
            return ExportResult.failure("连接失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("导出仪表板异常", e);
            return ExportResult.failure("异常: " + e.getMessage());
        }
    }

    /**
     * 创建数据源
     */
    public boolean createDatasource(String grafanaUrl, String apiToken, DatasourceConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> payload = new HashMap<>();
            payload.put("name", config.getName());
            payload.put("type", config.getType());
            payload.put("url", config.getUrl());
            payload.put("access", "proxy");
            payload.put("isDefault", config.isDefault());

            if (config.getBasicAuth() != null) {
                payload.put("basicAuth", true);
                payload.put("basicAuthUser", config.getBasicAuth().getUsername());
                payload.put("secureJsonData", Map.of(
                    "basicAuthPassword", config.getBasicAuth().getPassword()
                ));
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaUrl + "/api/datasources",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("数据源创建成功: {}", config.getName());
            } else {
                log.error("数据源创建失败: {}", response.getStatusCode());
            }

            return success;

        } catch (Exception e) {
            log.error("创建数据源异常", e);
            return false;
        }
    }

    /**
     * 将 JSON 转换为 Base64
     */
    public String toBase64(String dashboardJson) {
        return Base64.getEncoder().encodeToString(
                dashboardJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 从 Base64 解码 JSON
     */
    public String fromBase64(String base64) {
        byte[] decoded = Base64.getDecoder().decode(base64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * 获取仪表板列表
     */
    public DashboardListResult listDashboards(String grafanaUrl, String apiToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaUrl + "/api/search?query=basebackend",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return DashboardListResult.success(response.getBody());
            } else {
                return DashboardListResult.failure("HTTP " + response.getStatusCode().value());
            }

        } catch (Exception e) {
            log.error("获取仪表板列表异常", e);
            return DashboardListResult.failure("异常: " + e.getMessage());
        }
    }

    /**
     * 删除仪表板
     */
    public boolean deleteDashboard(String grafanaUrl, String apiToken, String uid) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    grafanaUrl + "/api/dashboards/uid/" + uid,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("仪表板删除成功: {}", uid);
            } else {
                log.error("仪表板删除失败: {}", response.getStatusCode());
            }

            return success;

        } catch (Exception e) {
            log.error("删除仪表板异常", e);
            return false;
        }
    }

    /**
     * 导入结果
     */
    public static class ImportResult {
        private final boolean success;
        private final String message;

        private ImportResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ImportResult success(String message) {
            return new ImportResult(true, message);
        }

        public static ImportResult failure(String message) {
            return new ImportResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 导出结果
     */
    public static class ExportResult {
        private final boolean success;
        private final String message;
        private final String dashboardJson;

        private ExportResult(boolean success, String message, String dashboardJson) {
            this.success = success;
            this.message = message;
            this.dashboardJson = dashboardJson;
        }

        public static ExportResult success(String dashboardJson) {
            return new ExportResult(true, "导出成功", dashboardJson);
        }

        public static ExportResult failure(String message) {
            return new ExportResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getDashboardJson() {
            return dashboardJson;
        }
    }

    /**
     * 仪表板列表结果
     */
    public static class DashboardListResult {
        private final boolean success;
        private final String message;
        private final String listJson;

        private DashboardListResult(boolean success, String message, String listJson) {
            this.success = success;
            this.message = message;
            this.listJson = listJson;
        }

        public static DashboardListResult success(String listJson) {
            return new DashboardListResult(true, "获取成功", listJson);
        }

        public static DashboardListResult failure(String message) {
            return new DashboardListResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getListJson() {
            return listJson;
        }
    }

    /**
     * 数据源配置
     */
    public static class DatasourceConfig {
        private String name;
        private String type;
        private String url;
        private boolean isDefault = false;
        private BasicAuthConfig basicAuth;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean aDefault) {
            isDefault = aDefault;
        }

        public BasicAuthConfig getBasicAuth() {
            return basicAuth;
        }

        public void setBasicAuth(BasicAuthConfig basicAuth) {
            this.basicAuth = basicAuth;
        }

        public static class BasicAuthConfig {
            private String username;
            private String password;

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }
}

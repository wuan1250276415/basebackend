/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  jakarta.validation.constraints.NotBlank
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.cloud.context.config.annotation.RefreshScope
 *  org.springframework.validation.annotation.Validated
 */
package com.basebackend.nacos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@RefreshScope
@ConfigurationProperties(prefix="nacos")
@Validated
public class NacosConfigProperties {
    @NotBlank(message="\u73af\u5883\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a")
    private @NotBlank(message="\u73af\u5883\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a") String environment = "dev";
    private String tenantId = "public";
    private Long appId;
    @Valid
    private Config config = new Config();
    @Valid
    private Discovery discovery = new Discovery();

    @Generated
    public NacosConfigProperties() {
    }

    @Generated
    public String getEnvironment() {
        return this.environment;
    }

    @Generated
    public String getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getAppId() {
        return this.appId;
    }

    @Generated
    public Config getConfig() {
        return this.config;
    }

    @Generated
    public Discovery getDiscovery() {
        return this.discovery;
    }

    @Generated
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Generated
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setAppId(Long appId) {
        this.appId = appId;
    }

    @Generated
    public void setConfig(Config config) {
        this.config = config;
    }

    @Generated
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NacosConfigProperties)) {
            return false;
        }
        NacosConfigProperties other = (NacosConfigProperties)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$appId = this.getAppId();
        Long other$appId = other.getAppId();
        if (this$appId == null ? other$appId != null : !((Object)this$appId).equals(other$appId)) {
            return false;
        }
        String this$environment = this.getEnvironment();
        String other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        Config this$config = this.getConfig();
        Config other$config = other.getConfig();
        if (this$config == null ? other$config != null : !((Object)this$config).equals(other$config)) {
            return false;
        }
        Discovery this$discovery = this.getDiscovery();
        Discovery other$discovery = other.getDiscovery();
        return !(this$discovery == null ? other$discovery != null : !((Object)this$discovery).equals(other$discovery));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof NacosConfigProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $appId = this.getAppId();
        result = result * 59 + ($appId == null ? 43 : ((Object)$appId).hashCode());
        String $environment = this.getEnvironment();
        result = result * 59 + ($environment == null ? 43 : $environment.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        Config $config = this.getConfig();
        result = result * 59 + ($config == null ? 43 : ((Object)$config).hashCode());
        Discovery $discovery = this.getDiscovery();
        result = result * 59 + ($discovery == null ? 43 : ((Object)$discovery).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "NacosConfigProperties(environment=" + this.getEnvironment() + ", tenantId=" + this.getTenantId() + ", appId=" + this.getAppId() + ", config=" + String.valueOf(this.getConfig()) + ", discovery=" + String.valueOf(this.getDiscovery()) + ")";
    }

    private static String maskSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "******";
    }

    public static class Config {
        private boolean enabled = true;
        @NotBlank(message="nacos.config.server-addr \u4e0d\u80fd\u4e3a\u7a7a")
        private @NotBlank(message="nacos.config.server-addr \u4e0d\u80fd\u4e3a\u7a7a") String serverAddr = "127.0.0.1:8848";
        private String namespace = "public";
        private String group = "DEFAULT_GROUP";
        private String fileExtension = "yml";
        private boolean importCheckEnabled = true;
        private Boolean refreshEnabled;
        private String username = "";
        private String password = "";
        @Valid
        private List<SharedConfig> sharedConfigs = new ArrayList<SharedConfig>();
        @Valid
        private List<ExtensionConfig> extensionConfigs = new ArrayList<ExtensionConfig>();

        @Generated
        public Config() {
        }

        @Generated
        public boolean isEnabled() {
            return this.enabled;
        }

        @Generated
        public String getServerAddr() {
            return this.serverAddr;
        }

        @Generated
        public String getNamespace() {
            return this.namespace;
        }

        @Generated
        public String getGroup() {
            return this.group;
        }

        @Generated
        public String getFileExtension() {
            return this.fileExtension;
        }

        @Generated
        public boolean isImportCheckEnabled() {
            return this.importCheckEnabled;
        }

        @Generated
        public Boolean getRefreshEnabled() {
            return this.refreshEnabled;
        }

        @Generated
        public String getUsername() {
            return this.username;
        }

        @Generated
        public String getPassword() {
            return this.password;
        }

        @Generated
        public List<SharedConfig> getSharedConfigs() {
            return this.sharedConfigs;
        }

        @Generated
        public List<ExtensionConfig> getExtensionConfigs() {
            return this.extensionConfigs;
        }

        @Generated
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Generated
        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        @Generated
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        @Generated
        public void setGroup(String group) {
            this.group = group;
        }

        @Generated
        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        @Generated
        public void setImportCheckEnabled(boolean importCheckEnabled) {
            this.importCheckEnabled = importCheckEnabled;
        }

        @Generated
        public void setRefreshEnabled(Boolean refreshEnabled) {
            this.refreshEnabled = refreshEnabled;
        }

        @Generated
        public void setUsername(String username) {
            this.username = username;
        }

        @Generated
        public void setPassword(String password) {
            this.password = password;
        }

        @Generated
        public void setSharedConfigs(List<SharedConfig> sharedConfigs) {
            this.sharedConfigs = sharedConfigs;
        }

        @Generated
        public void setExtensionConfigs(List<ExtensionConfig> extensionConfigs) {
            this.extensionConfigs = extensionConfigs;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Config)) {
                return false;
            }
            Config other = (Config)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isEnabled() != other.isEnabled()) {
                return false;
            }
            if (this.isImportCheckEnabled() != other.isImportCheckEnabled()) {
                return false;
            }
            Boolean this$refreshEnabled = this.getRefreshEnabled();
            Boolean other$refreshEnabled = other.getRefreshEnabled();
            if (this$refreshEnabled == null ? other$refreshEnabled != null : !((Object)this$refreshEnabled).equals(other$refreshEnabled)) {
                return false;
            }
            String this$serverAddr = this.getServerAddr();
            String other$serverAddr = other.getServerAddr();
            if (this$serverAddr == null ? other$serverAddr != null : !this$serverAddr.equals(other$serverAddr)) {
                return false;
            }
            String this$namespace = this.getNamespace();
            String other$namespace = other.getNamespace();
            if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) {
                return false;
            }
            String this$group = this.getGroup();
            String other$group = other.getGroup();
            if (this$group == null ? other$group != null : !this$group.equals(other$group)) {
                return false;
            }
            String this$fileExtension = this.getFileExtension();
            String other$fileExtension = other.getFileExtension();
            if (this$fileExtension == null ? other$fileExtension != null : !this$fileExtension.equals(other$fileExtension)) {
                return false;
            }
            String this$username = this.getUsername();
            String other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
                return false;
            }
            String this$password = this.getPassword();
            String other$password = other.getPassword();
            if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
                return false;
            }
            List<SharedConfig> this$sharedConfigs = this.getSharedConfigs();
            List<SharedConfig> other$sharedConfigs = other.getSharedConfigs();
            if (this$sharedConfigs == null ? other$sharedConfigs != null : !((Object)this$sharedConfigs).equals(other$sharedConfigs)) {
                return false;
            }
            List<ExtensionConfig> this$extensionConfigs = this.getExtensionConfigs();
            List<ExtensionConfig> other$extensionConfigs = other.getExtensionConfigs();
            return !(this$extensionConfigs == null ? other$extensionConfigs != null : !((Object)this$extensionConfigs).equals(other$extensionConfigs));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Config;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isEnabled() ? 79 : 97);
            result = result * 59 + (this.isImportCheckEnabled() ? 79 : 97);
            Boolean $refreshEnabled = this.getRefreshEnabled();
            result = result * 59 + ($refreshEnabled == null ? 43 : ((Object)$refreshEnabled).hashCode());
            String $serverAddr = this.getServerAddr();
            result = result * 59 + ($serverAddr == null ? 43 : $serverAddr.hashCode());
            String $namespace = this.getNamespace();
            result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
            String $group = this.getGroup();
            result = result * 59 + ($group == null ? 43 : $group.hashCode());
            String $fileExtension = this.getFileExtension();
            result = result * 59 + ($fileExtension == null ? 43 : $fileExtension.hashCode());
            String $username = this.getUsername();
            result = result * 59 + ($username == null ? 43 : $username.hashCode());
            String $password = this.getPassword();
            result = result * 59 + ($password == null ? 43 : $password.hashCode());
            List<SharedConfig> $sharedConfigs = this.getSharedConfigs();
            result = result * 59 + ($sharedConfigs == null ? 43 : ((Object)$sharedConfigs).hashCode());
            List<ExtensionConfig> $extensionConfigs = this.getExtensionConfigs();
            result = result * 59 + ($extensionConfigs == null ? 43 : ((Object)$extensionConfigs).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "NacosConfigProperties.Config(enabled=" + this.isEnabled() + ", serverAddr=" + this.getServerAddr() + ", namespace=" + this.getNamespace() + ", group=" + this.getGroup() + ", fileExtension=" + this.getFileExtension() + ", importCheckEnabled=" + this.isImportCheckEnabled() + ", refreshEnabled=" + this.getRefreshEnabled() + ", username=" + NacosConfigProperties.maskSensitive(this.getUsername()) + ", password=" + NacosConfigProperties.maskSensitive(this.getPassword()) + ", sharedConfigs=" + String.valueOf(this.getSharedConfigs()) + ", extensionConfigs=" + String.valueOf(this.getExtensionConfigs()) + ")";
        }

        public static class ExtensionConfig {
            @NotBlank
            private String dataId;
            private boolean refresh = true;
            private String group = "DEFAULT_GROUP";

            @Generated
            public ExtensionConfig() {
            }

            @Generated
            public String getDataId() {
                return this.dataId;
            }

            @Generated
            public boolean isRefresh() {
                return this.refresh;
            }

            @Generated
            public String getGroup() {
                return this.group;
            }

            @Generated
            public void setDataId(String dataId) {
                this.dataId = dataId;
            }

            @Generated
            public void setRefresh(boolean refresh) {
                this.refresh = refresh;
            }

            @Generated
            public void setGroup(String group) {
                this.group = group;
            }

            @Generated
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof ExtensionConfig)) {
                    return false;
                }
                ExtensionConfig other = (ExtensionConfig)o;
                if (!other.canEqual(this)) {
                    return false;
                }
                if (this.isRefresh() != other.isRefresh()) {
                    return false;
                }
                String this$dataId = this.getDataId();
                String other$dataId = other.getDataId();
                if (this$dataId == null ? other$dataId != null : !this$dataId.equals(other$dataId)) {
                    return false;
                }
                String this$group = this.getGroup();
                String other$group = other.getGroup();
                return !(this$group == null ? other$group != null : !this$group.equals(other$group));
            }

            @Generated
            protected boolean canEqual(Object other) {
                return other instanceof ExtensionConfig;
            }

            @Generated
            public int hashCode() {
                int PRIME = 59;
                int result = 1;
                result = result * 59 + (this.isRefresh() ? 79 : 97);
                String $dataId = this.getDataId();
                result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
                String $group = this.getGroup();
                result = result * 59 + ($group == null ? 43 : $group.hashCode());
                return result;
            }

            @Generated
            public String toString() {
                return "NacosConfigProperties.Config.ExtensionConfig(dataId=" + this.getDataId() + ", refresh=" + this.isRefresh() + ", group=" + this.getGroup() + ")";
            }
        }

        public static class SharedConfig {
            @NotBlank
            private String dataId;
            private boolean refresh = true;
            private String group = "DEFAULT_GROUP";

            @Generated
            public SharedConfig() {
            }

            @Generated
            public String getDataId() {
                return this.dataId;
            }

            @Generated
            public boolean isRefresh() {
                return this.refresh;
            }

            @Generated
            public String getGroup() {
                return this.group;
            }

            @Generated
            public void setDataId(String dataId) {
                this.dataId = dataId;
            }

            @Generated
            public void setRefresh(boolean refresh) {
                this.refresh = refresh;
            }

            @Generated
            public void setGroup(String group) {
                this.group = group;
            }

            @Generated
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof SharedConfig)) {
                    return false;
                }
                SharedConfig other = (SharedConfig)o;
                if (!other.canEqual(this)) {
                    return false;
                }
                if (this.isRefresh() != other.isRefresh()) {
                    return false;
                }
                String this$dataId = this.getDataId();
                String other$dataId = other.getDataId();
                if (this$dataId == null ? other$dataId != null : !this$dataId.equals(other$dataId)) {
                    return false;
                }
                String this$group = this.getGroup();
                String other$group = other.getGroup();
                return !(this$group == null ? other$group != null : !this$group.equals(other$group));
            }

            @Generated
            protected boolean canEqual(Object other) {
                return other instanceof SharedConfig;
            }

            @Generated
            public int hashCode() {
                int PRIME = 59;
                int result = 1;
                result = result * 59 + (this.isRefresh() ? 79 : 97);
                String $dataId = this.getDataId();
                result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
                String $group = this.getGroup();
                result = result * 59 + ($group == null ? 43 : $group.hashCode());
                return result;
            }

            @Generated
            public String toString() {
                return "NacosConfigProperties.Config.SharedConfig(dataId=" + this.getDataId() + ", refresh=" + this.isRefresh() + ", group=" + this.getGroup() + ")";
            }
        }
    }

    public static class Discovery {
        private boolean enabled = true;
        @NotBlank(message="nacos.discovery.server-addr \u4e0d\u80fd\u4e3a\u7a7a")
        private @NotBlank(message="nacos.discovery.server-addr \u4e0d\u80fd\u4e3a\u7a7a") String serverAddr = "127.0.0.1:8848";
        private String namespace = "public";
        private String group = "DEFAULT_GROUP";
        private String serviceName;
        private double weight = 1.0;
        private String cluster = "DEFAULT";
        private String username = "";
        private String password = "";
        private Map<String, String> metadata = new HashMap<String, String>();

        @Generated
        public Discovery() {
        }

        @Generated
        public boolean isEnabled() {
            return this.enabled;
        }

        @Generated
        public String getServerAddr() {
            return this.serverAddr;
        }

        @Generated
        public String getNamespace() {
            return this.namespace;
        }

        @Generated
        public String getGroup() {
            return this.group;
        }

        @Generated
        public String getServiceName() {
            return this.serviceName;
        }

        @Generated
        public double getWeight() {
            return this.weight;
        }

        @Generated
        public String getCluster() {
            return this.cluster;
        }

        @Generated
        public String getUsername() {
            return this.username;
        }

        @Generated
        public String getPassword() {
            return this.password;
        }

        @Generated
        public Map<String, String> getMetadata() {
            return this.metadata;
        }

        @Generated
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Generated
        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        @Generated
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        @Generated
        public void setGroup(String group) {
            this.group = group;
        }

        @Generated
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        @Generated
        public void setWeight(double weight) {
            this.weight = weight;
        }

        @Generated
        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        @Generated
        public void setUsername(String username) {
            this.username = username;
        }

        @Generated
        public void setPassword(String password) {
            this.password = password;
        }

        @Generated
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Discovery)) {
                return false;
            }
            Discovery other = (Discovery)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isEnabled() != other.isEnabled()) {
                return false;
            }
            if (Double.compare(this.getWeight(), other.getWeight()) != 0) {
                return false;
            }
            String this$serverAddr = this.getServerAddr();
            String other$serverAddr = other.getServerAddr();
            if (this$serverAddr == null ? other$serverAddr != null : !this$serverAddr.equals(other$serverAddr)) {
                return false;
            }
            String this$namespace = this.getNamespace();
            String other$namespace = other.getNamespace();
            if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) {
                return false;
            }
            String this$group = this.getGroup();
            String other$group = other.getGroup();
            if (this$group == null ? other$group != null : !this$group.equals(other$group)) {
                return false;
            }
            String this$serviceName = this.getServiceName();
            String other$serviceName = other.getServiceName();
            if (this$serviceName == null ? other$serviceName != null : !this$serviceName.equals(other$serviceName)) {
                return false;
            }
            String this$cluster = this.getCluster();
            String other$cluster = other.getCluster();
            if (this$cluster == null ? other$cluster != null : !this$cluster.equals(other$cluster)) {
                return false;
            }
            String this$username = this.getUsername();
            String other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
                return false;
            }
            String this$password = this.getPassword();
            String other$password = other.getPassword();
            if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
                return false;
            }
            Map<String, String> this$metadata = this.getMetadata();
            Map<String, String> other$metadata = other.getMetadata();
            return !(this$metadata == null ? other$metadata != null : !((Object)this$metadata).equals(other$metadata));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Discovery;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isEnabled() ? 79 : 97);
            long $weight = Double.doubleToLongBits(this.getWeight());
            result = result * 59 + (int)($weight >>> 32 ^ $weight);
            String $serverAddr = this.getServerAddr();
            result = result * 59 + ($serverAddr == null ? 43 : $serverAddr.hashCode());
            String $namespace = this.getNamespace();
            result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
            String $group = this.getGroup();
            result = result * 59 + ($group == null ? 43 : $group.hashCode());
            String $serviceName = this.getServiceName();
            result = result * 59 + ($serviceName == null ? 43 : $serviceName.hashCode());
            String $cluster = this.getCluster();
            result = result * 59 + ($cluster == null ? 43 : $cluster.hashCode());
            String $username = this.getUsername();
            result = result * 59 + ($username == null ? 43 : $username.hashCode());
            String $password = this.getPassword();
            result = result * 59 + ($password == null ? 43 : $password.hashCode());
            Map<String, String> $metadata = this.getMetadata();
            result = result * 59 + ($metadata == null ? 43 : ((Object)$metadata).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "NacosConfigProperties.Discovery(enabled=" + this.isEnabled() + ", serverAddr=" + this.getServerAddr() + ", namespace=" + this.getNamespace() + ", group=" + this.getGroup() + ", serviceName=" + this.getServiceName() + ", weight=" + this.getWeight() + ", cluster=" + this.getCluster() + ", username=" + NacosConfigProperties.maskSensitive(this.getUsername()) + ", password=" + NacosConfigProperties.maskSensitive(this.getPassword()) + ", metadata=" + String.valueOf(this.getMetadata()) + ")";
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.model;

import java.io.Serializable;
import java.util.Map;
import lombok.Generated;

public class ServiceInstance
implements Serializable {
    private String serviceName;
    private String groupName;
    private String clusterName;
    private String ip;
    private Integer port;
    private Double weight;
    private Boolean healthy;
    private Boolean enabled;
    private Boolean ephemeral;
    private Map<String, String> metadata;
    private String instanceId;

    @Generated
    public static ServiceInstanceBuilder builder() {
        return new ServiceInstanceBuilder();
    }

    @Generated
    public String getServiceName() {
        return this.serviceName;
    }

    @Generated
    public String getGroupName() {
        return this.groupName;
    }

    @Generated
    public String getClusterName() {
        return this.clusterName;
    }

    @Generated
    public String getIp() {
        return this.ip;
    }

    @Generated
    public Integer getPort() {
        return this.port;
    }

    @Generated
    public Double getWeight() {
        return this.weight;
    }

    @Generated
    public Boolean getHealthy() {
        return this.healthy;
    }

    @Generated
    public Boolean getEnabled() {
        return this.enabled;
    }

    @Generated
    public Boolean getEphemeral() {
        return this.ephemeral;
    }

    @Generated
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    @Generated
    public String getInstanceId() {
        return this.instanceId;
    }

    @Generated
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Generated
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Generated
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Generated
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Generated
    public void setPort(Integer port) {
        this.port = port;
    }

    @Generated
    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Generated
    public void setHealthy(Boolean healthy) {
        this.healthy = healthy;
    }

    @Generated
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setEphemeral(Boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    @Generated
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Generated
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ServiceInstance)) {
            return false;
        }
        ServiceInstance other = (ServiceInstance)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$port = this.getPort();
        Integer other$port = other.getPort();
        if (this$port == null ? other$port != null : !((Object)this$port).equals(other$port)) {
            return false;
        }
        Double this$weight = this.getWeight();
        Double other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !((Object)this$weight).equals(other$weight)) {
            return false;
        }
        Boolean this$healthy = this.getHealthy();
        Boolean other$healthy = other.getHealthy();
        if (this$healthy == null ? other$healthy != null : !((Object)this$healthy).equals(other$healthy)) {
            return false;
        }
        Boolean this$enabled = this.getEnabled();
        Boolean other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        Boolean this$ephemeral = this.getEphemeral();
        Boolean other$ephemeral = other.getEphemeral();
        if (this$ephemeral == null ? other$ephemeral != null : !((Object)this$ephemeral).equals(other$ephemeral)) {
            return false;
        }
        String this$serviceName = this.getServiceName();
        String other$serviceName = other.getServiceName();
        if (this$serviceName == null ? other$serviceName != null : !this$serviceName.equals(other$serviceName)) {
            return false;
        }
        String this$groupName = this.getGroupName();
        String other$groupName = other.getGroupName();
        if (this$groupName == null ? other$groupName != null : !this$groupName.equals(other$groupName)) {
            return false;
        }
        String this$clusterName = this.getClusterName();
        String other$clusterName = other.getClusterName();
        if (this$clusterName == null ? other$clusterName != null : !this$clusterName.equals(other$clusterName)) {
            return false;
        }
        String this$ip = this.getIp();
        String other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) {
            return false;
        }
        Map<String, String> this$metadata = this.getMetadata();
        Map<String, String> other$metadata = other.getMetadata();
        if (this$metadata == null ? other$metadata != null : !((Object)this$metadata).equals(other$metadata)) {
            return false;
        }
        String this$instanceId = this.getInstanceId();
        String other$instanceId = other.getInstanceId();
        return !(this$instanceId == null ? other$instanceId != null : !this$instanceId.equals(other$instanceId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ServiceInstance;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $port = this.getPort();
        result = result * 59 + ($port == null ? 43 : ((Object)$port).hashCode());
        Double $weight = this.getWeight();
        result = result * 59 + ($weight == null ? 43 : ((Object)$weight).hashCode());
        Boolean $healthy = this.getHealthy();
        result = result * 59 + ($healthy == null ? 43 : ((Object)$healthy).hashCode());
        Boolean $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        Boolean $ephemeral = this.getEphemeral();
        result = result * 59 + ($ephemeral == null ? 43 : ((Object)$ephemeral).hashCode());
        String $serviceName = this.getServiceName();
        result = result * 59 + ($serviceName == null ? 43 : $serviceName.hashCode());
        String $groupName = this.getGroupName();
        result = result * 59 + ($groupName == null ? 43 : $groupName.hashCode());
        String $clusterName = this.getClusterName();
        result = result * 59 + ($clusterName == null ? 43 : $clusterName.hashCode());
        String $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        Map<String, String> $metadata = this.getMetadata();
        result = result * 59 + ($metadata == null ? 43 : ((Object)$metadata).hashCode());
        String $instanceId = this.getInstanceId();
        result = result * 59 + ($instanceId == null ? 43 : $instanceId.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ServiceInstance(serviceName=" + this.getServiceName() + ", groupName=" + this.getGroupName() + ", clusterName=" + this.getClusterName() + ", ip=" + this.getIp() + ", port=" + this.getPort() + ", weight=" + this.getWeight() + ", healthy=" + this.getHealthy() + ", enabled=" + this.getEnabled() + ", ephemeral=" + this.getEphemeral() + ", metadata=" + String.valueOf(this.getMetadata()) + ", instanceId=" + this.getInstanceId() + ")";
    }

    @Generated
    public ServiceInstance() {
    }

    @Generated
    public ServiceInstance(String serviceName, String groupName, String clusterName, String ip, Integer port, Double weight, Boolean healthy, Boolean enabled, Boolean ephemeral, Map<String, String> metadata, String instanceId) {
        this.serviceName = serviceName;
        this.groupName = groupName;
        this.clusterName = clusterName;
        this.ip = ip;
        this.port = port;
        this.weight = weight;
        this.healthy = healthy;
        this.enabled = enabled;
        this.ephemeral = ephemeral;
        this.metadata = metadata;
        this.instanceId = instanceId;
    }

    @Generated
    public static class ServiceInstanceBuilder {
        @Generated
        private String serviceName;
        @Generated
        private String groupName;
        @Generated
        private String clusterName;
        @Generated
        private String ip;
        @Generated
        private Integer port;
        @Generated
        private Double weight;
        @Generated
        private Boolean healthy;
        @Generated
        private Boolean enabled;
        @Generated
        private Boolean ephemeral;
        @Generated
        private Map<String, String> metadata;
        @Generated
        private String instanceId;

        @Generated
        ServiceInstanceBuilder() {
        }

        @Generated
        public ServiceInstanceBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder ip(String ip) {
            this.ip = ip;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder port(Integer port) {
            this.port = port;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder weight(Double weight) {
            this.weight = weight;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder healthy(Boolean healthy) {
            this.healthy = healthy;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder ephemeral(Boolean ephemeral) {
            this.ephemeral = ephemeral;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        @Generated
        public ServiceInstanceBuilder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        @Generated
        public ServiceInstance build() {
            return new ServiceInstance(this.serviceName, this.groupName, this.clusterName, this.ip, this.port, this.weight, this.healthy, this.enabled, this.ephemeral, this.metadata, this.instanceId);
        }

        @Generated
        public String toString() {
            return "ServiceInstance.ServiceInstanceBuilder(serviceName=" + this.serviceName + ", groupName=" + this.groupName + ", clusterName=" + this.clusterName + ", ip=" + this.ip + ", port=" + this.port + ", weight=" + this.weight + ", healthy=" + this.healthy + ", enabled=" + this.enabled + ", ephemeral=" + this.ephemeral + ", metadata=" + String.valueOf(this.metadata) + ", instanceId=" + this.instanceId + ")";
        }
    }
}


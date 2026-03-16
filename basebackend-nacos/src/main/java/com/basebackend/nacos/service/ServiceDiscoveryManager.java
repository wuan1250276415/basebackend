/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.naming.NamingService
 *  com.alibaba.nacos.api.naming.listener.EventListener
 *  com.alibaba.nacos.api.naming.pojo.Instance
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.model.ServiceInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ServiceDiscoveryManager {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryManager.class);
    private final NamingService namingService;

    public List<String> getAllServices(int pageNo, int pageSize) {
        try {
            return this.namingService.getServicesOfServer(pageNo, pageSize).getData();
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u670d\u52a1\u5217\u8868\u5931\u8d25", (Throwable)e);
            return new ArrayList<String>();
        }
    }

    public List<ServiceInstance> getServiceInstances(String serviceName, String groupName) {
        try {
            List<Instance> instances = this.namingService.getAllInstances(serviceName, groupName);
            return instances.stream().map(this::convertToServiceInstance).collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u670d\u52a1\u5b9e\u4f8b\u5931\u8d25: serviceName={}, groupName={}", new Object[]{serviceName, groupName, e});
            return new ArrayList<ServiceInstance>();
        }
    }

    public List<ServiceInstance> getHealthyInstances(String serviceName, String groupName) {
        try {
            List<Instance> instances = this.namingService.selectInstances(serviceName, groupName, true);
            return instances.stream().map(this::convertToServiceInstance).collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u5065\u5eb7\u5b9e\u4f8b\u5931\u8d25: serviceName={}, groupName={}", new Object[]{serviceName, groupName, e});
            return new ArrayList<ServiceInstance>();
        }
    }

    public boolean registerInstance(ServiceInstance serviceInstance) {
        try {
            Instance instance = this.convertToNacosInstance(serviceInstance);
            this.namingService.registerInstance(serviceInstance.getServiceName(), serviceInstance.getGroupName(), instance);
            log.info("\u670d\u52a1\u5b9e\u4f8b\u6ce8\u518c\u6210\u529f: {}", (Object)serviceInstance.getInstanceId());
            return true;
        }
        catch (Exception e) {
            log.error("\u670d\u52a1\u5b9e\u4f8b\u6ce8\u518c\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    public boolean deregisterInstance(ServiceInstance serviceInstance) {
        try {
            this.namingService.deregisterInstance(serviceInstance.getServiceName(), serviceInstance.getGroupName(), serviceInstance.getIp(), serviceInstance.getPort().intValue());
            log.info("\u670d\u52a1\u5b9e\u4f8b\u6ce8\u9500\u6210\u529f: {}", (Object)serviceInstance.getInstanceId());
            return true;
        }
        catch (Exception e) {
            log.error("\u670d\u52a1\u5b9e\u4f8b\u6ce8\u9500\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    public boolean enableInstance(String serviceName, String groupName, String ip, int port) {
        try {
            List<Instance> instances = this.namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream().filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port).findFirst().orElse(null);
            if (targetInstance == null) {
                log.warn("\u672a\u627e\u5230\u76ee\u6807\u5b9e\u4f8b: {}:{}", (Object)ip, (Object)port);
                return false;
            }
            targetInstance.setEnabled(true);
            this.namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("\u5b9e\u4f8b\u4e0a\u7ebf\u6210\u529f: {}:{}", (Object)ip, (Object)port);
            return true;
        }
        catch (Exception e) {
            log.error("\u5b9e\u4f8b\u4e0a\u7ebf\u5931\u8d25: {}:{}", new Object[]{ip, port, e});
            return false;
        }
    }

    public boolean disableInstance(String serviceName, String groupName, String ip, int port) {
        try {
            List<Instance> instances = this.namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream().filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port).findFirst().orElse(null);
            if (targetInstance == null) {
                log.warn("\u672a\u627e\u5230\u76ee\u6807\u5b9e\u4f8b: {}:{}", (Object)ip, (Object)port);
                return false;
            }
            targetInstance.setEnabled(false);
            this.namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("\u5b9e\u4f8b\u4e0b\u7ebf\u6210\u529f: {}:{}", (Object)ip, (Object)port);
            return true;
        }
        catch (Exception e) {
            log.error("\u5b9e\u4f8b\u4e0b\u7ebf\u5931\u8d25: {}:{}", new Object[]{ip, port, e});
            return false;
        }
    }

    public boolean updateInstanceWeight(String serviceName, String groupName, String ip, int port, double weight) {
        try {
            List<Instance> instances = this.namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream().filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port).findFirst().orElse(null);
            if (targetInstance == null) {
                log.warn("\u672a\u627e\u5230\u76ee\u6807\u5b9e\u4f8b: {}:{}", (Object)ip, (Object)port);
                return false;
            }
            targetInstance.setWeight(weight);
            this.namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("\u5b9e\u4f8b\u6743\u91cd\u66f4\u65b0\u6210\u529f: {}:{}, weight={}", new Object[]{ip, port, weight});
            return true;
        }
        catch (Exception e) {
            log.error("\u5b9e\u4f8b\u6743\u91cd\u66f4\u65b0\u5931\u8d25: {}:{}", new Object[]{ip, port, e});
            return false;
        }
    }

    public void subscribe(String serviceName, String groupName, EventListener listener) {
        try {
            this.namingService.subscribe(serviceName, groupName, listener);
            log.info("\u8ba2\u9605\u670d\u52a1\u6210\u529f: serviceName={}, groupName={}", (Object)serviceName, (Object)groupName);
        }
        catch (Exception e) {
            log.error("\u8ba2\u9605\u670d\u52a1\u5931\u8d25", (Throwable)e);
        }
    }

    public void unsubscribe(String serviceName, String groupName, EventListener listener) {
        try {
            this.namingService.unsubscribe(serviceName, groupName, listener);
            log.info("\u53d6\u6d88\u8ba2\u9605\u670d\u52a1: serviceName={}, groupName={}", (Object)serviceName, (Object)groupName);
        }
        catch (Exception e) {
            log.error("\u53d6\u6d88\u8ba2\u9605\u670d\u52a1\u5931\u8d25", (Throwable)e);
        }
    }

    private ServiceInstance convertToServiceInstance(Instance instance) {
        return ServiceInstance.builder().serviceName(instance.getServiceName()).clusterName(instance.getClusterName()).ip(instance.getIp()).port(instance.getPort()).weight(instance.getWeight()).healthy(instance.isHealthy()).enabled(instance.isEnabled()).ephemeral(instance.isEphemeral()).metadata(instance.getMetadata()).instanceId(instance.getInstanceId()).build();
    }

    private Instance convertToNacosInstance(ServiceInstance serviceInstance) {
        Instance instance = new Instance();
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setClusterName(serviceInstance.getClusterName());
        instance.setIp(serviceInstance.getIp());
        instance.setPort(serviceInstance.getPort().intValue());
        instance.setWeight(serviceInstance.getWeight().doubleValue());
        instance.setHealthy(serviceInstance.getHealthy().booleanValue());
        instance.setEnabled(serviceInstance.getEnabled().booleanValue());
        instance.setEphemeral(serviceInstance.getEphemeral().booleanValue());
        instance.setMetadata(serviceInstance.getMetadata());
        return instance;
    }

    @Generated
    public ServiceDiscoveryManager(NamingService namingService) {
        this.namingService = namingService;
    }
}

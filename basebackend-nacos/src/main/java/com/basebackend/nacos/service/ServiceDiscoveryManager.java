package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.basebackend.nacos.model.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务发现管理服务
 * 提供服务和实例的查询、上下线管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDiscoveryManager {

    private final NamingService namingService;

    /**
     * 获取所有服务列表
     */
    public List<String> getAllServices(int pageNo, int pageSize) {
        try {
            return namingService.getServicesOfServer(pageNo, pageSize).getData();
        } catch (Exception e) {
            log.error("获取服务列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取服务的所有实例
     */
    public List<ServiceInstance> getServiceInstances(String serviceName, String groupName) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName, groupName);
            return instances.stream()
                    .map(this::convertToServiceInstance)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取服务实例失败: serviceName={}, groupName={}", serviceName, groupName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取健康实例
     */
    public List<ServiceInstance> getHealthyInstances(String serviceName, String groupName) {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, groupName, true);
            return instances.stream()
                    .map(this::convertToServiceInstance)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取健康实例失败: serviceName={}, groupName={}", serviceName, groupName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 注册服务实例
     */
    public boolean registerInstance(ServiceInstance serviceInstance) {
        try {
            Instance instance = convertToNacosInstance(serviceInstance);
            namingService.registerInstance(
                    serviceInstance.getServiceName(),
                    serviceInstance.getGroupName(),
                    instance
            );
            log.info("服务实例注册成功: {}", serviceInstance.getInstanceId());
            return true;
        } catch (Exception e) {
            log.error("服务实例注册失败", e);
            return false;
        }
    }

    /**
     * 注销服务实例
     */
    public boolean deregisterInstance(ServiceInstance serviceInstance) {
        try {
            namingService.deregisterInstance(
                    serviceInstance.getServiceName(),
                    serviceInstance.getGroupName(),
                    serviceInstance.getIp(),
                    serviceInstance.getPort()
            );
            log.info("服务实例注销成功: {}", serviceInstance.getInstanceId());
            return true;
        } catch (Exception e) {
            log.error("服务实例注销失败", e);
            return false;
        }
    }

    /**
     * 上线实例
     */
    public boolean enableInstance(String serviceName, String groupName, String ip, int port) {
        try {
            // 获取所有实例，找到匹配的实例
            List<Instance> instances = namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream()
                    .filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port)
                    .findFirst()
                    .orElse(null);

            if (targetInstance == null) {
                log.warn("未找到目标实例: {}:{}", ip, port);
                return false;
            }

            targetInstance.setEnabled(true);
            namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("实例上线成功: {}:{}", ip, port);
            return true;
        } catch (Exception e) {
            log.error("实例上线失败: {}:{}", ip, port, e);
            return false;
        }
    }

    /**
     * 下线实例
     */
    public boolean disableInstance(String serviceName, String groupName, String ip, int port) {
        try {
            // 获取所有实例，找到匹配的实例
            List<Instance> instances = namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream()
                    .filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port)
                    .findFirst()
                    .orElse(null);

            if (targetInstance == null) {
                log.warn("未找到目标实例: {}:{}", ip, port);
                return false;
            }

            targetInstance.setEnabled(false);
            namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("实例下线成功: {}:{}", ip, port);
            return true;
        } catch (Exception e) {
            log.error("实例下线失败: {}:{}", ip, port, e);
            return false;
        }
    }

    /**
     * 更新实例权重
     */
    public boolean updateInstanceWeight(String serviceName, String groupName, String ip, int port, double weight) {
        try {
            // 获取所有实例，找到匹配的实例
            List<Instance> instances = namingService.getAllInstances(serviceName, groupName);
            Instance targetInstance = instances.stream()
                    .filter(inst -> inst.getIp().equals(ip) && inst.getPort() == port)
                    .findFirst()
                    .orElse(null);

            if (targetInstance == null) {
                log.warn("未找到目标实例: {}:{}", ip, port);
                return false;
            }

            targetInstance.setWeight(weight);
            namingService.registerInstance(serviceName, groupName, targetInstance);
            log.info("实例权重更新成功: {}:{}, weight={}", ip, port, weight);
            return true;
        } catch (Exception e) {
            log.error("实例权重更新失败: {}:{}", ip, port, e);
            return false;
        }
    }

    /**
     * 订阅服务变化
     */
    public void subscribe(String serviceName, String groupName, com.alibaba.nacos.api.naming.listener.EventListener listener) {
        try {
            namingService.subscribe(serviceName, groupName, listener);
            log.info("订阅服务成功: serviceName={}, groupName={}", serviceName, groupName);
        } catch (Exception e) {
            log.error("订阅服务失败", e);
        }
    }

    /**
     * 取消订阅服务
     */
    public void unsubscribe(String serviceName, String groupName, com.alibaba.nacos.api.naming.listener.EventListener listener) {
        try {
            namingService.unsubscribe(serviceName, groupName, listener);
            log.info("取消订阅服务: serviceName={}, groupName={}", serviceName, groupName);
        } catch (Exception e) {
            log.error("取消订阅服务失败", e);
        }
    }

    /**
     * 将Nacos Instance转换为ServiceInstance
     */
    private ServiceInstance convertToServiceInstance(Instance instance) {
        return ServiceInstance.builder()
                .serviceName(instance.getServiceName())
                .clusterName(instance.getClusterName())
                .ip(instance.getIp())
                .port(instance.getPort())
                .weight(instance.getWeight())
                .healthy(instance.isHealthy())
                .enabled(instance.isEnabled())
                .ephemeral(instance.isEphemeral())
                .metadata(instance.getMetadata())
                .instanceId(instance.getInstanceId())
                .build();
    }

    /**
     * 将ServiceInstance转换为Nacos Instance
     */
    private Instance convertToNacosInstance(ServiceInstance serviceInstance) {
        Instance instance = new Instance();
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setClusterName(serviceInstance.getClusterName());
        instance.setIp(serviceInstance.getIp());
        instance.setPort(serviceInstance.getPort());
        instance.setWeight(serviceInstance.getWeight());
        instance.setHealthy(serviceInstance.getHealthy());
        instance.setEnabled(serviceInstance.getEnabled());
        instance.setEphemeral(serviceInstance.getEphemeral());
        instance.setMetadata(serviceInstance.getMetadata());
        return instance;
    }
}

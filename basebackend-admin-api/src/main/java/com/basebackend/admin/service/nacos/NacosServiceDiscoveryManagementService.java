package com.basebackend.admin.service.nacos;

import com.basebackend.admin.dto.nacos.ServiceInstanceDTO;
import com.basebackend.nacos.model.ServiceInstance;
import com.basebackend.nacos.service.ServiceDiscoveryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Nacos服务发现管理Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NacosServiceDiscoveryManagementService {

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    /**
     * 获取所有服务列表
     */
    public List<String> getAllServices(int pageNo, int pageSize) {
        return serviceDiscoveryManager.getAllServices(pageNo, pageSize);
    }

    /**
     * 获取服务的所有实例
     */
    public List<ServiceInstanceDTO> getServiceInstances(String serviceName, String groupName) {
        List<ServiceInstance> instances = serviceDiscoveryManager.getServiceInstances(serviceName, groupName);
        return instances.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取健康实例
     */
    public List<ServiceInstanceDTO> getHealthyInstances(String serviceName, String groupName) {
        List<ServiceInstance> instances = serviceDiscoveryManager.getHealthyInstances(serviceName, groupName);
        return instances.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 上线实例
     */
    public boolean enableInstance(String serviceName, String groupName, String ip, int port) {
        return serviceDiscoveryManager.enableInstance(serviceName, groupName, ip, port);
    }

    /**
     * 下线实例
     */
    public boolean disableInstance(String serviceName, String groupName, String ip, int port) {
        return serviceDiscoveryManager.disableInstance(serviceName, groupName, ip, port);
    }

    /**
     * 更新实例权重
     */
    public boolean updateInstanceWeight(String serviceName, String groupName, String ip, int port, double weight) {
        return serviceDiscoveryManager.updateInstanceWeight(serviceName, groupName, ip, port, weight);
    }

    /**
     * 转换为DTO
     */
    private ServiceInstanceDTO convertToDTO(ServiceInstance instance) {
        ServiceInstanceDTO dto = new ServiceInstanceDTO();
        dto.setServiceName(instance.getServiceName());
        dto.setGroupName(instance.getGroupName());
        dto.setClusterName(instance.getClusterName());
        dto.setIp(instance.getIp());
        dto.setPort(instance.getPort());
        dto.setWeight(instance.getWeight());
        dto.setHealthy(instance.getHealthy());
        dto.setEnabled(instance.getEnabled());
        dto.setInstanceId(instance.getInstanceId());
        return dto;
    }
}

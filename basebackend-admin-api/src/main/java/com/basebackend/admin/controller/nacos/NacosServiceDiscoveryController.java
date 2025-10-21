package com.basebackend.admin.controller.nacos;

import com.basebackend.admin.dto.nacos.ServiceInstanceDTO;
import com.basebackend.admin.service.nacos.NacosServiceDiscoveryManagementService;
import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nacos服务发现Controller
 */
@RestController
@RequestMapping("/api/nacos/service")
@RequiredArgsConstructor
public class NacosServiceDiscoveryController {

    private final NacosServiceDiscoveryManagementService serviceDiscoveryManagementService;

    /**
     * 获取所有服务列表
     */
    @GetMapping("/list")
    public Result<List<String>> getAllServices(@RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "100") int pageSize) {
        return Result.success(serviceDiscoveryManagementService.getAllServices(pageNo, pageSize));
    }

    /**
     * 获取服务的所有实例
     */
    @GetMapping("/{serviceName}/instances")
    public Result<List<ServiceInstanceDTO>> getServiceInstances(@PathVariable String serviceName,
                                                                  @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName) {
        return Result.success(serviceDiscoveryManagementService.getServiceInstances(serviceName, groupName));
    }

    /**
     * 获取健康实例
     */
    @GetMapping("/{serviceName}/healthy-instances")
    public Result<List<ServiceInstanceDTO>> getHealthyInstances(@PathVariable String serviceName,
                                                                  @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName) {
        return Result.success(serviceDiscoveryManagementService.getHealthyInstances(serviceName, groupName));
    }

    /**
     * 上线实例
     */
    @PostMapping("/instance/enable")
    public Result<Boolean> enableInstance(@RequestParam String serviceName,
                                           @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName,
                                           @RequestParam String ip,
                                           @RequestParam int port) {
        return Result.success(serviceDiscoveryManagementService.enableInstance(serviceName, groupName, ip, port));
    }

    /**
     * 下线实例
     */
    @PostMapping("/instance/disable")
    public Result<Boolean> disableInstance(@RequestParam String serviceName,
                                            @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName,
                                            @RequestParam String ip,
                                            @RequestParam int port) {
        return Result.success(serviceDiscoveryManagementService.disableInstance(serviceName, groupName, ip, port));
    }

    /**
     * 更新实例权重
     */
    @PostMapping("/instance/weight")
    public Result<Boolean> updateWeight(@RequestParam String serviceName,
                                         @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName,
                                         @RequestParam String ip,
                                         @RequestParam int port,
                                         @RequestParam double weight) {
        return Result.success(serviceDiscoveryManagementService.updateInstanceWeight(serviceName, groupName, ip, port, weight));
    }
}

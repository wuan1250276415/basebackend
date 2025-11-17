package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 系统服务Feign客户端
 */
@FeignClient(name = "basebackend-system-api", path = "/api/system")
public interface SystemServiceClient {

    /**
     * 获取部门信息
     */
    @GetMapping("/depts/{id}")
    Result<Object> getDeptById(@PathVariable("id") Long id);

    /**
     * 获取菜单树
     */
    @GetMapping("/menus/tree")
    Result<Object> getMenuTree();

    /**
     * 根据用户ID获取菜单
     */
    @GetMapping("/menus/user/{userId}")
    Result<Object> getMenuTreeByUserId(@PathVariable("userId") Long userId);

    /**
     * 获取字典数据
     */
    @GetMapping("/dicts/data/type/{dictType}")
    Result<Object> getDictDataByType(@PathVariable("dictType") String dictType);
}

package com.basebackend.service.client;

import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 系统服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/system")
public interface SystemServiceClient {

    @GetExchange("/depts/{id}")
    @Operation(summary = "根据ID获取部门")
    Result<Object> getDeptById(@PathVariable("id") Long id);

    @GetExchange("/menus/tree")
    @Operation(summary = "获取菜单树")
    Result<Object> getMenuTree();

    @GetExchange("/menus/user/{userId}")
    @Operation(summary = "根据用户ID获取菜单树")
    Result<Object> getMenuTreeByUserId(@PathVariable("userId") Long userId);

    @GetExchange("/dicts/data/type/{dictType}")
    @Operation(summary = "根据字典类型获取字典数据")
    Result<Object> getDictDataByType(@PathVariable("dictType") String dictType);
}

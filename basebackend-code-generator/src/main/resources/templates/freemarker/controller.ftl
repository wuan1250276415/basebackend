package ${packageName}.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${packageName}.entity.${className};
import ${packageName}.service.${className}Service;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ${tableComment}控制器
 * 
 * @author ${author}
 * @date ${date}
 */
@Slf4j
@RestController
@RequestMapping("/api/${moduleName}/${urlPath}")
@RequiredArgsConstructor
@Tag(name = "${tableComment}管理")
public class ${className}Controller {

    private final ${className}Service ${variableName}Service;

    @GetMapping
    @Operation(summary = "分页查询${tableComment}")
    public Result<Page<${className}>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(${variableName}Service.page(current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询${tableComment}")
    public Result<${className}> getById(@PathVariable Long id) {
        return Result.success(${variableName}Service.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建${tableComment}")
    public Result<String> create(@RequestBody ${className} entity) {
        ${variableName}Service.create(entity);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新${tableComment}")
    public Result<String> update(@PathVariable Long id, @RequestBody ${className} entity) {
        entity.setId(id);
        ${variableName}Service.update(entity);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除${tableComment}")
    public Result<String> delete(@PathVariable Long id) {
        ${variableName}Service.delete(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除${tableComment}")
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        ${variableName}Service.deleteBatch(ids);
        return Result.success("删除成功");
    }
}

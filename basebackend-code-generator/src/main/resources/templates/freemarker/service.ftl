package ${packageName}.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${packageName}.entity.${className};

import java.util.List;

/**
 * ${tableComment}服务接口
 * 
 * @author ${author}
 * @date ${date}
 */
public interface ${className}Service {

    /**
     * 分页查询
     */
    Page<${className}> page(int current, int size);

    /**
     * 根据ID查询
     */
    ${className} getById(Long id);

    /**
     * 创建
     */
    void create(${className} entity);

    /**
     * 更新
     */
    void update(${className} entity);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 批量删除
     */
    void deleteBatch(List<Long> ids);
}

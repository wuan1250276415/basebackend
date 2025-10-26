package ${packageName}.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${packageName}.entity.${className};
import ${packageName}.mapper.${className}Mapper;
import ${packageName}.service.${className}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ${tableComment}服务实现
 * 
 * @author ${author}
 * @date ${date}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${className}ServiceImpl implements ${className}Service {

    private final ${className}Mapper ${variableName}Mapper;

    @Override
    public Page<${className}> page(int current, int size) {
        Page<${className}> page = new Page<>(current, size);
        return ${variableName}Mapper.selectPage(page, null);
    }

    @Override
    public ${className} getById(Long id) {
        return ${variableName}Mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(${className} entity) {
        ${variableName}Mapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(${className} entity) {
        ${variableName}Mapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ${variableName}Mapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ${variableName}Mapper.deleteBatchIds(ids);
    }
}

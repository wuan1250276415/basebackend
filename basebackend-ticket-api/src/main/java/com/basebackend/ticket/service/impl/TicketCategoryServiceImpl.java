package com.basebackend.ticket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.dto.TicketCategoryDTO;
import com.basebackend.ticket.dto.TicketCategoryTreeVO;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.service.TicketCategoryService;
import com.basebackend.ticket.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单分类服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryMapper categoryMapper;
    private final AuditHelper auditHelper;

    @Override
    @Cacheable(value = "ticket:category", key = "'tree'")
    public List<TicketCategoryTreeVO> tree() {
        LambdaQueryWrapper<TicketCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketCategory::getStatus, 1)
                .orderByAsc(TicketCategory::getSortOrder)
                .orderByAsc(TicketCategory::getId);

        List<TicketCategory> categories = categoryMapper.selectList(wrapper);
        List<TicketCategoryTreeVO> voList = categories.stream()
                .map(this::convertToTreeVO)
                .toList();

        return buildTree(voList);
    }

    @Override
    public TicketCategory getById(Long id) {
        TicketCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("工单分类不存在: " + id);
        }
        return category;
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket:category", allEntries = true)
    public void create(TicketCategoryDTO dto) {
        log.info("创建工单分类: name={}", dto.name());

        TicketCategory category = new TicketCategory();
        BeanUtil.copyProperties(dto, category);

        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getSlaHours() == null) {
            category.setSlaHours(24);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        auditHelper.setCreateAuditFields(category);
        categoryMapper.insert(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket:category", allEntries = true)
    public void update(Long id, TicketCategoryDTO dto) {
        TicketCategory category = getById(id);
        log.info("更新工单分类: id={}, name={}", id, dto.name());

        if (dto.name() != null) {
            category.setName(dto.name());
        }
        if (dto.parentId() != null) {
            category.setParentId(dto.parentId());
        }
        if (dto.icon() != null) {
            category.setIcon(dto.icon());
        }
        if (dto.sortOrder() != null) {
            category.setSortOrder(dto.sortOrder());
        }
        if (dto.description() != null) {
            category.setDescription(dto.description());
        }
        if (dto.slaHours() != null) {
            category.setSlaHours(dto.slaHours());
        }
        if (dto.status() != null) {
            category.setStatus(dto.status());
        }

        auditHelper.setUpdateAuditFields(category);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket:category", allEntries = true)
    public void delete(Long id) {
        // 检查是否有子分类
        LambdaQueryWrapper<TicketCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketCategory::getParentId, id);
        long childCount = categoryMapper.selectCount(wrapper);
        if (childCount > 0) {
            throw new RuntimeException("存在子分类，无法删除");
        }

        log.info("删除工单分类: id={}", id);
        categoryMapper.deleteById(id);
    }

    private TicketCategoryTreeVO convertToTreeVO(TicketCategory category) {
        TicketCategoryTreeVO vo = new TicketCategoryTreeVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setIcon(category.getIcon());
        vo.setSortOrder(category.getSortOrder());
        vo.setDescription(category.getDescription());
        vo.setSlaHours(category.getSlaHours());
        vo.setStatus(category.getStatus());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private List<TicketCategoryTreeVO> buildTree(List<TicketCategoryTreeVO> voList) {
        Map<Long, List<TicketCategoryTreeVO>> grouped = voList.stream()
                .collect(Collectors.groupingBy(TicketCategoryTreeVO::getParentId));

        List<TicketCategoryTreeVO> roots = grouped.getOrDefault(0L, new ArrayList<>());
        fillChildren(roots, grouped);
        return roots;
    }

    private void fillChildren(List<TicketCategoryTreeVO> parents,
                              Map<Long, List<TicketCategoryTreeVO>> grouped) {
        for (TicketCategoryTreeVO parent : parents) {
            List<TicketCategoryTreeVO> children = grouped.getOrDefault(parent.getId(), new ArrayList<>());
            parent.setChildren(children);
            if (!children.isEmpty()) {
                fillChildren(children, grouped);
            }
        }
    }
}

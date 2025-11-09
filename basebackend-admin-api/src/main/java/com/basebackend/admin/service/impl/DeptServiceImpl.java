package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.basebackend.admin.dto.DeptDTO;
import com.basebackend.admin.entity.SysDept;
import com.basebackend.admin.mapper.SysDeptMapper;
import com.basebackend.admin.service.DeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<DeptDTO> getDeptTree() {
        log.info("获取部门树");
        List<SysDept> depts = deptMapper.selectDeptTreeList();
        return buildDeptTree(depts, 0L);
    }

    @Override
    public List<DeptDTO> getDeptList() {
        log.info("获取部门列表");
        List<SysDept> depts = deptMapper.selectList(null);
        return depts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DeptDTO getById(Long id) {
        log.info("根据ID查询部门: {}", id);
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        return convertToDTO(dept);
    }

    @Override
    @Transactional
    public void create(DeptDTO deptDTO) {
        log.info("创建部门: {}", deptDTO.getDeptName());

        // 检查部门名称是否唯一
        if (!checkDeptNameUnique(deptDTO.getDeptName(), deptDTO.getParentId(), null)) {
            throw new RuntimeException("部门名称已存在");
        }

        // 创建部门
        SysDept dept = new SysDept();
        BeanUtil.copyProperties(deptDTO, dept);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        dept.setCreateBy(1L); // 临时硬编码
        dept.setUpdateBy(1L); // 临时硬编码

        deptMapper.insert(dept);

        log.info("部门创建成功: {}", dept.getDeptName());
    }

    @Override
    @Transactional
    public void update(DeptDTO deptDTO) {
        log.info("更新部门: {}", deptDTO.getId());

        SysDept dept = deptMapper.selectById(deptDTO.getId());
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }

        // 检查部门名称是否唯一
        if (!checkDeptNameUnique(deptDTO.getDeptName(), deptDTO.getParentId(), deptDTO.getId())) {
            throw new RuntimeException("部门名称已存在");
        }

        // 更新部门信息
        BeanUtil.copyProperties(deptDTO, dept);
        dept.setUpdateTime(LocalDateTime.now());
        dept.setUpdateBy(1L); // 临时硬编码

        deptMapper.updateById(dept);

        log.info("部门更新成功: {}", dept.getDeptName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("删除部门: {}", id);

        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }

        // 检查是否有子部门
        if (hasChildren(id)) {
            throw new RuntimeException("存在子部门，不允许删除");
        }

        // 逻辑删除
        deptMapper.deleteById(id);

        log.info("部门删除成功: {}", dept.getDeptName());
    }

    @Override
    public List<DeptDTO> getChildrenByDeptId(Long deptId) {
        log.info("根据部门ID获取子部门列表: {}", deptId);
        List<SysDept> children = deptMapper.selectChildrenByDeptId(deptId);
        return children.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getChildrenDeptIds(Long deptId) {
        log.info("根据部门ID获取所有子部门ID列表: {}", deptId);
        return deptMapper.selectChildrenDeptIds(deptId);
    }

    @Override
    public boolean checkDeptNameUnique(String deptName, Long parentId, Long deptId) {
        return deptMapper.checkDeptNameUnique(deptName, parentId, deptId) == 0;
    }

    @Override
    public boolean hasChildren(Long deptId) {
        return deptMapper.selectCountByParentId(deptId) > 0;
    }

    @Override
    public DeptDTO getByDeptName(String deptName) {
        log.info("根据部门名称查询: {}", deptName);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptName, deptName);
        SysDept dept = deptMapper.selectOne(wrapper);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        return convertToDTO(dept);
    }

    @Override
    public DeptDTO getByDeptCode(String deptCode) {
        log.warn("getByDeptCode 方法暂未实现：数据库表 sys_dept 中没有 dept_code 字段");
        throw new UnsupportedOperationException(
            "根据部门编码查询功能暂未实现：数据库表中没有 dept_code 字段。请使用 getByDeptName 方法代替。"
        );
    }

    @Override
    public List<DeptDTO> getBatchByIds(List<Long> ids) {
        log.info("批量查询部门: {}", ids);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<SysDept> depts = deptMapper.selectBatchIds(ids);
        return depts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeptDTO> getByParentId(Long parentId) {
        log.info("根据父部门ID查询: {}", parentId);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getParentId, parentId);
        List<SysDept> depts = deptMapper.selectList(wrapper);
        return depts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 构建部门树
     */
    private List<DeptDTO> buildDeptTree(List<SysDept> depts, Long parentId) {
        List<DeptDTO> tree = new ArrayList<>();
        
        for (SysDept dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                DeptDTO dto = convertToDTO(dept);
                List<DeptDTO> children = buildDeptTree(depts, dept.getId());
                if (!children.isEmpty()) {
                    dto.setChildren(children);
                }
                tree.add(dto);
            }
        }
        
        return tree;
    }

    /**
     * 转换为DTO
     */
    private DeptDTO convertToDTO(SysDept dept) {
        DeptDTO dto = new DeptDTO();
        BeanUtil.copyProperties(dept, dto);
        return dto;
    }
}

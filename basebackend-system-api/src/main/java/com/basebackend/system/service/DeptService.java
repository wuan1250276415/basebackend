package com.basebackend.system.service;

import com.basebackend.system.dto.DeptDTO;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DeptService {

    /**
     * 获取部门树
     */
    List<DeptDTO> getDeptTree();

    /**
     * 获取部门列表
     */
    List<DeptDTO> getDeptList();

    /**
     * 根据ID查询部门
     */
    DeptDTO getById(Long id);

    /**
     * 创建部门
     */
    void create(DeptDTO deptDTO);

    /**
     * 更新部门
     */
    void update(DeptDTO deptDTO);

    /**
     * 删除部门
     */
    void delete(Long id);

    /**
     * 根据部门ID获取子部门列表
     */
    List<DeptDTO> getChildrenByDeptId(Long deptId);

    /**
     * 根据部门ID获取所有子部门ID列表
     */
    List<Long> getChildrenDeptIds(Long deptId);

    /**
     * 检查部门名称是否唯一
     */
    boolean checkDeptNameUnique(String deptName, Long parentId, Long deptId);

    /**
     * 检查部门是否有子部门
     */
    boolean hasChildren(Long deptId);

    /**
     * 根据部门名称查询部门
     */
    DeptDTO getByDeptName(String deptName);

    /**
     * 根据部门编码查询部门
     */
    DeptDTO getByDeptCode(String deptCode);

    /**
     * 批量查询部门
     */
    List<DeptDTO> getBatchByIds(List<Long> ids);

    /**
     * 根据父部门ID查询直接子部门
     */
    List<DeptDTO> getByParentId(Long parentId);
}

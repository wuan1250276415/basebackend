package com.basebackend.dept.service;

import com.basebackend.dept.dto.DeptDTO;

import java.util.List;

/**
 * 部门服务接口
 *
 * @author BaseBackend Team
 */
public interface DeptService {

    /**
     * 获取部门树
     *
     * @return 部门树列表
     */
    List<DeptDTO> getDeptTree();

    /**
     * 获取部门列表
     *
     * @return 部门列表
     */
    List<DeptDTO> getDeptList();

    /**
     * 根据ID查询部门
     *
     * @param id 部门ID
     * @return 部门DTO
     */
    DeptDTO getById(Long id);

    /**
     * 创建部门
     *
     * @param deptDTO 部门DTO
     */
    void create(DeptDTO deptDTO);

    /**
     * 更新部门
     *
     * @param deptDTO 部门DTO
     */
    void update(DeptDTO deptDTO);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void delete(Long id);

    /**
     * 根据部门ID获取子部门列表
     *
     * @param deptId 部门ID
     * @return 子部门列表
     */
    List<DeptDTO> getChildrenByDeptId(Long deptId);

    /**
     * 根据部门ID获取所有子部门ID列表
     *
     * @param deptId 部门ID
     * @return 子部门ID列表
     */
    List<Long> getChildrenDeptIds(Long deptId);

    /**
     * 检查部门名称是否唯一
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @param deptId   部门ID
     * @return 是否唯一
     */
    boolean checkDeptNameUnique(String deptName, Long parentId, Long deptId);

    /**
     * 检查部门是否有子部门
     *
     * @param deptId 部门ID
     * @return 是否有子部门
     */
    boolean hasChildren(Long deptId);

    /**
     * 根据部门名称查询部门
     *
     * @param deptName 部门名称
     * @return 部门DTO
     */
    DeptDTO getByDeptName(String deptName);

    /**
     * 根据部门编码查询部门
     *
     * @param deptCode 部门编码
     * @return 部门DTO
     */
    DeptDTO getByDeptCode(String deptCode);

    /**
     * 批量查询部门
     *
     * @param ids 部门ID列表
     * @return 部门列表
     */
    List<DeptDTO> getBatchByIds(List<Long> ids);

    /**
     * 根据父部门ID查询直接子部门
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    List<DeptDTO> getByParentId(Long parentId);
}

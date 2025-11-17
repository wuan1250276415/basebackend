package com.basebackend.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.dept.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统部门Mapper接口
 *
 * @author BaseBackend Team
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 根据父部门ID查询子部门数量
     *
     * @param parentId 父部门ID
     * @return 子部门数量
     */
    int selectCountByParentId(@Param("parentId") Long parentId);

    /**
     * 查询部门树列表
     *
     * @return 部门列表
     */
    List<SysDept> selectDeptTreeList();

    /**
     * 根据部门ID查询子部门列表
     *
     * @param deptId 部门ID
     * @return 子部门列表
     */
    List<SysDept> selectChildrenByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据部门ID查询所有子部门ID列表
     *
     * @param deptId 部门ID
     * @return 子部门ID列表
     */
    List<Long> selectChildrenDeptIds(@Param("deptId") Long deptId);

    /**
     * 检查部门名称是否唯一
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @param deptId   部门ID（更新时需要排除自己）
     * @return 存在的数量
     */
    int checkDeptNameUnique(@Param("deptName") String deptName, @Param("parentId") Long parentId, @Param("deptId") Long deptId);

    /**
     * 根据用户ID查询部门信息
     *
     * @param userId 用户ID
     * @return 部门信息
     */
    SysDept selectDeptByUserId(@Param("userId") Long userId);
}

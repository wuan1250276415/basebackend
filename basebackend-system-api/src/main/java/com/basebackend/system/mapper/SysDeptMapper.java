package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门Mapper接口
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 根据父部门ID查询子部门数量
     */
    int selectCountByParentId(@Param("parentId") Long parentId);

    /**
     * 查询部门树列表
     */
    List<SysDept> selectDeptTreeList();

    /**
     * 根据部门ID查询子部门列表
     */
    List<SysDept> selectChildrenByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据部门ID查询所有子部门ID列表
     */
    List<Long> selectChildrenDeptIds(@Param("deptId") Long deptId);

    /**
     * 检查部门名称是否唯一
     */
    int checkDeptNameUnique(@Param("deptName") String deptName, @Param("parentId") Long parentId, @Param("deptId") Long deptId);

    /**
     * 根据用户ID查询部门信息
     */
    SysDept selectDeptByUserId(@Param("userId") Long userId);
}

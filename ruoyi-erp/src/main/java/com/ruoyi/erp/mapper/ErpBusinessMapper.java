package com.ruoyi.erp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 业务对象可见性辅助查询
 *
 * @author ruoyi
 */
public interface ErpBusinessMapper
{
    /**
     * 查询部门自身及下级部门ID
     *
     * @param deptId 部门ID
     * @return 部门ID集合
     */
    List<Long> selectDeptScope(@Param("deptId") Long deptId);
}

package com.ruoyi.erp.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 单号序列查询
 *
 * @author ruoyi
 */
public interface ErpSequenceMapper
{
    /**
     * 查询指定表中指定列以某前缀开头的最大单号
     *
     * @param table 表名（内部常量，不可来自外部输入）
     * @param column 列名（内部常量）
     * @param prefix 单号前缀
     * @return 最大单号
     */
    String selectMaxNo(@Param("table") String table, @Param("column") String column, @Param("prefix") String prefix);
}

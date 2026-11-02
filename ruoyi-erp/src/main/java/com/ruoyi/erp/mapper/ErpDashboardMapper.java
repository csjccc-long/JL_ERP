package com.ruoyi.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 采购驾驶舱统计
 *
 * @author ruoyi
 */
public interface ErpDashboardMapper
{
    /** 关键指标 */
    Map<String, Object> selectOverview(@Param("threshold") int threshold);

    /** 待办审批 */
    List<Map<String, Object>> selectPendingTasks();

    /** 低库存预警 */
    List<Map<String, Object>> selectLowStockList(@Param("threshold") int threshold);

    /** 近 N 天入库趋势 */
    List<Map<String, Object>> selectReceiptTrend(@Param("days") int days);

    /** 订单状态分布 */
    List<Map<String, Object>> selectOrderStatusStat();
}

package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;

/**
 * 采购驾驶舱统计服务
 *
 * @author ruoyi
 */
public interface IErpDashboardService
{
    /** 关键指标卡片 */
    Map<String, Object> overview();

    /** 待办审批列表 */
    List<Map<String, Object>> pendingTasks();

    /** 低库存预警 */
    List<Map<String, Object>> lowStockList();

    /** 近 N 天入库趋势 */
    List<Map<String, Object>> receiptTrend(int days);

    /** 订单状态分布 */
    List<Map<String, Object>> orderStatusStat();
}

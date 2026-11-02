package com.ruoyi.erp.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.mapper.ErpDashboardMapper;
import com.ruoyi.erp.service.IErpDashboardService;

/**
 * 采购驾驶舱统计服务实现
 *
 * @author ruoyi
 */
@Service
public class ErpDashboardServiceImpl implements IErpDashboardService
{
    @Autowired
    private ErpDashboardMapper dashboardMapper;

    @Value("${erp.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Override
    public Map<String, Object> overview()
    {
        return dashboardMapper.selectOverview(lowStockThreshold);
    }

    @Override
    public List<Map<String, Object>> pendingTasks()
    {
        return dashboardMapper.selectPendingTasks();
    }

    @Override
    public List<Map<String, Object>> lowStockList()
    {
        return dashboardMapper.selectLowStockList(lowStockThreshold);
    }

    @Override
    public List<Map<String, Object>> receiptTrend(int days)
    {
        int span = days <= 0 ? 7 : Math.min(days, 90);
        return dashboardMapper.selectReceiptTrend(span);
    }

    @Override
    public List<Map<String, Object>> orderStatusStat()
    {
        return dashboardMapper.selectOrderStatusStat();
    }
}

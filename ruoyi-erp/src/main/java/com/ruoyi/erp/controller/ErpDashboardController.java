package com.ruoyi.erp.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.service.IErpDashboardService;

/**
 * 采购驾驶舱
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/dashboard")
public class ErpDashboardController extends BaseController
{
    @Autowired
    private IErpDashboardService dashboardService;

    @PreAuthorize("@ss.hasPermi('erp:dashboard:view')")
    @GetMapping({ "/overview", "/summary" })
    public AjaxResult overview()
    {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("cards", dashboardService.overview());
        data.put("orderStatus", dashboardService.orderStatusStat());
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('erp:dashboard:view')")
    @GetMapping("/pending")
    public AjaxResult pending()
    {
        return success(dashboardService.pendingTasks());
    }

    @PreAuthorize("@ss.hasPermi('erp:dashboard:view')")
    @GetMapping("/low-stock")
    public AjaxResult lowStock()
    {
        return success(dashboardService.lowStockList());
    }

    @PreAuthorize("@ss.hasPermi('erp:dashboard:view')")
    @GetMapping("/receipt-trend")
    public AjaxResult receiptTrend(@RequestParam(defaultValue = "7") int days)
    {
        return success(dashboardService.receiptTrend(days));
    }
}

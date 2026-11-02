package com.ruoyi.erp.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.domain.ErpInventoryBalance;
import com.ruoyi.erp.service.IErpInventoryBalanceService;

/**
 * 库存余额Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/inventory")
public class ErpInventoryBalanceController extends BaseController
{
    @Autowired
    private IErpInventoryBalanceService erpInventoryBalanceService;

    @PreAuthorize("@ss.hasPermi('erp:inventory:list')")
    @GetMapping({ "/list", "/balance/list" })
    public TableDataInfo list(ErpInventoryBalance erpInventoryBalance)
    {
        startPage();
        List<ErpInventoryBalance> list = erpInventoryBalanceService.selectErpInventoryBalanceList(erpInventoryBalance);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:export')")
    @Log(title = "库存余额", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpInventoryBalance erpInventoryBalance)
    {
        List<ErpInventoryBalance> list = erpInventoryBalanceService.selectErpInventoryBalanceList(erpInventoryBalance);
        ExcelUtil<ErpInventoryBalance> util = new ExcelUtil<ErpInventoryBalance>(ErpInventoryBalance.class);
        util.exportExcel(response, list, "库存余额数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpInventoryBalanceService.selectErpInventoryBalanceById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:add')")
    @Log(title = "库存余额", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpInventoryBalance erpInventoryBalance)
    {
        erpInventoryBalance.setCreateBy(getUsername());
        return toAjax(erpInventoryBalanceService.insertErpInventoryBalance(erpInventoryBalance));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:edit')")
    @Log(title = "库存余额", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpInventoryBalance erpInventoryBalance)
    {
        erpInventoryBalance.setUpdateBy(getUsername());
        return toAjax(erpInventoryBalanceService.updateErpInventoryBalance(erpInventoryBalance));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:remove')")
    @Log(title = "库存余额", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpInventoryBalanceService.deleteErpInventoryBalanceByIds(ids));
    }
}
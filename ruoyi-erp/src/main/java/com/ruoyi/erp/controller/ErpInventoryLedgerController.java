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
import com.ruoyi.erp.domain.ErpInventoryLedger;
import com.ruoyi.erp.service.IErpInventoryLedgerService;

/**
 * 库存流水Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/inventory/ledger")
public class ErpInventoryLedgerController extends BaseController
{
    @Autowired
    private IErpInventoryLedgerService erpInventoryLedgerService;

    @PreAuthorize("@ss.hasPermi('erp:inventory:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpInventoryLedger erpInventoryLedger)
    {
        startPage();
        List<ErpInventoryLedger> list = erpInventoryLedgerService.selectErpInventoryLedgerList(erpInventoryLedger);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:export')")
    @Log(title = "库存流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpInventoryLedger erpInventoryLedger)
    {
        List<ErpInventoryLedger> list = erpInventoryLedgerService.selectErpInventoryLedgerList(erpInventoryLedger);
        ExcelUtil<ErpInventoryLedger> util = new ExcelUtil<ErpInventoryLedger>(ErpInventoryLedger.class);
        util.exportExcel(response, list, "库存流水数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpInventoryLedgerService.selectErpInventoryLedgerById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:add')")
    @Log(title = "库存流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpInventoryLedger erpInventoryLedger)
    {
        erpInventoryLedger.setCreateBy(getUsername());
        return toAjax(erpInventoryLedgerService.insertErpInventoryLedger(erpInventoryLedger));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:edit')")
    @Log(title = "库存流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpInventoryLedger erpInventoryLedger)
    {
        erpInventoryLedger.setUpdateBy(getUsername());
        return toAjax(erpInventoryLedgerService.updateErpInventoryLedger(erpInventoryLedger));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:remove')")
    @Log(title = "库存流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpInventoryLedgerService.deleteErpInventoryLedgerByIds(ids));
    }
}
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.domain.ErpSupplier;
import com.ruoyi.erp.service.IErpSupplierService;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * 供应商Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/supplier")
public class ErpSupplierController extends BaseController
{
    @Autowired
    private IErpSupplierService erpSupplierService;

    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:supplier:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpSupplier erpSupplier)
    {
        startPage();
        List<ErpSupplier> list = erpSupplierService.selectErpSupplierList(erpSupplier);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:export')")
    @Log(title = "供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpSupplier erpSupplier)
    {
        List<ErpSupplier> list = erpSupplierService.selectErpSupplierList(erpSupplier);
        ExcelUtil<ErpSupplier> util = new ExcelUtil<ErpSupplier>(ErpSupplier.class);
        util.exportExcel(response, list, "供应商数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpSupplierService.selectErpSupplierById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:add')")
    @Log(title = "供应商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpSupplier erpSupplier)
    {
        erpSupplier.setCreateBy(getUsername());
        return toAjax(erpSupplierService.insertErpSupplier(erpSupplier));
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:edit')")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpSupplier erpSupplier)
    {
        erpSupplier.setUpdateBy(getUsername());
        return toAjax(erpSupplierService.updateErpSupplier(erpSupplier));
    }

    /**
     * 提交准入审核
     */
    @PreAuthorize("@ss.hasPermi('erp:supplier:edit')")
    @Log(title = "供应商提交准入", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id)
    {
        workflowService.submitSupplier(id);
        return success();
    }

    /**
     * 准入通过
     */
    @PreAuthorize("@ss.hasPermi('erp:supplier:approve')")
    @Log(title = "供应商准入通过", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String opinion)
    {
        workflowService.auditSupplier(id, com.ruoyi.erp.constant.ErpStatus.SUPPLIER_APPROVED, riskLevel, opinion);
        return success();
    }

    /**
     * 准入驳回
     */
    @PreAuthorize("@ss.hasPermi('erp:supplier:approve')")
    @Log(title = "供应商准入驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.auditSupplier(id, com.ruoyi.erp.constant.ErpStatus.SUPPLIER_REJECTED, null, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:supplier:remove')")
    @Log(title = "供应商", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpSupplierService.deleteErpSupplierByIds(ids));
    }
}
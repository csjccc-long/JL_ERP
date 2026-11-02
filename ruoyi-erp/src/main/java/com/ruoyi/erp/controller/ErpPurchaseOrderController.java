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
import com.ruoyi.erp.domain.ErpPurchaseOrder;
import com.ruoyi.erp.service.IErpPurchaseOrderService;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * 采购订单Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/order")
public class ErpPurchaseOrderController extends BaseController
{
    @Autowired
    private IErpPurchaseOrderService erpPurchaseOrderService;

    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpPurchaseOrder erpPurchaseOrder)
    {
        startPage();
        List<ErpPurchaseOrder> list = erpPurchaseOrderService.selectErpPurchaseOrderList(erpPurchaseOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:order:export')")
    @Log(title = "采购订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpPurchaseOrder erpPurchaseOrder)
    {
        List<ErpPurchaseOrder> list = erpPurchaseOrderService.selectErpPurchaseOrderList(erpPurchaseOrder);
        ExcelUtil<ErpPurchaseOrder> util = new ExcelUtil<ErpPurchaseOrder>(ErpPurchaseOrder.class);
        util.exportExcel(response, list, "采购订单数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:order:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpPurchaseOrderService.selectErpPurchaseOrderById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:add')")
    @Log(title = "采购订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpPurchaseOrder erpPurchaseOrder)
    {
        erpPurchaseOrder.setCreateBy(getUsername());
        return toAjax(erpPurchaseOrderService.insertErpPurchaseOrder(erpPurchaseOrder));
    }

    @PreAuthorize("@ss.hasPermi('erp:order:edit')")
    @Log(title = "采购订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpPurchaseOrder erpPurchaseOrder)
    {
        erpPurchaseOrder.setUpdateBy(getUsername());
        return toAjax(erpPurchaseOrderService.updateErpPurchaseOrder(erpPurchaseOrder));
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('erp:order:submit')")
    @Log(title = "采购订单提交", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id)
    {
        workflowService.submitOrder(id);
        return success();
    }

    /**
     * 审批通过
     */
    @PreAuthorize("@ss.hasPermi('erp:order:approve')")
    @Log(title = "采购订单审批通过", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveOrder(id, true, opinion);
        return success();
    }

    /**
     * 审批驳回
     */
    @PreAuthorize("@ss.hasPermi('erp:order:approve')")
    @Log(title = "采购订单驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveOrder(id, false, opinion);
        return success();
    }

    /**
     * 取消订单
     */
    @PreAuthorize("@ss.hasPermi('erp:order:cancel')")
    @Log(title = "采购订单取消", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.cancelOrder(id, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:order:remove')")
    @Log(title = "采购订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpPurchaseOrderService.deleteErpPurchaseOrderByIds(ids));
    }
}
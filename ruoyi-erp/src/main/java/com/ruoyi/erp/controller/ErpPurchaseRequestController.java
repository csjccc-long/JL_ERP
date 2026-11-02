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
import com.ruoyi.erp.domain.ErpPurchaseRequest;
import com.ruoyi.erp.service.IErpPurchaseRequestService;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * 采购申请Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/request")
public class ErpPurchaseRequestController extends BaseController
{
    @Autowired
    private IErpPurchaseRequestService erpPurchaseRequestService;

    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:request:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpPurchaseRequest erpPurchaseRequest)
    {
        startPage();
        List<ErpPurchaseRequest> list = erpPurchaseRequestService.selectErpPurchaseRequestList(erpPurchaseRequest);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:request:export')")
    @Log(title = "采购申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpPurchaseRequest erpPurchaseRequest)
    {
        List<ErpPurchaseRequest> list = erpPurchaseRequestService.selectErpPurchaseRequestList(erpPurchaseRequest);
        ExcelUtil<ErpPurchaseRequest> util = new ExcelUtil<ErpPurchaseRequest>(ErpPurchaseRequest.class);
        util.exportExcel(response, list, "采购申请数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:request:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpPurchaseRequestService.selectErpPurchaseRequestById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:request:add')")
    @Log(title = "采购申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpPurchaseRequest erpPurchaseRequest)
    {
        erpPurchaseRequest.setCreateBy(getUsername());
        return toAjax(erpPurchaseRequestService.insertErpPurchaseRequest(erpPurchaseRequest));
    }

    @PreAuthorize("@ss.hasPermi('erp:request:edit')")
    @Log(title = "采购申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpPurchaseRequest erpPurchaseRequest)
    {
        erpPurchaseRequest.setUpdateBy(getUsername());
        return toAjax(erpPurchaseRequestService.updateErpPurchaseRequest(erpPurchaseRequest));
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('erp:request:submit')")
    @Log(title = "采购申请提交", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id)
    {
        workflowService.submitRequest(id);
        return success();
    }

    /**
     * 审批通过
     */
    @PreAuthorize("@ss.hasPermi('erp:request:approve')")
    @Log(title = "采购申请审批通过", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveRequest(id, true, opinion);
        return success();
    }

    /**
     * 审批驳回
     */
    @PreAuthorize("@ss.hasPermi('erp:request:approve')")
    @Log(title = "采购申请驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveRequest(id, false, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:request:remove')")
    @Log(title = "采购申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpPurchaseRequestService.deleteErpPurchaseRequestByIds(ids));
    }
}
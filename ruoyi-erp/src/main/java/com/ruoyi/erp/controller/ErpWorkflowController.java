package com.ruoyi.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.domain.ErpApprovalRecord;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * ERP 业务流程控制器（提交、审批、入库确认、冲销、审批轨迹）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/workflow")
public class ErpWorkflowController extends BaseController
{
    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:supplier:approve')")
    @Log(title = "供应商准入审核", businessType = BusinessType.UPDATE)
    @PostMapping("/supplier/audit/{id}")
    public AjaxResult auditSupplier(@PathVariable Long id,
            @RequestParam String admissionStatus,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String opinion)
    {
        workflowService.auditSupplier(id, admissionStatus, riskLevel, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:submit')")
    @Log(title = "合同提交审批", businessType = BusinessType.UPDATE)
    @PostMapping("/contract/submit/{id}")
    public AjaxResult submitContract(@PathVariable Long id)
    {
        workflowService.submitContract(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PostMapping("/contract/approve/{id}")
    public AjaxResult approveContract(@PathVariable Long id,
            @RequestParam boolean pass,
            @RequestParam(required = false) String opinion)
    {
        workflowService.approveContract(id, pass, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:terminate')")
    @Log(title = "合同终止", businessType = BusinessType.UPDATE)
    @PostMapping("/contract/terminate/{id}")
    public AjaxResult terminateContract(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.terminateContract(id, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:request:submit')")
    @Log(title = "采购申请提交", businessType = BusinessType.UPDATE)
    @PostMapping("/request/submit/{id}")
    public AjaxResult submitRequest(@PathVariable Long id)
    {
        workflowService.submitRequest(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:request:approve')")
    @Log(title = "采购申请审批", businessType = BusinessType.UPDATE)
    @PostMapping("/request/approve/{id}")
    public AjaxResult approveRequest(@PathVariable Long id,
            @RequestParam boolean pass,
            @RequestParam(required = false) String opinion)
    {
        workflowService.approveRequest(id, pass, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:order:submit')")
    @Log(title = "采购订单提交", businessType = BusinessType.UPDATE)
    @PostMapping("/order/submit/{id}")
    public AjaxResult submitOrder(@PathVariable Long id)
    {
        workflowService.submitOrder(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:order:approve')")
    @Log(title = "采购订单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/order/approve/{id}")
    public AjaxResult approveOrder(@PathVariable Long id,
            @RequestParam boolean pass,
            @RequestParam(required = false) String opinion)
    {
        workflowService.approveOrder(id, pass, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:order:cancel')")
    @Log(title = "采购订单取消", businessType = BusinessType.UPDATE)
    @PostMapping("/order/cancel/{id}")
    public AjaxResult cancelOrder(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.cancelOrder(id, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:confirm')")
    @Log(title = "到货入库确认", businessType = BusinessType.UPDATE)
    @PostMapping("/receipt/confirm/{id}")
    public AjaxResult confirmReceipt(@PathVariable Long id)
    {
        workflowService.confirmReceipt(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:reverse')")
    @Log(title = "入库冲销", businessType = BusinessType.UPDATE)
    @PostMapping("/receipt/reverse/{id}")
    public AjaxResult reverseReceipt(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.reverseReceipt(id, opinion);
        return success();
    }

    @GetMapping("/approval/list")
    public AjaxResult approvalList(@RequestParam String businessType, @RequestParam Long businessId)
    {
        List<ErpApprovalRecord> list = workflowService.selectApprovalRecords(businessType, businessId);
        return success(list);
    }
}

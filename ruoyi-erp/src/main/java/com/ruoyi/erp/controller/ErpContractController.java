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
import com.ruoyi.erp.domain.ErpContract;
import com.ruoyi.erp.service.IErpContractService;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * 合同Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/contract")
public class ErpContractController extends BaseController
{
    @Autowired
    private IErpContractService erpContractService;

    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:contract:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpContract erpContract)
    {
        startPage();
        List<ErpContract> list = erpContractService.selectErpContractList(erpContract);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:export')")
    @Log(title = "合同", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpContract erpContract)
    {
        List<ErpContract> list = erpContractService.selectErpContractList(erpContract);
        ExcelUtil<ErpContract> util = new ExcelUtil<ErpContract>(ErpContract.class);
        util.exportExcel(response, list, "合同数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpContractService.selectErpContractById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:add')")
    @Log(title = "合同", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpContract erpContract)
    {
        erpContract.setCreateBy(getUsername());
        return toAjax(erpContractService.insertErpContract(erpContract));
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:edit')")
    @Log(title = "合同", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpContract erpContract)
    {
        erpContract.setUpdateBy(getUsername());
        return toAjax(erpContractService.updateErpContract(erpContract));
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('erp:contract:submit')")
    @Log(title = "合同提交审批", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id)
    {
        workflowService.submitContract(id);
        return success();
    }

    /**
     * 审批通过（生效）
     */
    @PreAuthorize("@ss.hasPermi('erp:contract:approve')")
    @Log(title = "合同生效", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/activate")
    public AjaxResult activate(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveContract(id, true, opinion);
        return success();
    }

    /**
     * 审批驳回
     */
    @PreAuthorize("@ss.hasPermi('erp:contract:approve')")
    @Log(title = "合同驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult rejectContract(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.approveContract(id, false, opinion);
        return success();
    }

    /**
     * 终止合同
     */
    @PreAuthorize("@ss.hasPermi('erp:contract:terminate')")
    @Log(title = "合同终止", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/terminate")
    public AjaxResult terminate(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.terminateContract(id, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:contract:remove')")
    @Log(title = "合同", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpContractService.deleteErpContractByIds(ids));
    }
}
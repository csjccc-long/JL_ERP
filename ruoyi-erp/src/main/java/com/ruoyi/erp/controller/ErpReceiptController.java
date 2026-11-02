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
import com.ruoyi.erp.domain.ErpReceipt;
import com.ruoyi.erp.service.IErpReceiptService;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * 到货入库单Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/receipt")
public class ErpReceiptController extends BaseController
{
    @Autowired
    private IErpReceiptService erpReceiptService;

    @Autowired
    private IErpWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('erp:receipt:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpReceipt erpReceipt)
    {
        startPage();
        List<ErpReceipt> list = erpReceiptService.selectErpReceiptList(erpReceipt);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:export')")
    @Log(title = "到货入库单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpReceipt erpReceipt)
    {
        List<ErpReceipt> list = erpReceiptService.selectErpReceiptList(erpReceipt);
        ExcelUtil<ErpReceipt> util = new ExcelUtil<ErpReceipt>(ErpReceipt.class);
        util.exportExcel(response, list, "到货入库单数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpReceiptService.selectErpReceiptById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:add')")
    @Log(title = "到货入库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpReceipt erpReceipt)
    {
        erpReceipt.setCreateBy(getUsername());
        return toAjax(erpReceiptService.insertErpReceipt(erpReceipt));
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:edit')")
    @Log(title = "到货入库单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpReceipt erpReceipt)
    {
        erpReceipt.setUpdateBy(getUsername());
        return toAjax(erpReceiptService.updateErpReceipt(erpReceipt));
    }

    /**
     * 确认入库
     */
    @PreAuthorize("@ss.hasPermi('erp:receipt:confirm')")
    @Log(title = "到货入库确认", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id)
    {
        workflowService.confirmReceipt(id);
        return success();
    }

    /**
     * 冲销入库
     */
    @PreAuthorize("@ss.hasPermi('erp:receipt:reverse')")
    @Log(title = "入库冲销", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reverse")
    public AjaxResult reverse(@PathVariable Long id, @RequestParam(required = false) String opinion)
    {
        workflowService.reverseReceipt(id, opinion);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('erp:receipt:remove')")
    @Log(title = "到货入库单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpReceiptService.deleteErpReceiptByIds(ids));
    }
}
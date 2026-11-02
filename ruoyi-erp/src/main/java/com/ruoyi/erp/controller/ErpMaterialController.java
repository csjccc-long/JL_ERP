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
import com.ruoyi.erp.domain.ErpMaterial;
import com.ruoyi.erp.service.IErpMaterialService;

/**
 * 物料Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/material")
public class ErpMaterialController extends BaseController
{
    @Autowired
    private IErpMaterialService erpMaterialService;

    @PreAuthorize("@ss.hasPermi('erp:material:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpMaterial erpMaterial)
    {
        startPage();
        List<ErpMaterial> list = erpMaterialService.selectErpMaterialList(erpMaterial);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:material:export')")
    @Log(title = "物料", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpMaterial erpMaterial)
    {
        List<ErpMaterial> list = erpMaterialService.selectErpMaterialList(erpMaterial);
        ExcelUtil<ErpMaterial> util = new ExcelUtil<ErpMaterial>(ErpMaterial.class);
        util.exportExcel(response, list, "物料数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:material:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpMaterialService.selectErpMaterialById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:material:add')")
    @Log(title = "物料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpMaterial erpMaterial)
    {
        erpMaterial.setCreateBy(getUsername());
        return toAjax(erpMaterialService.insertErpMaterial(erpMaterial));
    }

    @PreAuthorize("@ss.hasPermi('erp:material:edit')")
    @Log(title = "物料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpMaterial erpMaterial)
    {
        erpMaterial.setUpdateBy(getUsername());
        return toAjax(erpMaterialService.updateErpMaterial(erpMaterial));
    }

    @PreAuthorize("@ss.hasPermi('erp:material:remove')")
    @Log(title = "物料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpMaterialService.deleteErpMaterialByIds(ids));
    }
}
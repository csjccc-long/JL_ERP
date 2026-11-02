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
import com.ruoyi.erp.domain.ErpWarehouse;
import com.ruoyi.erp.service.IErpWarehouseService;

/**
 * 仓库Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/warehouse")
public class ErpWarehouseController extends BaseController
{
    @Autowired
    private IErpWarehouseService erpWarehouseService;

    @PreAuthorize("@ss.hasPermi('erp:warehouse:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpWarehouse erpWarehouse)
    {
        startPage();
        List<ErpWarehouse> list = erpWarehouseService.selectErpWarehouseList(erpWarehouse);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:export')")
    @Log(title = "仓库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ErpWarehouse erpWarehouse)
    {
        List<ErpWarehouse> list = erpWarehouseService.selectErpWarehouseList(erpWarehouse);
        ExcelUtil<ErpWarehouse> util = new ExcelUtil<ErpWarehouse>(ErpWarehouse.class);
        util.exportExcel(response, list, "仓库数据");
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(erpWarehouseService.selectErpWarehouseById(id));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:add')")
    @Log(title = "仓库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ErpWarehouse erpWarehouse)
    {
        erpWarehouse.setCreateBy(getUsername());
        return toAjax(erpWarehouseService.insertErpWarehouse(erpWarehouse));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:edit')")
    @Log(title = "仓库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ErpWarehouse erpWarehouse)
    {
        erpWarehouse.setUpdateBy(getUsername());
        return toAjax(erpWarehouseService.updateErpWarehouse(erpWarehouse));
    }

    @PreAuthorize("@ss.hasPermi('erp:warehouse:remove')")
    @Log(title = "仓库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(erpWarehouseService.deleteErpWarehouseByIds(ids));
    }
}
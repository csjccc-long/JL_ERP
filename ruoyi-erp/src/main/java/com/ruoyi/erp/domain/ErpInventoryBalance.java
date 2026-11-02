package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存余额对象 erp_inventory_balance
 *
 * @author ruoyi
 */
public class ErpInventoryBalance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    @Excel(name = "仓库名称")
    private String warehouseName;

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    @Excel(name = "物料编码")
    private String materialCode;

    /** 物料名称 */
    @Excel(name = "物料名称")
    private String materialName;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String specification;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;

    /** 可用数量 */
    @Excel(name = "可用数量")
    private BigDecimal availableQuantity;

    /** 移动平均成本 */
    @Excel(name = "移动平均成本")
    private BigDecimal averageCost;

    /** 库存金额 */
    @Excel(name = "库存金额")
    private BigDecimal inventoryAmount;

    /** 最后业务时间 */
    @Excel(name = "最后业务时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastBusinessTime;

    /** 是否低库存 */
    private Boolean lowStock;

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setMaterialId(Long materialId)
    {
        this.materialId = materialId;
    }

    public Long getMaterialId()
    {
        return materialId;
    }

    public void setMaterialCode(String materialCode)
    {
        this.materialCode = materialCode;
    }

    public String getMaterialCode()
    {
        return materialCode;
    }

    public void setMaterialName(String materialName)
    {
        this.materialName = materialName;
    }

    public String getMaterialName()
    {
        return materialName;
    }

    public void setSpecification(String specification)
    {
        this.specification = specification;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setAvailableQuantity(BigDecimal availableQuantity)
    {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getAvailableQuantity()
    {
        return availableQuantity;
    }

    public void setAverageCost(BigDecimal averageCost)
    {
        this.averageCost = averageCost;
    }

    public BigDecimal getAverageCost()
    {
        return averageCost;
    }

    public void setInventoryAmount(BigDecimal inventoryAmount)
    {
        this.inventoryAmount = inventoryAmount;
    }

    public BigDecimal getInventoryAmount()
    {
        return inventoryAmount;
    }

    public void setLastBusinessTime(Date lastBusinessTime)
    {
        this.lastBusinessTime = lastBusinessTime;
    }

    public Date getLastBusinessTime()
    {
        return lastBusinessTime;
    }

    public void setLowStock(Boolean lowStock)
    {
        this.lowStock = lowStock;
    }

    public Boolean getLowStock()
    {
        return lowStock;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
}

package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存流水对象 erp_inventory_ledger
 *
 * @author ruoyi
 */
public class ErpInventoryLedger extends BaseEntity
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

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 来源类型 */
    @Excel(name = "来源类型")
    private String sourceType;

    /** 来源单据ID */
    @Excel(name = "来源单据ID")
    private Long sourceId;

    /** 来源明细ID */
    private Long sourceItemId;

    /** 变动数量 */
    @Excel(name = "变动数量")
    private BigDecimal changeQuantity;

    /** 单位成本 */
    @Excel(name = "单位成本")
    private BigDecimal unitCost;

    /** 变动后结存 */
    @Excel(name = "变动后结存")
    private BigDecimal balanceQuantity;

    /** 发生时间 */
    @Excel(name = "发生时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date occurredTime;

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

    public void setBusinessType(String businessType)
    {
        this.businessType = businessType;
    }

    public String getBusinessType()
    {
        return businessType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setSourceItemId(Long sourceItemId)
    {
        this.sourceItemId = sourceItemId;
    }

    public Long getSourceItemId()
    {
        return sourceItemId;
    }

    public void setChangeQuantity(BigDecimal changeQuantity)
    {
        this.changeQuantity = changeQuantity;
    }

    public BigDecimal getChangeQuantity()
    {
        return changeQuantity;
    }

    public void setUnitCost(BigDecimal unitCost)
    {
        this.unitCost = unitCost;
    }

    public BigDecimal getUnitCost()
    {
        return unitCost;
    }

    public void setBalanceQuantity(BigDecimal balanceQuantity)
    {
        this.balanceQuantity = balanceQuantity;
    }

    public BigDecimal getBalanceQuantity()
    {
        return balanceQuantity;
    }

    public void setOccurredTime(Date occurredTime)
    {
        this.occurredTime = occurredTime;
    }

    public Date getOccurredTime()
    {
        return occurredTime;
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

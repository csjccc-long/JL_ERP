package com.ruoyi.erp.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 采购申请明细对象 erp_purchase_request_item
 *
 * @author ruoyi
 */
public class ErpPurchaseRequestItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 申请ID */
    private Long requestId;

    /** 物料ID */
    @Excel(name = "物料ID")
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

    /** 数量 */
    @Excel(name = "数量")
    private BigDecimal quantity;

    /** 预估单价 */
    @Excel(name = "预估单价")
    private BigDecimal estimatedPrice;

    /** 预估金额 */
    @Excel(name = "预估金额")
    private BigDecimal estimatedAmount;

    public void setRequestId(Long requestId)
    {
        this.requestId = requestId;
    }

    public Long getRequestId()
    {
        return requestId;
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

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice)
    {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getEstimatedPrice()
    {
        return estimatedPrice;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount)
    {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getEstimatedAmount()
    {
        return estimatedAmount;
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

package com.ruoyi.erp.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 到货入库明细对象 erp_receipt_item
 *
 * @author ruoyi
 */
public class ErpReceiptItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 入库单ID */
    private Long receiptId;

    /** 订单明细ID */
    private Long orderItemId;

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    @Excel(name = "物料编码")
    private String materialCode;

    /** 物料名称 */
    @Excel(name = "物料名称")
    private String materialName;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;

    /** 到货数量 */
    @Excel(name = "到货数量")
    private BigDecimal receivedQuantity;

    /** 合格数量 */
    @Excel(name = "合格数量")
    private BigDecimal qualifiedQuantity;

    /** 不合格数量 */
    @Excel(name = "不合格数量")
    private BigDecimal rejectedQuantity;

    /** 单位成本 */
    @Excel(name = "单位成本")
    private BigDecimal unitCost;

    public void setReceiptId(Long receiptId)
    {
        this.receiptId = receiptId;
    }

    public Long getReceiptId()
    {
        return receiptId;
    }

    public void setOrderItemId(Long orderItemId)
    {
        this.orderItemId = orderItemId;
    }

    public Long getOrderItemId()
    {
        return orderItemId;
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

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity)
    {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getReceivedQuantity()
    {
        return receivedQuantity;
    }

    public void setQualifiedQuantity(BigDecimal qualifiedQuantity)
    {
        this.qualifiedQuantity = qualifiedQuantity;
    }

    public BigDecimal getQualifiedQuantity()
    {
        return qualifiedQuantity;
    }

    public void setRejectedQuantity(BigDecimal rejectedQuantity)
    {
        this.rejectedQuantity = rejectedQuantity;
    }

    public BigDecimal getRejectedQuantity()
    {
        return rejectedQuantity;
    }

    public void setUnitCost(BigDecimal unitCost)
    {
        this.unitCost = unitCost;
    }

    public BigDecimal getUnitCost()
    {
        return unitCost;
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

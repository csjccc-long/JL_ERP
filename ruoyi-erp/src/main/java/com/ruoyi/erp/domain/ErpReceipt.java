package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;
import java.util.List;

/**
 * 到货入库单对象 erp_receipt
 *
 * @author ruoyi
 */
public class ErpReceipt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 入库单号 */
    @Excel(name = "入库单号")
    private String receiptNo;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    @Excel(name = "仓库名称")
    private String warehouseName;

    /** 到货日期 */
    @Excel(name = "到货日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date receiptDate;

    /** 质检说明 */
    @Excel(name = "质检说明")
    private String qualityNote;

    /** 入库状态 */
    @Excel(name = "入库状态")
    private String receiptStatus;

    /** 确认人 */
    private Long confirmedBy;

    /** 确认时间 */
    @Excel(name = "确认时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmedTime;

    /** 幂等键 */
    private String idempotencyKey;

    /** 部门ID */
    private Long deptId;

    /** 到货入库单明细 */
    private List<ErpReceiptItem> items;

    public void setReceiptNo(String receiptNo)
    {
        this.receiptNo = receiptNo;
    }

    public String getReceiptNo()
    {
        return receiptNo;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

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

    public void setReceiptDate(Date receiptDate)
    {
        this.receiptDate = receiptDate;
    }

    public Date getReceiptDate()
    {
        return receiptDate;
    }

    public void setQualityNote(String qualityNote)
    {
        this.qualityNote = qualityNote;
    }

    public String getQualityNote()
    {
        return qualityNote;
    }

    public void setReceiptStatus(String receiptStatus)
    {
        this.receiptStatus = receiptStatus;
    }

    public String getReceiptStatus()
    {
        return receiptStatus;
    }

    public void setConfirmedBy(Long confirmedBy)
    {
        this.confirmedBy = confirmedBy;
    }

    public Long getConfirmedBy()
    {
        return confirmedBy;
    }

    public void setConfirmedTime(Date confirmedTime)
    {
        this.confirmedTime = confirmedTime;
    }

    public Date getConfirmedTime()
    {
        return confirmedTime;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setItems(List<ErpReceiptItem> items)
    {
        this.items = items;
    }

    public List<ErpReceiptItem> getItems()
    {
        return items;
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

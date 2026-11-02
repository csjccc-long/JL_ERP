package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购订单对象 erp_purchase_order
 *
 * @author ruoyi
 */
public class ErpPurchaseOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 采购申请ID */
    private Long requestId;

    /** 采购申请单号 */
    @Excel(name = "采购申请单号")
    private String requestNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    @Excel(name = "合同编号")
    private String contractNo;

    /** 下单日期 */
    @Excel(name = "下单日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date orderDate;

    /** 期望到货日期 */
    @Excel(name = "期望到货日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expectedDate;

    /** 订单总额 */
    @Excel(name = "订单总额")
    private BigDecimal totalAmount;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String orderStatus;

    /** 审批意见 */
    private String approvalComment;

    /** 部门ID */
    private Long deptId;

    /** 采购订单明细 */
    private List<ErpPurchaseOrderItem> items;

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setRequestId(Long requestId)
    {
        this.requestId = requestId;
    }

    public Long getRequestId()
    {
        return requestId;
    }

    public void setRequestNo(String requestNo)
    {
        this.requestNo = requestNo;
    }

    public String getRequestNo()
    {
        return requestNo;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }

    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public Long getContractId()
    {
        return contractId;
    }

    public void setContractNo(String contractNo)
    {
        this.contractNo = contractNo;
    }

    public String getContractNo()
    {
        return contractNo;
    }

    public void setOrderDate(Date orderDate)
    {
        this.orderDate = orderDate;
    }

    public Date getOrderDate()
    {
        return orderDate;
    }

    public void setExpectedDate(Date expectedDate)
    {
        this.expectedDate = expectedDate;
    }

    public Date getExpectedDate()
    {
        return expectedDate;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setOrderStatus(String orderStatus)
    {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus()
    {
        return orderStatus;
    }

    public void setApprovalComment(String approvalComment)
    {
        this.approvalComment = approvalComment;
    }

    public String getApprovalComment()
    {
        return approvalComment;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setItems(List<ErpPurchaseOrderItem> items)
    {
        this.items = items;
    }

    public List<ErpPurchaseOrderItem> getItems()
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

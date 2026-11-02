package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购申请对象 erp_purchase_request
 *
 * @author ruoyi
 */
public class ErpPurchaseRequest extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 申请单号 */
    @Excel(name = "申请单号")
    private String requestNo;

    /** 申请标题 */
    @Excel(name = "申请标题")
    private String title;

    /** 申请人 */
    private Long applicantId;

    /** 申请人名称 */
    @Excel(name = "申请人名称")
    private String applicantName;

    /** 需求日期 */
    @Excel(name = "需求日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date requiredDate;

    /** 目标仓库 */
    private Long targetWarehouseId;

    /** 目标仓库 */
    @Excel(name = "目标仓库")
    private String targetWarehouseName;

    /** 申请原因 */
    @Excel(name = "申请原因")
    private String requestReason;

    /** 预估金额 */
    @Excel(name = "预估金额")
    private BigDecimal estimatedAmount;

    /** 申请状态 */
    @Excel(name = "申请状态")
    private String requestStatus;

    /** 部门ID */
    private Long deptId;

    /** 采购申请明细 */
    private List<ErpPurchaseRequestItem> items;

    public void setRequestNo(String requestNo)
    {
        this.requestNo = requestNo;
    }

    public String getRequestNo()
    {
        return requestNo;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setApplicantId(Long applicantId)
    {
        this.applicantId = applicantId;
    }

    public Long getApplicantId()
    {
        return applicantId;
    }

    public void setApplicantName(String applicantName)
    {
        this.applicantName = applicantName;
    }

    public String getApplicantName()
    {
        return applicantName;
    }

    public void setRequiredDate(Date requiredDate)
    {
        this.requiredDate = requiredDate;
    }

    public Date getRequiredDate()
    {
        return requiredDate;
    }

    public void setTargetWarehouseId(Long targetWarehouseId)
    {
        this.targetWarehouseId = targetWarehouseId;
    }

    public Long getTargetWarehouseId()
    {
        return targetWarehouseId;
    }

    public void setTargetWarehouseName(String targetWarehouseName)
    {
        this.targetWarehouseName = targetWarehouseName;
    }

    public String getTargetWarehouseName()
    {
        return targetWarehouseName;
    }

    public void setRequestReason(String requestReason)
    {
        this.requestReason = requestReason;
    }

    public String getRequestReason()
    {
        return requestReason;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount)
    {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getEstimatedAmount()
    {
        return estimatedAmount;
    }

    public void setRequestStatus(String requestStatus)
    {
        this.requestStatus = requestStatus;
    }

    public String getRequestStatus()
    {
        return requestStatus;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setItems(List<ErpPurchaseRequestItem> items)
    {
        this.items = items;
    }

    public List<ErpPurchaseRequestItem> getItems()
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

package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同对象 erp_contract
 *
 * @author ruoyi
 */
public class ErpContract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 合同编号 */
    @Excel(name = "合同编号")
    private String contractNo;

    /** 合同名称 */
    @Excel(name = "合同名称")
    private String contractName;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 生效日期 */
    @Excel(name = "生效日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 到期日期 */
    @Excel(name = "到期日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 合同金额 */
    @Excel(name = "合同金额")
    private BigDecimal contractAmount;

    /** 币种 */
    @Excel(name = "币种")
    private String currency;

    /** 付款条件 */
    @Excel(name = "付款条件")
    private String paymentTerms;

    /** 合同状态 */
    @Excel(name = "合同状态")
    private String contractStatus;

    /** 负责人 */
    @Excel(name = "负责人")
    private Long ownerId;

    /** 负责人名称 */
    @Excel(name = "负责人名称")
    private String ownerName;

    /** 签订日期 */
    @Excel(name = "签订日期", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date signedDate;

    /** 部门ID */
    private Long deptId;

    public void setContractNo(String contractNo)
    {
        this.contractNo = contractNo;
    }

    public String getContractNo()
    {
        return contractNo;
    }

    public void setContractName(String contractName)
    {
        this.contractName = contractName;
    }

    public String getContractName()
    {
        return contractName;
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

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setContractAmount(BigDecimal contractAmount)
    {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getContractAmount()
    {
        return contractAmount;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setPaymentTerms(String paymentTerms)
    {
        this.paymentTerms = paymentTerms;
    }

    public String getPaymentTerms()
    {
        return paymentTerms;
    }

    public void setContractStatus(String contractStatus)
    {
        this.contractStatus = contractStatus;
    }

    public String getContractStatus()
    {
        return contractStatus;
    }

    public void setOwnerId(Long ownerId)
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setSignedDate(Date signedDate)
    {
        this.signedDate = signedDate;
    }

    public Date getSignedDate()
    {
        return signedDate;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
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

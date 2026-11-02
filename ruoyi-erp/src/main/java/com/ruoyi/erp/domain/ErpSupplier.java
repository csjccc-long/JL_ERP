package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 供应商对象 erp_supplier
 *
 * @author ruoyi
 */
public class ErpSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 供应商编码 */
    @Excel(name = "供应商编码")
    private String supplierCode;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 统一社会信用代码 */
    @Excel(name = "统一社会信用代码")
    private String creditCode;

    /** 供应商分类 */
    @Excel(name = "供应商分类")
    private String supplierCategory;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contactName;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 地址 */
    @Excel(name = "地址")
    private String address;

    /** 开户银行 */
    @Excel(name = "开户银行")
    private String bankName;

    /** 银行账号 */
    @Excel(name = "银行账号")
    private String bankAccount;

    /** 税号 */
    @Excel(name = "税号")
    private String taxNo;

    /** 准入状态 */
    @Excel(name = "准入状态")
    private String admissionStatus;

    /** 风险等级 */
    @Excel(name = "风险等级")
    private String riskLevel;

    /** 负责人 */
    @Excel(name = "负责人")
    private Long ownerId;

    /** 准入时间 */
    @Excel(name = "准入时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date admissionTime;

    /** 部门ID */
    private Long deptId;

    public void setSupplierCode(String supplierCode)
    {
        this.supplierCode = supplierCode;
    }

    public String getSupplierCode()
    {
        return supplierCode;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setCreditCode(String creditCode)
    {
        this.creditCode = creditCode;
    }

    public String getCreditCode()
    {
        return creditCode;
    }

    public void setSupplierCategory(String supplierCategory)
    {
        this.supplierCategory = supplierCategory;
    }

    public String getSupplierCategory()
    {
        return supplierCategory;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    public String getContactName()
    {
        return contactName;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getAddress()
    {
        return address;
    }

    public void setBankName(String bankName)
    {
        this.bankName = bankName;
    }

    public String getBankName()
    {
        return bankName;
    }

    public void setBankAccount(String bankAccount)
    {
        this.bankAccount = bankAccount;
    }

    public String getBankAccount()
    {
        return bankAccount;
    }

    public void setTaxNo(String taxNo)
    {
        this.taxNo = taxNo;
    }

    public String getTaxNo()
    {
        return taxNo;
    }

    public void setAdmissionStatus(String admissionStatus)
    {
        this.admissionStatus = admissionStatus;
    }

    public String getAdmissionStatus()
    {
        return admissionStatus;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setOwnerId(Long ownerId)
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId()
    {
        return ownerId;
    }

    public void setAdmissionTime(Date admissionTime)
    {
        this.admissionTime = admissionTime;
    }

    public Date getAdmissionTime()
    {
        return admissionTime;
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

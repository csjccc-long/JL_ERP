package com.ruoyi.erp.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 审批记录对象 erp_approval_record
 *
 * @author ruoyi
 */
public class ErpApprovalRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务ID */
    @Excel(name = "业务ID")
    private Long businessId;

    /** 动作 */
    @Excel(name = "动作")
    private String action;

    /** 原状态 */
    @Excel(name = "原状态")
    private String fromStatus;

    /** 新状态 */
    @Excel(name = "新状态")
    private String toStatus;

    /** 审批意见 */
    @Excel(name = "审批意见")
    private String opinion;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    @Excel(name = "操作人名称")
    private String operatorName;

    /** 操作时间 */
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    public void setBusinessType(String businessType)
    {
        this.businessType = businessType;
    }

    public String getBusinessType()
    {
        return businessType;
    }

    public void setBusinessId(Long businessId)
    {
        this.businessId = businessId;
    }

    public Long getBusinessId()
    {
        return businessId;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    public void setFromStatus(String fromStatus)
    {
        this.fromStatus = fromStatus;
    }

    public String getFromStatus()
    {
        return fromStatus;
    }

    public void setToStatus(String toStatus)
    {
        this.toStatus = toStatus;
    }

    public String getToStatus()
    {
        return toStatus;
    }

    public void setOpinion(String opinion)
    {
        this.opinion = opinion;
    }

    public String getOpinion()
    {
        return opinion;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperateTime(Date operateTime)
    {
        this.operateTime = operateTime;
    }

    public Date getOperateTime()
    {
        return operateTime;
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

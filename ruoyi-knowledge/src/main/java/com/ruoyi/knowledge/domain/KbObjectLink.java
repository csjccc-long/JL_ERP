package com.ruoyi.knowledge.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档业务关联对象 kb_object_link
 *
 * @author ruoyi
 */
public class KbObjectLink extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文档ID */
    private Long documentId;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 关联类型 */
    private String relationType;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setDocumentId(Long documentId)
    {
        this.documentId = documentId;
    }

    public Long getDocumentId()
    {
        return documentId;
    }

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

    public void setRelationType(String relationType)
    {
        this.relationType = relationType;
    }

    public String getRelationType()
    {
        return relationType;
    }
}

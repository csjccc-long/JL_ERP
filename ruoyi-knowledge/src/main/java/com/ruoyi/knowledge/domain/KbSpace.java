package com.ruoyi.knowledge.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 知识空间对象 kb_space
 *
 * @author ruoyi
 */
public class KbSpace extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 知识空间名称 */
    private String spaceName;

    /** 空间类型 */
    private String spaceType;

    /** RAGFlow 数据集ID */
    private String ragDatasetId;

    /** 描述 */
    private String description;

    /** 负责人 */
    private Long ownerId;

    /** 空间状态 */
    private String spaceStatus;

    /**  */
    private Long deptId;

    /**  */
    private Integer version;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setSpaceName(String spaceName)
    {
        this.spaceName = spaceName;
    }

    public String getSpaceName()
    {
        return spaceName;
    }

    public void setSpaceType(String spaceType)
    {
        this.spaceType = spaceType;
    }

    public String getSpaceType()
    {
        return spaceType;
    }

    public void setRagDatasetId(String ragDatasetId)
    {
        this.ragDatasetId = ragDatasetId;
    }

    public String getRagDatasetId()
    {
        return ragDatasetId;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setOwnerId(Long ownerId)
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId()
    {
        return ownerId;
    }

    public void setSpaceStatus(String spaceStatus)
    {
        this.spaceStatus = spaceStatus;
    }

    public String getSpaceStatus()
    {
        return spaceStatus;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public Integer getVersion()
    {
        return version;
    }
}

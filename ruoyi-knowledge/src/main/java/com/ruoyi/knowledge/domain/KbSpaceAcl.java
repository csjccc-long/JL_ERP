package com.ruoyi.knowledge.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 知识空间授权对象 kb_space_acl
 *
 * @author ruoyi
 */
public class KbSpaceAcl extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 空间ID */
    private Long spaceId;

    /** 授权对象类型 */
    private String subjectType;

    /** 授权对象ID */
    private Long subjectId;

    /** 权限级别 */
    private String permissionLevel;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setSpaceId(Long spaceId)
    {
        this.spaceId = spaceId;
    }

    public Long getSpaceId()
    {
        return spaceId;
    }

    public void setSubjectType(String subjectType)
    {
        this.subjectType = subjectType;
    }

    public String getSubjectType()
    {
        return subjectType;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setPermissionLevel(String permissionLevel)
    {
        this.permissionLevel = permissionLevel;
    }

    public String getPermissionLevel()
    {
        return permissionLevel;
    }
}

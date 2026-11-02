package com.ruoyi.knowledge.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 助手会话对象 kb_assistant_session
 *
 * @author ruoyi
 */
public class KbAssistantSession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 会话标题 */
    private String title;

    /** 上下文类型 */
    private String contextType;

    /** 上下文ID */
    private Long contextId;

    /** 会话状态 */
    private String sessionStatus;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setContextType(String contextType)
    {
        this.contextType = contextType;
    }

    public String getContextType()
    {
        return contextType;
    }

    public void setContextId(Long contextId)
    {
        this.contextId = contextId;
    }

    public Long getContextId()
    {
        return contextId;
    }

    public void setSessionStatus(String sessionStatus)
    {
        this.sessionStatus = sessionStatus;
    }

    public String getSessionStatus()
    {
        return sessionStatus;
    }
}

package com.ruoyi.knowledge.domain.dto;

import java.io.Serializable;

/**
 * 智能助手提问请求
 *
 * @author ruoyi
 */
public class KbAskRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 问题内容 */
    private String question;

    /** 会话ID（为空时创建新会话） */
    private Long sessionId;

    /** 上下文类型：POLICY/SUPPLIER/CONTRACT/REQUEST/ORDER/RECEIPT/INVENTORY */
    private String contextType;

    /** 上下文业务ID */
    private Long contextId;

    /** 指定知识空间（为空时使用全部可访问空间） */
    private Long spaceId;

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getContextType()
    {
        return contextType;
    }

    public void setContextType(String contextType)
    {
        this.contextType = contextType;
    }

    public Long getContextId()
    {
        return contextId;
    }

    public void setContextId(Long contextId)
    {
        this.contextId = contextId;
    }

    public Long getSpaceId()
    {
        return spaceId;
    }

    public void setSpaceId(Long spaceId)
    {
        this.spaceId = spaceId;
    }
}

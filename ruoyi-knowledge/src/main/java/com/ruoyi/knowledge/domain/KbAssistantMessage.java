package com.ruoyi.knowledge.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 助手消息对象 kb_assistant_message
 *
 * @author ruoyi
 */
public class KbAssistantMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 角色 */
    private String role;

    /** 内容 */
    private String content;

    /** 模型 */
    private String modelName;

    /** 耗时 */
    private Integer latencyMs;

    /** 追踪ID */
    private String traceId;

    /** 创建时间 */
    private Date createdTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getRole()
    {
        return role;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setLatencyMs(Integer latencyMs)
    {
        this.latencyMs = latencyMs;
    }

    public Integer getLatencyMs()
    {
        return latencyMs;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setCreatedTime(Date createdTime)
    {
        this.createdTime = createdTime;
    }

    public Date getCreatedTime()
    {
        return createdTime;
    }
}

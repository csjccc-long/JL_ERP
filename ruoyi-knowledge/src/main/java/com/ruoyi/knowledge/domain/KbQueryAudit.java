package com.ruoyi.knowledge.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 问答审计对象 kb_query_audit
 *
 * @author ruoyi
 */
public class KbQueryAudit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 追踪ID */
    private String traceId;

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 问题 */
    private String question;

    /** 上下文类型 */
    private String contextType;

    /** 上下文ID */
    private Long contextId;

    /** 空间范围 */
    private String spaceIdsJson;

    /** 文档范围 */
    private String documentIdsJson;

    /** 结果状态 */
    private String status;

    /** 模型 */
    private String modelName;

    /** 耗时 */
    private Integer latencyMs;

    /** 错误信息 */
    private String errorMessage;

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

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getQuestion()
    {
        return question;
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

    public void setSpaceIdsJson(String spaceIdsJson)
    {
        this.spaceIdsJson = spaceIdsJson;
    }

    public String getSpaceIdsJson()
    {
        return spaceIdsJson;
    }

    public void setDocumentIdsJson(String documentIdsJson)
    {
        this.documentIdsJson = documentIdsJson;
    }

    public String getDocumentIdsJson()
    {
        return documentIdsJson;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
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

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
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

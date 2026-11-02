package com.ruoyi.knowledge.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 索引任务对象 kb_index_job
 *
 * @author ruoyi
 */
public class KbIndexJob extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文档ID */
    private Long documentId;

    /** 任务状态 */
    private String jobStatus;

    /** 尝试次数 */
    private Integer attemptCount;

    /** 下次重试时间 */
    private Date nextRetryTime;

    /** 开始时间 */
    private Date startedTime;

    /** 完成时间 */
    private Date finishedTime;

    /** 错误信息 */
    private String errorMessage;

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

    public void setJobStatus(String jobStatus)
    {
        this.jobStatus = jobStatus;
    }

    public String getJobStatus()
    {
        return jobStatus;
    }

    public void setAttemptCount(Integer attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public Integer getAttemptCount()
    {
        return attemptCount;
    }

    public void setNextRetryTime(Date nextRetryTime)
    {
        this.nextRetryTime = nextRetryTime;
    }

    public Date getNextRetryTime()
    {
        return nextRetryTime;
    }

    public void setStartedTime(Date startedTime)
    {
        this.startedTime = startedTime;
    }

    public Date getStartedTime()
    {
        return startedTime;
    }

    public void setFinishedTime(Date finishedTime)
    {
        this.finishedTime = finishedTime;
    }

    public Date getFinishedTime()
    {
        return finishedTime;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }
}

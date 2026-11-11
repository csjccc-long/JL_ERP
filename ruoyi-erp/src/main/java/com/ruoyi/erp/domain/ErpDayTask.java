package com.ruoyi.erp.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 按日的待办与自动化任务对象 erp_day_task
 *
 * <p>个人中心「打卡足迹」右侧的时间线：每一天有哪些待办被处理、哪些自动化跑过，
 * 成功、失败、进行中一目了然。</p>
 *
 * @author ruoyi
 */
public class ErpDayTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long dayTaskId;

    /** 归属日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate taskDate;

    /** 类型（TODO 待办 / AUTOMATION 自动化） */
    private String taskType;

    /** 标题 */
    private String title;

    /** 执行详情或失败原因 */
    private String detail;

    /** 状态（PENDING 待处理 / RUNNING 进行中 / DONE 完成 / FAILED 失败） */
    private String status;

    /** 负责人 */
    private String owner;

    /** 关联单号 */
    private String refCode;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    public Long getDayTaskId()
    {
        return dayTaskId;
    }

    public void setDayTaskId(Long dayTaskId)
    {
        this.dayTaskId = dayTaskId;
    }

    public LocalDate getTaskDate()
    {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate)
    {
        this.taskDate = taskDate;
    }

    public String getTaskType()
    {
        return taskType;
    }

    public void setTaskType(String taskType)
    {
        this.taskType = taskType;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDetail()
    {
        return detail;
    }

    public void setDetail(String detail)
    {
        this.detail = detail;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getRefCode()
    {
        return refCode;
    }

    public void setRefCode(String refCode)
    {
        this.refCode = refCode;
    }

    public LocalDateTime getStartedTime()
    {
        return startedTime;
    }

    public void setStartedTime(LocalDateTime startedTime)
    {
        this.startedTime = startedTime;
    }

    public LocalDateTime getFinishedTime()
    {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime)
    {
        this.finishedTime = finishedTime;
    }
}

package com.ruoyi.erp.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户打卡记录对象 erp_checkin
 *
 * @author ruoyi
 */
public class ErpCheckin extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 打卡ID */
    private Long checkinId;

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String userName;

    /** 打卡日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinDate;

    /** 打卡时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    /** 当日动作数：登录与业务操作次数，用于热力强度 */
    private Integer actions;

    /** 来源（manual 手动打卡 / seed 演示数据） */
    private String source;

    public Long getCheckinId()
    {
        return checkinId;
    }

    public void setCheckinId(Long checkinId)
    {
        this.checkinId = checkinId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public LocalDate getCheckinDate()
    {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate)
    {
        this.checkinDate = checkinDate;
    }

    public LocalDateTime getCheckinTime()
    {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime)
    {
        this.checkinTime = checkinTime;
    }

    public Integer getActions()
    {
        return actions;
    }

    public void setActions(Integer actions)
    {
        this.actions = actions;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}

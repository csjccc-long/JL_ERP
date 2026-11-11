package com.ruoyi.erp.service;

import java.util.Map;

/**
 * 打卡记录 服务层
 *
 * @author ruoyi
 */
public interface IErpCheckinService
{
    /**
     * 打卡看板：最近一年的每日活跃度、今日打卡状态与连续天数
     *
     * @param userId 用户ID
     * @param userName 登录账号
     * @return 看板数据
     */
    Map<String, Object> board(Long userId, String userName);

    /**
     * 今日打卡（一天一次，重复调用返回已有时刻）
     *
     * @param userId 用户ID
     * @param userName 登录账号
     * @return 打卡后的看板数据
     */
    Map<String, Object> checkin(Long userId, String userName);

    /**
     * 某一天的明细：当天的待办、自动化任务与审批动作
     *
     * @param userId 用户ID
     * @param userName 登录账号
     * @param date 日期 yyyy-MM-dd，为空取今天
     * @return 当日明细
     */
    Map<String, Object> day(Long userId, String userName, String date);
}

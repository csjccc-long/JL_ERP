package com.ruoyi.erp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.service.IErpCheckinService;

/**
 * 个人打卡
 *
 * <p>个人中心的「我的打卡」：只读写当前登录人自己的记录，任何已登录用户都可用。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/erp/checkin")
public class ErpCheckinController extends BaseController
{
    @Autowired
    private IErpCheckinService checkinService;

    /** 打卡看板：最近一年每日热度、今日状态与连续天数 */
    @GetMapping("/board")
    public AjaxResult board()
    {
        return success(checkinService.board(getUserId(), getUsername()));
    }

    /** 某一天的待办与自动化任务明细 */
    @GetMapping("/day")
    public AjaxResult day(@RequestParam(value = "date", required = false) String date)
    {
        return success(checkinService.day(getUserId(), getUsername(), date));
    }

    /** 今日打卡 */
    @PostMapping
    public AjaxResult checkin()
    {
        return success(checkinService.checkin(getUserId(), getUsername()));
    }
}

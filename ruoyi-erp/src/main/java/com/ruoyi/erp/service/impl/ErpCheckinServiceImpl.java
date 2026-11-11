package com.ruoyi.erp.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.ruoyi.erp.domain.ErpCheckin;
import com.ruoyi.erp.domain.ErpDayTask;
import com.ruoyi.erp.mapper.ErpCheckinMapper;
import com.ruoyi.erp.mapper.ErpDayTaskMapper;
import com.ruoyi.erp.service.IErpCheckinService;

/**
 * 打卡记录 服务层实现
 *
 * <p>热力的「强度」= 当天打卡 1 次 + 登录成功次数 + 业务操作次数，
 * 全部取自系统里真实发生过的记录（erp_checkin / sys_logininfor / sys_oper_log）。</p>
 *
 * @author ruoyi
 */
@Service
public class ErpCheckinServiceImpl implements IErpCheckinService
{
    /** 展示范围：最近 365 天（对齐到周一，正好 53 列） */
    private static final int RANGE_DAYS = 364;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] WEEKDAYS = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    @Autowired
    private ErpCheckinMapper checkinMapper;

    @Autowired
    private ErpDayTaskMapper dayTaskMapper;

    @Override
    public Map<String, Object> board(Long userId, String userName)
    {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(RANGE_DAYS);
        // 起止都落在周一，网格才是整齐的 53 列
        start = start.minusDays(start.getDayOfWeek().getValue() - 1L);

        Map<LocalDate, String> checkinTime = new HashMap<LocalDate, String>();
        Map<LocalDate, Integer> checkinActions = new HashMap<LocalDate, Integer>();
        for (Map<String, Object> row : checkinMapper.selectCheckinRows(userId, start, today))
        {
            LocalDate day = LocalDate.parse(String.valueOf(row.get("checkinDate")), DATE);
            checkinTime.put(day, String.valueOf(row.get("checkinTime")));
            checkinActions.put(day, asInt(row.get("actions")));
        }

        Map<LocalDate, Integer> logins = countByDay(
                checkinMapper.selectLoginCountByDay(userName, start.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        Map<LocalDate, Integer> opers = countByDay(
                checkinMapper.selectOperCountByDay(userName, start.atStartOfDay(), today.plusDays(1).atStartOfDay()));

        // 逐日合并：打卡 1 + 当天的登录与操作
        List<Map<String, Object>> days = new ArrayList<Map<String, Object>>();
        int totalActions = 0;
        int busiestCount = 0;
        String busiestDate = null;
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1))
        {
            int count = (checkinTime.containsKey(day) ? 1 : 0) + checkinActions.getOrDefault(day, 0)
                    + logins.getOrDefault(day, 0) + opers.getOrDefault(day, 0);
            if (count <= 0)
            {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("date", day.format(DATE));
            item.put("count", count);
            item.put("checked", checkinTime.containsKey(day));
            days.add(item);
            totalActions += count;
            if (count > busiestCount)
            {
                busiestCount = count;
                busiestDate = day.format(DATE);
            }
        }

        // 失败的自动化与待办：热力图上打红点，点开才是详情
        Map<String, Object> fails = new LinkedHashMap<String, Object>();
        for (Map<String, Object> row : dayTaskMapper.countFailedByDay(start, today))
        {
            fails.put(String.valueOf(row.get("day")), asInt(row.get("amount")));
        }

        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("checkedDays", checkinTime.size());
        stats.put("checkedTotal", checkinMapper.countByUser(userId));
        stats.put("totalActions", totalActions);
        stats.put("busiest", busiestCount);
        stats.put("busiestDate", busiestDate);
        stats.put("currentStreak", currentStreak(checkinTime, today));
        Map<String, Object> longest = longestStreak(checkinTime);
        stats.put("longestStreak", longest.get("days"));
        stats.put("longestStart", longest.get("start"));
        stats.put("longestEnd", longest.get("end"));
        Map<String, Integer> byStatus = new HashMap<String, Integer>();
        for (Map<String, Object> row : dayTaskMapper.countByStatus(start, today))
        {
            byStatus.put(String.valueOf(row.get("status")), asInt(row.get("amount")));
        }
        stats.put("failedTotal", byStatus.getOrDefault("FAILED", 0));
        stats.put("pendingTotal", byStatus.getOrDefault("PENDING", 0));
        stats.put("taskTotal", byStatus.values().stream().mapToInt(Integer::intValue).sum());

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("start", start.format(DATE));
        data.put("end", today.format(DATE));
        data.put("today", today.format(DATE));
        data.put("checked", checkinTime.containsKey(today));
        data.put("checkinTime", checkinTime.get(today));
        data.put("days", days);
        data.put("fails", fails);
        data.put("stats", stats);
        return data;
    }

    @Override
    public Map<String, Object> checkin(Long userId, String userName)
    {
        LocalDate today = LocalDate.now();
        ErpCheckin exists = checkinMapper.selectByUserAndDate(userId, today);
        if (exists == null)
        {
            ErpCheckin checkin = new ErpCheckin();
            checkin.setUserId(userId);
            checkin.setUserName(userName);
            checkin.setCheckinDate(today);
            checkin.setCheckinTime(LocalDateTime.now());
            checkin.setActions(0);
            checkin.setSource("manual");
            checkin.setRemark("");
            try
            {
                checkinMapper.insertErpCheckin(checkin);
            }
            catch (DuplicateKeyException e)
            {
                // 并发重复打卡：当天已有记录，按已打卡返回
            }
        }
        return board(userId, userName);
    }


    @Override
    public Map<String, Object> day(Long userId, String userName, String date)
    {
        LocalDate day = parseDay(date);
        ErpCheckin checkin = checkinMapper.selectByUserAndDate(userId, day);

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (ErpDayTask task : dayTaskMapper.selectByDate(day))
        {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", "task-" + task.getDayTaskId());
            item.put("kind", "AUTOMATION".equals(task.getTaskType()) ? "AUTOMATION" : "TODO");
            item.put("title", task.getTitle());
            item.put("detail", task.getDetail() == null ? "" : task.getDetail());
            item.put("status", task.getStatus());
            item.put("owner", task.getOwner());
            item.put("ref", task.getRefCode());
            item.put("time", task.getStartedTime() == null ? null : task.getStartedTime().format(TIME));
            item.put("finishedTime", task.getFinishedTime() == null ? null : task.getFinishedTime().format(TIME));
            items.add(item);
        }
        // 当天真实发生过的审批动作，和上面的人工/自动任务排在同一条时间线上
        for (Map<String, Object> event : dayTaskMapper.selectApprovalEvents(day, day))
        {
            String toStatus = String.valueOf(event.get("to_status"));
            String action = String.valueOf(event.get("action"));
            String business = businessLabel(String.valueOf(event.get("business_type")));
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", "approval-" + event.get("id"));
            item.put("kind", "APPROVAL");
            item.put("title", business + " #" + event.get("business_id") + " · " + actionLabel(action));
            item.put("detail", event.get("opinion") == null ? business + "流转至 " + toStatus : String.valueOf(event.get("opinion")));
            item.put("status", "REJECTED".equals(toStatus) ? "FAILED"
                    : "PENDING_APPROVAL".equals(toStatus) ? "PENDING" : "DONE");
            item.put("owner", event.get("operator_name"));
            item.put("ref", String.valueOf(event.get("business_type")).toLowerCase() + "-" + event.get("business_id"));
            item.put("time", event.get("operateTime"));
            item.put("finishedTime", event.get("operateTime"));
            items.add(item);
        }
        // 一条时间线，按发生先后读下来
        items.sort((a, b) -> String.valueOf(a.get("time") == null ? "99:99" : a.get("time"))
                .compareTo(String.valueOf(b.get("time") == null ? "99:99" : b.get("time"))));

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("total", items.size());
        counts.put("todo", 0);
        counts.put("automation", 0);
        counts.put("done", 0);
        counts.put("failed", 0);
        counts.put("running", 0);
        counts.put("pending", 0);
        for (Map<String, Object> item : items)
        {
            String kind = String.valueOf(item.get("kind"));
            String status = String.valueOf(item.get("status"));
            if ("AUTOMATION".equals(kind))
            {
                counts.put("automation", counts.get("automation") + 1);
            }
            else
            {
                counts.put("todo", counts.get("todo") + 1);
            }
            String key = "RUNNING".equals(status) ? "running" : "PENDING".equals(status) ? "pending"
                    : "FAILED".equals(status) ? "failed" : "done";
            counts.put(key, counts.get(key) + 1);
        }

        Map<LocalDate, Integer> logins = countByDay(
                checkinMapper.selectLoginCountByDay(userName, day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
        Map<LocalDate, Integer> opers = countByDay(
                checkinMapper.selectOperCountByDay(userName, day.atStartOfDay(), day.plusDays(1).atStartOfDay()));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("date", day.format(DATE));
        data.put("weekday", WEEKDAYS[day.getDayOfWeek().getValue() - 1]);
        data.put("checked", checkin != null);
        data.put("checkinTime", checkin == null || checkin.getCheckinTime() == null ? null : checkin.getCheckinTime().format(TIME));
        data.put("actions", logins.getOrDefault(day, 0) + opers.getOrDefault(day, 0));
        data.put("counts", counts);
        data.put("items", items);
        return data;
    }

    /** 宽容地解析 yyyy-MM-dd，非法或为空按今天处理 */
    private LocalDate parseDay(String date)
    {
        if (date == null || date.trim().isEmpty())
        {
            return LocalDate.now();
        }
        try
        {
            return LocalDate.parse(date.trim(), DATE);
        }
        catch (RuntimeException e)
        {
            return LocalDate.now();
        }
    }

    private String businessLabel(String type)
    {
        switch (type)
        {
            case "CONTRACT":
                return "采购合同";
            case "PURCHASE_REQUEST":
                return "采购申请";
            case "PURCHASE_ORDER":
                return "采购订单";
            case "RECEIPT":
                return "入库单";
            case "PAYMENT":
                return "付款申请";
            default:
                return "业务单据";
        }
    }

    private String actionLabel(String action)
    {
        switch (action)
        {
            case "SUBMIT":
                return "提交";
            case "APPROVE":
                return "审批通过";
            case "REJECT":
                return "驳回";
            case "CONFIRM":
                return "确认";
            case "CANCEL":
                return "撤回";
            default:
                return action;
        }
    }

    /** 结果集按天聚合：{day, amount} */
    private Map<LocalDate, Integer> countByDay(List<Map<String, Object>> rows)
    {
        Map<LocalDate, Integer> result = new HashMap<LocalDate, Integer>();
        for (Map<String, Object> row : rows)
        {
            Object day = row.get("day");
            if (day == null)
            {
                continue;
            }
            result.put(LocalDate.parse(String.valueOf(day), DATE), asInt(row.get("amount")));
        }
        return result;
    }

    /**
     * 当前连续打卡天数：今天已打卡就从今天往前数，没打卡则从昨天往前数
     * （今天还没过完，不能算断签）。
     */
    private int currentStreak(Map<LocalDate, String> checkinTime, LocalDate today)
    {
        LocalDate day = checkinTime.containsKey(today) ? today : today.minusDays(1);
        int streak = 0;
        while (checkinTime.containsKey(day))
        {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    /** 最长连续打卡：日期天然有序，顺着扫一遍即可 */
    private Map<String, Object> longestStreak(Map<LocalDate, String> checkinTime)
    {
        TreeMap<LocalDate, String> sorted = new TreeMap<LocalDate, String>(checkinTime);
        int best = 0;
        LocalDate bestStart = null;
        LocalDate bestEnd = null;
        LocalDate runStart = null;
        LocalDate prev = null;
        int run = 0;
        for (LocalDate day : sorted.keySet())
        {
            if (prev != null && prev.plusDays(1).equals(day))
            {
                run++;
            }
            else
            {
                run = 1;
                runStart = day;
            }
            if (run > best)
            {
                best = run;
                bestStart = runStart;
                bestEnd = day;
            }
            prev = day;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("days", best);
        result.put("start", bestStart == null ? null : bestStart.format(DATE));
        result.put("end", bestEnd == null ? null : bestEnd.format(DATE));
        return result;
    }

    private int asInt(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        if (value == null)
        {
            return 0;
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
}

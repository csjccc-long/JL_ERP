package com.ruoyi.erp.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.erp.domain.ErpDayTask;

/**
 * 按日的待办与自动化任务 数据层
 *
 * @author ruoyi
 */
public interface ErpDayTaskMapper
{
    /** 某一天的全部任务，按开始时间正序 */
    List<ErpDayTask> selectByDate(@Param("taskDate") LocalDate taskDate);

    /** 一段时间内某一天的失败次数，供热力图打红点 */
    List<Map<String, Object>> countFailedByDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 一段时间内按状态汇总 */
    List<Map<String, Object>> countByStatus(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 某一天发生的审批动作（提交、审批、驳回、确认） */
    List<Map<String, Object>> selectApprovalEvents(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

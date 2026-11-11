package com.ruoyi.erp.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.erp.domain.ErpCheckin;

/**
 * 打卡记录 数据层
 *
 * @author ruoyi
 */
public interface ErpCheckinMapper
{
    /** 查询某人某天的打卡记录 */
    ErpCheckin selectByUserAndDate(@Param("userId") Long userId, @Param("checkinDate") LocalDate checkinDate);

    /** 查询某人一段时间内的打卡记录（仅已打卡的日期） */
    List<Map<String, Object>> selectCheckinRows(@Param("userId") Long userId, @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** 按天统计登录成功次数 */
    List<Map<String, Object>> selectLoginCountByDay(@Param("userName") String userName,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按天统计业务操作次数 */
    List<Map<String, Object>> selectOperCountByDay(@Param("userName") String userName,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 新增打卡 */
    int insertErpCheckin(ErpCheckin checkin);

    /** 累计打卡天数 */
    int countByUser(@Param("userId") Long userId);
}

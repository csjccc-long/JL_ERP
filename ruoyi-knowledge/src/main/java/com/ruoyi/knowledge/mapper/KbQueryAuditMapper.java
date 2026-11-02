package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbQueryAudit;

/**
 * 问答审计Mapper接口
 *
 * @author ruoyi
 */
public interface KbQueryAuditMapper
{
    KbQueryAudit selectById(@Param("id") Long id);

    List<KbQueryAudit> selectList(KbQueryAudit query);

    int insert(KbQueryAudit entity);

    int update(KbQueryAudit entity);

    int deleteById(@Param("id") Long id);

    /**
     * 按追踪ID查询问答审计
     *
     * @param traceId 追踪ID
     * @return 审计记录
     */
    KbQueryAudit selectByTraceId(@Param("traceId") String traceId);

    /**
     * 查询最近高频问询
     *
     * @param limit 数量上限
     * @return 问询统计
     */
    List<java.util.Map<String, Object>> selectTopQuestions(@Param("limit") int limit);

    /**
     * 查询最近失败问询
     *
     * @param limit 数量上限
     * @return 记录集合
     */
    List<KbQueryAudit> selectRecentFailures(@Param("limit") int limit);
}

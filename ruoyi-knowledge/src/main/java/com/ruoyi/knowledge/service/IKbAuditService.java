package com.ruoyi.knowledge.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.knowledge.domain.KbQueryAudit;

/**
 * 问答审计服务
 *
 * @author ruoyi
 */
public interface IKbAuditService
{
    /**
     * 记录问答审计
     *
     * @param audit 审计记录
     */
    void record(KbQueryAudit audit);

    /**
     * 查询问答审计列表
     *
     * @param query 查询条件
     * @return 审计集合
     */
    List<KbQueryAudit> selectKbQueryAuditList(KbQueryAudit query);

    /**
     * 按追踪ID查询
     *
     * @param traceId 追踪ID
     * @return 审计记录
     */
    KbQueryAudit selectByTraceId(String traceId);

    /**
     * 近 7 天高频问询
     *
     * @param limit 数量上限
     * @return 统计集合
     */
    List<Map<String, Object>> selectTopQuestions(int limit);

    /**
     * 最近失败问询
     *
     * @param limit 数量上限
     * @return 审计集合
     */
    List<KbQueryAudit> selectRecentFailures(int limit);
}

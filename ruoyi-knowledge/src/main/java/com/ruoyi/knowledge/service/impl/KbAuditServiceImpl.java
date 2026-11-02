package com.ruoyi.knowledge.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.knowledge.domain.KbQueryAudit;
import com.ruoyi.knowledge.mapper.KbQueryAuditMapper;
import com.ruoyi.knowledge.service.IKbAuditService;

/**
 * 问答审计服务实现
 *
 * @author ruoyi
 */
@Service
public class KbAuditServiceImpl implements IKbAuditService
{
    private static final Logger log = LoggerFactory.getLogger(KbAuditServiceImpl.class);

    @Autowired
    private KbQueryAuditMapper queryAuditMapper;

    @Override
    public void record(KbQueryAudit audit)
    {
        if (audit.getCreatedTime() == null)
        {
            audit.setCreatedTime(new Date());
        }
        // 审计写入失败不得影响问答主流程
        try
        {
            queryAuditMapper.insert(audit);
        }
        catch (RuntimeException e)
        {
            log.warn("问答审计写入失败：traceId={}，原因={}", audit.getTraceId(), e.getMessage());
        }
    }

    @Override
    public List<KbQueryAudit> selectKbQueryAuditList(KbQueryAudit query)
    {
        return queryAuditMapper.selectList(query);
    }

    @Override
    public KbQueryAudit selectByTraceId(String traceId)
    {
        KbQueryAudit audit = queryAuditMapper.selectByTraceId(traceId);
        if (audit == null)
        {
            throw new ServiceException("审计记录不存在");
        }
        return audit;
    }

    @Override
    public List<Map<String, Object>> selectTopQuestions(int limit)
    {
        return queryAuditMapper.selectTopQuestions(limit <= 0 ? 5 : Math.min(limit, 20));
    }

    @Override
    public List<KbQueryAudit> selectRecentFailures(int limit)
    {
        return queryAuditMapper.selectRecentFailures(limit <= 0 ? 5 : Math.min(limit, 20));
    }
}

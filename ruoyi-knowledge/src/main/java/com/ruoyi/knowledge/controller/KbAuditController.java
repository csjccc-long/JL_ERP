package com.ruoyi.knowledge.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.knowledge.domain.KbQueryAudit;
import com.ruoyi.knowledge.service.IKbAuditService;

/**
 * 问答审计
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/knowledge/audit")
public class KbAuditController extends BaseController
{
    @Autowired
    private IKbAuditService auditService;

    /**
     * 查询问答审计
     */
    @PreAuthorize("@ss.hasPermi('knowledge:audit:list')")
    @GetMapping("/queries")
    public TableDataInfo list(KbQueryAudit query)
    {
        startPage();
        List<KbQueryAudit> list = auditService.selectKbQueryAuditList(query);
        return getDataTable(list);
    }

    /**
     * 查询问答审计详情
     */
    @PreAuthorize("@ss.hasPermi('knowledge:audit:list')")
    @GetMapping("/queries/{traceId}")
    public AjaxResult detail(@PathVariable String traceId)
    {
        return success(auditService.selectByTraceId(traceId));
    }

    /**
     * 审计概览（驾驶舱使用）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:audit:list')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("topQuestions", auditService.selectTopQuestions(5));
        result.put("recentFailures", auditService.selectRecentFailures(5));
        return success(result);
    }
}

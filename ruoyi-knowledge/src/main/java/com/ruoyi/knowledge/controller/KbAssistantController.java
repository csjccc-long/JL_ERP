package com.ruoyi.knowledge.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.knowledge.domain.KbAssistantMessage;
import com.ruoyi.knowledge.domain.KbAssistantSession;
import com.ruoyi.knowledge.domain.dto.KbAskRequest;
import com.ruoyi.knowledge.service.IKbAssistantService;

/**
 * 智能助手
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/assistant")
public class KbAssistantController extends BaseController
{
    @Autowired
    private IKbAssistantService assistantService;

    /**
     * 会话列表
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @GetMapping("/sessions")
    public AjaxResult sessions()
    {
        List<KbAssistantSession> list = assistantService.selectSessions();
        return success(list);
    }

    /**
     * 新建会话
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @Log(title = "助手会话", businessType = BusinessType.INSERT)
    @PostMapping("/sessions")
    public AjaxResult createSession(@RequestBody(required = false) KbAssistantSession session)
    {
        KbAssistantSession body = session == null ? new KbAssistantSession() : session;
        return success(assistantService.createSession(body.getContextType(), body.getContextId(), body.getTitle()));
    }

    /**
     * 删除会话
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @Log(title = "助手会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/sessions/{id}")
    public AjaxResult deleteSession(@PathVariable Long id)
    {
        return toAjax(assistantService.deleteSession(id));
    }

    /**
     * 会话消息
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @GetMapping("/sessions/{id}/messages")
    public AjaxResult messages(@PathVariable Long id)
    {
        List<KbAssistantMessage> list = assistantService.selectMessages(id);
        return success(list);
    }

    /**
     * 流式问答
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @Log(title = "智能助手问答", businessType = BusinessType.OTHER)
    @PostMapping(value = "/ask", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter ask(@RequestBody KbAskRequest request)
    {
        SseEmitter emitter = new SseEmitter(180000L);
        assistantService.ask(request, emitter);
        return emitter;
    }

    /**
     * 助手健康检查
     */
    @PreAuthorize("@ss.hasPermi('knowledge:assistant:ask')")
    @GetMapping("/health")
    public AjaxResult health()
    {
        return success(assistantService.health());
    }
}

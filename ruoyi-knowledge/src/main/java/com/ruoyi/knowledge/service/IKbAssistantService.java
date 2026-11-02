package com.ruoyi.knowledge.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.knowledge.domain.KbAssistantMessage;
import com.ruoyi.knowledge.domain.KbAssistantSession;
import com.ruoyi.knowledge.domain.dto.KbAskRequest;

/**
 * 智能助手服务
 *
 * @author ruoyi
 */
public interface IKbAssistantService
{
    /**
     * 当前用户的会话列表
     *
     * @return 会话集合
     */
    List<KbAssistantSession> selectSessions();

    /**
     * 创建会话
     *
     * @param contextType 上下文类型
     * @param contextId 上下文ID
     * @param title 标题
     * @return 会话
     */
    KbAssistantSession createSession(String contextType, Long contextId, String title);

    /**
     * 会话消息
     *
     * @param sessionId 会话ID
     * @return 消息集合
     */
    List<KbAssistantMessage> selectMessages(Long sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteSession(Long sessionId);

    /**
     * 流式问答
     *
     * @param request 请求
     * @param emitter SSE 发射器
     */
    void ask(KbAskRequest request, SseEmitter emitter);

    /**
     * 助手健康检查
     *
     * @return 状态信息
     */
    Map<String, Object> health();
}

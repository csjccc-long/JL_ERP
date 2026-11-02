package com.ruoyi.knowledge.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.service.IBusinessObjectService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.config.RagFlowProperties;
import com.ruoyi.knowledge.constant.KbConstants;
import com.ruoyi.knowledge.domain.KbAnswerCitation;
import com.ruoyi.knowledge.domain.KbAssistantMessage;
import com.ruoyi.knowledge.domain.KbAssistantSession;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbQueryAudit;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.domain.dto.KbAskRequest;
import com.ruoyi.knowledge.mapper.KbAnswerCitationMapper;
import com.ruoyi.knowledge.mapper.KbAssistantMessageMapper;
import com.ruoyi.knowledge.mapper.KbAssistantSessionMapper;
import com.ruoyi.knowledge.mapper.KbDocumentMapper;
import com.ruoyi.knowledge.mapper.KbSpaceMapper;
import com.ruoyi.knowledge.ragflow.ChatStreamHandler;
import com.ruoyi.knowledge.ragflow.RagFlowClient;
import com.ruoyi.knowledge.ragflow.RagFlowException;
import com.ruoyi.knowledge.service.IKbAssistantService;
import com.ruoyi.knowledge.service.IKbAuditService;
import com.ruoyi.knowledge.service.IKbPermissionService;

/**
 * 智能助手服务实现
 *
 * <p>问答流程：校验业务上下文可见性 → 计算可检索文档集合（空间 ACL ∩ 业务可见性 ∩ READY 当前版本）
 * → 调用 RAGFlow 流式问答 → 过滤引用 → 保存消息、引用和审计。</p>
 *
 * @author ruoyi
 */
@Service
public class KbAssistantServiceImpl implements IKbAssistantService
{
    private static final Logger log = LoggerFactory.getLogger(KbAssistantServiceImpl.class);

    private static final long SSE_TIMEOUT = 180000L;

    /** 按数据集集合自动创建的专用助手名前缀 */
    private static final String CHAT_PREFIX = "ERP-AUTO-";

    /** 专用助手的系统提示词：限制仅依据知识库作答，并统一中文兜底口径 */
    private static final String CHAT_SYSTEM_PROMPT = "你是企业知识库助手。请仅依据以下知识库内容回答问题，使用中文回答，并尽量引用文档中的原文信息。"
            + "若知识库内容与问题无关，必须回答：知识库中未找到相关内容，请确认文档已上传并完成索引。\n\n知识库内容：\n{knowledge}";

    /** 专用助手的空回答文案 */
    private static final String CHAT_EMPTY_RESPONSE = "知识库中未找到相关内容，请确认文档已上传并完成索引。";

    /** 数据集集合（排序后逗号拼接）到 RAGFlow 助手 ID 的缓存，避免并发请求互相改写同一个助手 */
    private final Map<String, String> chatCache = new ConcurrentHashMap<String, String>();

    /** 首次使用时从 RAGFlow 恢复已创建的专用助手 */
    private volatile boolean chatCacheLoaded = false;

    private final Object chatLock = new Object();

    @Autowired
    private KbAssistantSessionMapper sessionMapper;

    @Autowired
    private KbAssistantMessageMapper messageMapper;

    @Autowired
    private KbAnswerCitationMapper citationMapper;

    @Autowired
    private KbDocumentMapper documentMapper;

    @Autowired
    private KbSpaceMapper spaceMapper;

    @Autowired
    private RagFlowClient ragFlowClient;

    @Autowired
    private RagFlowProperties properties;

    @Autowired
    private IKbPermissionService permissionService;

    @Autowired
    private IKbAuditService auditService;

    @Autowired(required = false)
    private List<IBusinessObjectService> businessObjectServices;

    @Autowired
    @Qualifier("kbAskExecutor")
    private ExecutorService askExecutor;

    @Override
    public List<KbAssistantSession> selectSessions()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        KbAssistantSession query = new KbAssistantSession();
        query.setUserId(loginUser.getUserId());
        query.setSessionStatus(KbConstants.SESSION_ACTIVE);
        return sessionMapper.selectList(query);
    }

    @Override
    public KbAssistantSession createSession(String contextType, Long contextId, String title)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        KbAssistantSession session = new KbAssistantSession();
        session.setUserId(loginUser.getUserId());
        session.setTitle(StringUtils.isEmpty(title) ? "新会话" : title);
        session.setContextType(contextType);
        session.setContextId(contextId);
        session.setSessionStatus(KbConstants.SESSION_ACTIVE);
        session.setCreateBy(SecurityUtils.getUsername());
        session.setCreateTime(new Date());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public List<KbAssistantMessage> selectMessages(Long sessionId)
    {
        requireOwnSession(sessionId);
        KbAssistantMessage query = new KbAssistantMessage();
        query.setSessionId(sessionId);
        List<KbAssistantMessage> messages = messageMapper.selectList(query);
        List<KbAssistantMessage> ordered = new ArrayList<KbAssistantMessage>();
        if (messages != null)
        {
            for (int i = messages.size() - 1; i >= 0; i--)
            {
                ordered.add(messages.get(i));
            }
        }
        return ordered;
    }

    @Override
    public int deleteSession(Long sessionId)
    {
        requireOwnSession(sessionId);
        KbAssistantSession update = new KbAssistantSession();
        update.setId(sessionId);
        update.setSessionStatus("CLOSED");
        return sessionMapper.update(update);
    }

    private KbAssistantSession requireOwnSession(Long sessionId)
    {
        KbAssistantSession session = sessionMapper.selectById(sessionId);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (session == null || !loginUser.getUserId().equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问", 403);
        }
        return session;
    }

    @Override
    public void ask(KbAskRequest request, SseEmitter emitter)
    {
        final LoginUser loginUser = SecurityUtils.getLoginUser();
        askExecutor.execute(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null,
                    loginUser.getAuthorities()));
            SecurityContextHolder.setContext(context);
            try
            {
                doAsk(request, emitter);
            }
            catch (ServiceException e)
            {
                sendError(emitter, "", "FAILED", e.getMessage());
                emitter.complete();
            }
            catch (RuntimeException e)
            {
                log.error("智能助手问答异常", e);
                sendError(emitter, "", "FAILED", "服务处理失败，请稍后重试");
                emitter.complete();
            }
            finally
            {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private void doAsk(KbAskRequest request, SseEmitter emitter)
    {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long begin = System.currentTimeMillis();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        KbQueryAudit audit = new KbQueryAudit();
        audit.setTraceId(traceId);
        audit.setUserId(loginUser == null ? null : loginUser.getUserId());
        audit.setDeptId(loginUser == null ? null : loginUser.getDeptId());
        audit.setQuestion(question);
        audit.setContextType(request.getContextType());
        audit.setContextId(request.getContextId());

        if (StringUtils.isEmpty(question))
        {
            sendError(emitter, traceId, KbConstants.AUDIT_FAILED, "问题不能为空");
            audit.setStatus(KbConstants.AUDIT_FAILED);
            audit.setErrorMessage("问题不能为空");
            audit.setLatencyMs((int) (System.currentTimeMillis() - begin));
            auditService.record(audit);
            emitter.complete();
            return;
        }

        // 1. 校验业务上下文可见性
        String contextBlock = null;
        if (StringUtils.isNotEmpty(request.getContextType()) && !"POLICY".equals(request.getContextType()))
        {
            Map<String, Object> summary = loadBusinessSummary(request.getContextType(), request.getContextId());
            if (summary == null)
            {
                sendError(emitter, traceId, KbConstants.AUDIT_FORBIDDEN, "无权访问当前业务对象");
                audit.setStatus(KbConstants.AUDIT_FORBIDDEN);
                audit.setErrorMessage("业务上下文不可见");
                audit.setLatencyMs((int) (System.currentTimeMillis() - begin));
                auditService.record(audit);
                emitter.complete();
                return;
            }
            contextBlock = buildContextBlock(request.getContextType(), request.getContextId(), summary);
        }

        // 2. 计算可检索文档集合
        List<KbDocument> documents = permissionService.retrievableDocuments();
        List<String> ragDocumentIds = new ArrayList<String>();
        Set<Long> spaceIdSet = new HashSet<Long>();
        List<Long> documentIds = new ArrayList<Long>();
        for (KbDocument document : documents)
        {
            if (request.getSpaceId() != null && !request.getSpaceId().equals(document.getSpaceId()))
            {
                continue;
            }
            ragDocumentIds.add(document.getRagDocumentId());
            spaceIdSet.add(document.getSpaceId());
            documentIds.add(document.getId());
        }
        audit.setSpaceIdsJson(JSON.toJSONString(spaceIdSet));
        audit.setDocumentIdsJson(JSON.toJSONString(documentIds));

        // 3. 会话
        KbAssistantSession session;
        boolean newSession = false;
        if (request.getSessionId() == null)
        {
            session = createSession(request.getContextType(), request.getContextId(),
                    question.length() > KbConstants.SESSION_TITLE_LENGTH
                            ? question.substring(0, KbConstants.SESSION_TITLE_LENGTH) : question);
            newSession = true;
        }
        else
        {
            session = requireOwnSession(request.getSessionId());
        }
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("traceId", traceId);
        meta.put("sessionId", session.getId());
        meta.put("newSession", newSession);
        send(emitter, "meta", JSON.toJSONString(meta));

        saveMessage(session.getId(), "USER", question, null, null, traceId);

        if (ragDocumentIds.isEmpty() || spaceIdSet.isEmpty())
        {
            String message = "当前没有可检索的文档，请先上传并完成索引";
            sendDelta(emitter, message);
            saveAssistantMessage(session.getId(), message, null, traceId, begin);
            sendDone(emitter, KbConstants.AUDIT_INSUFFICIENT, begin);
            audit.setStatus(KbConstants.AUDIT_INSUFFICIENT);
            audit.setErrorMessage("无可检索文档");
            audit.setLatencyMs((int) (System.currentTimeMillis() - begin));
            auditService.record(audit);
            emitter.complete();
            return;
        }

        List<String> datasetIds = new ArrayList<String>();
        for (Long spaceId : spaceIdSet)
        {
            KbSpace space = spaceMapper.selectById(spaceId);
            if (space != null && StringUtils.isNotEmpty(space.getRagDatasetId())
                    && !datasetIds.contains(space.getRagDatasetId()))
            {
                datasetIds.add(space.getRagDatasetId());
            }
        }

        // 4. 解析专用助手：每个数据集集合使用独立助手，避免并发请求互相改写助手数据集范围
        String chatId = resolveChatId(datasetIds, traceId);

        // 5. 组装请求
        JSONObject body = new JSONObject();
        body.put("chat_id", chatId);
        body.put("stream", true);
        body.put("question", buildQuestion(question, contextBlock, session.getId()));
        body.put("doc_ids", String.join(",", ragDocumentIds));
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", body.getString("question"));
        messages.add(userMessage);
        body.put("messages", messages);

        List<KbAnswerCitation> citations = new ArrayList<KbAnswerCitation>();
        List<Map<String, Object>> citationPayloads = new ArrayList<Map<String, Object>>();
        StringBuilder answer = new StringBuilder();
        int attempt = 0;
        while (true)
        {
            try
            {
                ragFlowClient.streamChat(body, new ChatStreamHandler()
                {
                    @Override
                    public void onDelta(String delta)
                    {
                        answer.append(delta);
                        sendDelta(emitter, delta);
                    }

                    @Override
                    public void onReference(JSONArray chunks)
                    {
                        try
                        {
                            collectCitations(chunks, ragDocumentIds, documentIds, citations, citationPayloads);
                        }
                        catch (RuntimeException e)
                        {
                            log.warn("引用解析失败 traceId={}: {}", traceId, e.getMessage());
                        }
                    }

                    @Override
                    public void onDone()
                    {
                        // 由外层统一收尾
                    }
                });
                break;
            }
            catch (RagFlowException e)
            {
                // 未产生任何增量内容时重试一次，覆盖 RAGFlow 偶发的服务端连接错误
                if (answer.length() == 0 && attempt < 1)
                {
                    attempt++;
                    citations.clear();
                    citationPayloads.clear();
                    log.warn("RAGFlow 问答失败，重试第 {} 次 traceId={}: {}", attempt, traceId, e.getMessage());
                    continue;
                }
                log.warn("RAGFlow 问答失败 traceId={}: {}", traceId, e.getMessage());
                String text = answer.length() == 0 ? "知识服务暂不可用，请稍后重试。ERP 业务操作不受影响。" : answer.toString();
                if (answer.length() == 0)
                {
                    sendDelta(emitter, text);
                }
                sendError(emitter, traceId, KbConstants.AUDIT_UNAVAILABLE, "知识服务暂不可用");
                saveAssistantMessage(session.getId(), text, null, traceId, begin);
                audit.setStatus(KbConstants.AUDIT_UNAVAILABLE);
                audit.setErrorMessage("RAGFlow 调用失败");
                audit.setLatencyMs((int) (System.currentTimeMillis() - begin));
                auditService.record(audit);
                emitter.complete();
                return;
            }
        }

        String content = answer.toString();
        boolean noEvidence = isEmptyResponse(content);
        if (noEvidence)
        {
            // 模型未给出有效答案，检索到的分块不构成引用证据
            citations.clear();
        }
        else
        {
            // 引用延迟到答案确定有效后再下发，避免空答案仍展示引用
            for (Map<String, Object> payload : citationPayloads)
            {
                send(emitter, "citation", JSON.toJSONString(payload));
            }
        }
        Long messageId = saveAssistantMessage(session.getId(), content, null, traceId, begin);
        for (KbAnswerCitation citation : citations)
        {
            citation.setMessageId(messageId);
            citationMapper.insert(citation);
        }
        String status = StringUtils.isEmpty(content) || noEvidence
                ? KbConstants.AUDIT_INSUFFICIENT : KbConstants.AUDIT_SUCCESS;
        if (noEvidence)
        {
            audit.setErrorMessage("无匹配文档");
        }
        sendDone(emitter, status, begin);
        audit.setStatus(status);
        audit.setModelName(null);
        audit.setLatencyMs((int) (System.currentTimeMillis() - begin));
        auditService.record(audit);
        emitter.complete();
    }

    private Map<String, Object> loadBusinessSummary(String contextType, Long contextId)
    {
        if (contextId == null || businessObjectServices == null)
        {
            return null;
        }
        for (IBusinessObjectService service : businessObjectServices)
        {
            if (service.supports(contextType))
            {
                return service.loadSummary(contextType, contextId);
            }
        }
        return null;
    }

    private String buildContextBlock(String contextType, Long contextId, Map<String, Object> summary)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("<ERP_CONTEXT source=\"").append(contextType).append("\" id=\"").append(contextId)
                .append("\">\n");
        builder.append(JSON.toJSONString(summary)).append("\n");
        builder.append("</ERP_CONTEXT>");
        return builder.toString();
    }

    /**
     * 解析（必要时创建）与数据集集合绑定的专用 RAGFlow 助手。
     *
     * <p>RAGFlow 的问答接口只会在助手绑定的数据集内检索，而助手的数据集范围是可变的。
     * 若所有请求共用一个全局助手，并发问答会互相覆盖数据集范围，导致检索到错误空间。
     * 这里按“排序后的数据集集合”派生独立助手，并用内存缓存复用；首次调用时从 RAGFlow
     * 的助手列表恢复，ERP 重启后可直接复用已创建的助手。</p>
     *
     * @param datasetIds 本次问答涉及的数据集 ID
     * @param traceId 链路标识
     * @return 助手 ID；无法创建时回退到配置的全局助手
     */
    private String resolveChatId(List<String> datasetIds, String traceId)
    {
        List<String> validDatasetIds = new ArrayList<String>();
        for (String datasetId : datasetIds)
        {
            try
            {
                if (ragFlowClient.datasetExists(datasetId))
                {
                    validDatasetIds.add(datasetId);
                }
                else
                {
                    log.warn("RAGFlow 数据集不存在，已跳过 traceId={}: {}", traceId, datasetId);
                }
            }
            catch (RuntimeException e)
            {
                log.warn("校验 RAGFlow 数据集失败 traceId={} datasetId={}: {}", traceId, datasetId, e.getMessage());
            }
        }
        if (validDatasetIds.isEmpty())
        {
            return properties.getChatId();
        }
        Collections.sort(validDatasetIds);
        String key = String.join(",", validDatasetIds);
        String cached = chatCache.get(key);
        if (StringUtils.isNotEmpty(cached))
        {
            return cached;
        }
        synchronized (chatLock)
        {
            cached = chatCache.get(key);
            if (StringUtils.isNotEmpty(cached))
            {
                return cached;
            }
            if (!chatCacheLoaded)
            {
                loadExistingChats(traceId);
                chatCacheLoaded = true;
                cached = chatCache.get(key);
                if (StringUtils.isNotEmpty(cached))
                {
                    return cached;
                }
            }
            String chatId = ragFlowClient.createChat(CHAT_PREFIX + Integer.toHexString(key.hashCode()),
                    validDatasetIds, CHAT_SYSTEM_PROMPT, CHAT_EMPTY_RESPONSE);
            if (StringUtils.isNotEmpty(chatId))
            {
                chatCache.put(key, chatId);
                log.info("已创建专用 RAGFlow 助手 traceId={} chatId={} datasets={}", traceId, chatId, validDatasetIds);
                return chatId;
            }
        }
        log.warn("创建专用 RAGFlow 助手失败，回退全局助手 traceId={}", traceId);
        return properties.getChatId();
    }

    /**
     * 从 RAGFlow 助手列表恢复本服务创建过的专用助手缓存。
     *
     * @param traceId 链路标识
     */
    private void loadExistingChats(String traceId)
    {
        try
        {
            JSONArray chats = ragFlowClient.listChats();
            if (chats == null)
            {
                return;
            }
            for (int i = 0; i < chats.size(); i++)
            {
                JSONObject chat = chats.getJSONObject(i);
                if (chat == null || !StringUtils.startsWith(chat.getString("name"), CHAT_PREFIX))
                {
                    continue;
                }
                JSONArray datasets = chat.getJSONArray("dataset_ids");
                if (datasets == null || datasets.isEmpty())
                {
                    continue;
                }
                List<String> datasetIds = new ArrayList<String>();
                for (int j = 0; j < datasets.size(); j++)
                {
                    datasetIds.add(datasets.getString(j));
                }
                Collections.sort(datasetIds);
                chatCache.putIfAbsent(String.join(",", datasetIds), chat.getString("id"));
            }
            log.info("已恢复 {} 个专用 RAGFlow 助手 traceId={}", chatCache.size(), traceId);
        }
        catch (RuntimeException e)
        {
            log.warn("恢复专用 RAGFlow 助手失败 traceId={}: {}", traceId, e.getMessage());
        }
    }

    private String buildQuestion(String question, String contextBlock, Long sessionId)
    {
        StringBuilder builder = new StringBuilder();
        List<KbAssistantMessage> history = loadHistory(sessionId);
        if (!history.isEmpty())
        {
            builder.append("【历史对话】\n");
            for (KbAssistantMessage message : history)
            {
                String role = "USER".equals(message.getRole()) ? "用户" : "助手";
                String content = message.getContent();
                if (content != null && content.length() > 500)
                {
                    content = content.substring(0, 500) + "...";
                }
                builder.append(role).append("：").append(content).append("\n");
            }
            builder.append("\n");
        }
        if (contextBlock != null)
        {
            builder.append(contextBlock).append("\n\n");
        }
        builder.append("【当前问题】\n").append(question);
        // 不在问题正文追加长段约束语：RAGFlow 助手提示词已约束“仅依据知识库”，
        // 追加长句会被小参数模型误判为“依据不足”而直接返回空回答。
        return builder.toString();
    }

    private List<KbAssistantMessage> loadHistory(Long sessionId)
    {
        KbAssistantMessage query = new KbAssistantMessage();
        query.setSessionId(sessionId);
        List<KbAssistantMessage> all = messageMapper.selectList(query);
        List<KbAssistantMessage> history = new ArrayList<KbAssistantMessage>();
        if (all == null || all.isEmpty())
        {
            return history;
        }
        int limit = KbConstants.HISTORY_ROUNDS * 2;
        int count = Math.min(limit, all.size());
        for (int i = count - 1; i >= 0; i--)
        {
            history.add(all.get(i));
        }
        return history;
    }

    private void collectCitations(JSONArray chunks, List<String> allowedRagIds, List<Long> allowedErpIds,
            List<KbAnswerCitation> citations, List<Map<String, Object>> payloads)
    {
        for (int i = 0; i < chunks.size(); i++)
        {
            JSONObject chunk = chunks.getJSONObject(i);
            if (chunk == null)
            {
                continue;
            }
            String ragDocumentId = chunk.getString("document_id");
            if (StringUtils.isEmpty(ragDocumentId) || !allowedRagIds.contains(ragDocumentId))
            {
                continue;
            }
            KbDocument document = documentMapper.selectByRagDocumentId(ragDocumentId);
            if (document == null || !allowedErpIds.contains(document.getId()))
            {
                continue;
            }
            Integer pageNumber = firstPageNumber(chunk);
            boolean duplicated = false;
            for (KbAnswerCitation existing : citations)
            {
                if (ragDocumentId.equals(existing.getRagDocumentId())
                        && (pageNumber == null ? existing.getPageNumber() == null : pageNumber.equals(existing.getPageNumber())))
                {
                    duplicated = true;
                    break;
                }
            }
            if (duplicated)
            {
                continue;
            }
            KbAnswerCitation citation = new KbAnswerCitation();
            citation.setSourceType(KbConstants.CITATION_DOCUMENT);
            citation.setSourceId(document.getId());
            citation.setRagChunkId(chunk.getString("id"));
            citation.setRagDocumentId(ragDocumentId);
            citation.setPageNumber(pageNumber);
            String content = chunk.getString("content");
            citation.setSnippet(content != null && content.length() > 500 ? content.substring(0, 500) : content);
            BigDecimal similarity = chunk.getBigDecimal("similarity");
            citation.setSimilarityScore(similarity);
            citation.setCitationOrder(citations.size() + 1);
            citations.add(citation);

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("sourceType", KbConstants.CITATION_DOCUMENT);
            payload.put("documentId", document.getId());
            payload.put("documentName", document.getDocumentName());
            payload.put("pageNumber", pageNumber);
            payload.put("snippet", citation.getSnippet());
            payload.put("order", citation.getCitationOrder());
            payloads.add(payload);
        }
    }

    private boolean isEmptyResponse(String content)
    {
        if (StringUtils.isEmpty(content))
        {
            return true;
        }
        String text = content.trim();
        return text.contains("No relevant content was found in the knowledge base")
                || text.contains("The answer you are looking for is not found in the dataset")
                || text.contains("未找到相关内容")
                || text.contains("知识库中未找到");
    }

    private Integer firstPageNumber(JSONObject chunk)
    {
        Integer page = chunk.getInteger("page_number");
        if (page != null)
        {
            return page;
        }
        JSONArray positions = chunk.getJSONArray("positions");
        if (positions != null && !positions.isEmpty())
        {
            Object first = positions.get(0);
            if (first instanceof JSONArray)
            {
                // RAGFlow 原生格式：[[页码, x0, x1, top, bottom], ...]
                JSONArray position = (JSONArray) first;
                if (!position.isEmpty() && position.getInteger(0) != null)
                {
                    return position.getInteger(0);
                }
            }
            else if (first instanceof JSONObject)
            {
                JSONObject position = (JSONObject) first;
                JSONArray pages = position.getJSONArray("page_numbers");
                if (pages != null && !pages.isEmpty())
                {
                    return pages.getInteger(0);
                }
                Integer pageNo = position.getInteger("page");
                if (pageNo != null)
                {
                    return pageNo;
                }
            }
        }
        return null;
    }

    private Long saveAssistantMessage(Long sessionId, String content, String modelName, String traceId, long begin)
    {
        KbAssistantMessage message = new KbAssistantMessage();
        message.setSessionId(sessionId);
        message.setRole("ASSISTANT");
        message.setContent(content);
        message.setModelName(modelName);
        message.setTraceId(traceId);
        message.setLatencyMs((int) (System.currentTimeMillis() - begin));
        message.setCreatedTime(new Date());
        messageMapper.insert(message);
        return message.getId();
    }

    private void saveMessage(Long sessionId, String role, String content, String modelName, Integer latencyMs,
            String traceId)
    {
        KbAssistantMessage message = new KbAssistantMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setModelName(modelName);
        message.setLatencyMs(latencyMs);
        message.setTraceId(traceId);
        message.setCreatedTime(new Date());
        messageMapper.insert(message);
    }

    private void send(SseEmitter emitter, String event, String data)
    {
        try
        {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.TEXT_PLAIN));
        }
        catch (IOException e)
        {
            log.debug("SSE 连接已关闭: {}", e.getMessage());
        }
    }

    private void sendDelta(SseEmitter emitter, String delta)
    {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("content", delta);
        send(emitter, "delta", JSON.toJSONString(payload));
    }

    private void sendDone(SseEmitter emitter, String status, long begin)
    {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("status", status);
        payload.put("latencyMs", (int) (System.currentTimeMillis() - begin));
        send(emitter, "done", JSON.toJSONString(payload));
    }

    private void sendError(SseEmitter emitter, String traceId, String code, String message)
    {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", code);
        payload.put("message", message);
        payload.put("traceId", traceId);
        send(emitter, "error", JSON.toJSONString(payload));
    }

    @Override
    public Map<String, Object> health()
    {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("ragflowConfigured", ragFlowClient.isConfigured());
        result.put("ragflowAvailable", ragFlowClient.isConfigured() && ragFlowClient.health());
        result.put("chatConfigured", StringUtils.isNotEmpty(properties.getChatId()) || !chatCache.isEmpty());
        result.put("retrievableDocuments", permissionService.retrievableDocuments().size());
        return result;
    }
}

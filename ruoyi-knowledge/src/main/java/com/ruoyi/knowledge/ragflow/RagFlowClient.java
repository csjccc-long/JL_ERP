package com.ruoyi.knowledge.ragflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.config.RagFlowProperties;

/**
 * RAGFlow REST 客户端
 *
 * <p>负责数据集、文档上传与解析、检索和流式问答。所有请求仅在服务端执行，
 * API Key 不进入前端；连接超时 5 秒、读取超时 90 秒，失败按指数退避重试。</p>
 *
 * @author ruoyi
 */
@Component
public class RagFlowClient
{
    private static final Logger log = LoggerFactory.getLogger(RagFlowClient.class);

    private final RagFlowProperties properties;

    private final HttpClient httpClient;

    public RagFlowClient(RagFlowProperties properties)
    {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }

    /**
     * 服务是否已配置
     *
     * @return 已配置返回 true
     */
    public boolean isConfigured()
    {
        return StringUtils.isNotEmpty(properties.getBaseUrl());
    }

    /**
     * 健康检查，不触发模型生成
     *
     * @return 可用返回 true
     */
    public boolean health()
    {
        try
        {
            getJson("/api/v1/datasets?page=1&page_size=1");
            return true;
        }
        catch (RuntimeException e)
        {
            log.warn("RAGFlow 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建数据集
     *
     * @param name 数据集名称
     * @param description 描述
     * @return RAGFlow 数据集ID
     */
    public String createDataset(String name, String description)
    {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("description", description);
        body.put("chunk_method", properties.getChunkMethod());
        JSONObject data = postJson("/api/v1/datasets", body);
        if (data == null)
        {
            throw new RagFlowException("创建数据集未返回结果");
        }
        if (data.containsKey("id"))
        {
            return data.getString("id");
        }
        JSONArray items = data.getJSONArray("data");
        if (items != null && !items.isEmpty())
        {
            return items.getJSONObject(0).getString("id");
        }
        throw new RagFlowException("创建数据集未返回数据集ID");
    }

    /**
     * 删除数据集
     *
     * @param datasetId 数据集ID
     */
    public void deleteDatasets(List<String> datasetIds)
    {
        if (datasetIds == null || datasetIds.isEmpty())
        {
            return;
        }
        JSONObject body = new JSONObject();
        body.put("ids", datasetIds);
        deleteJson("/api/v1/datasets", body);
    }

    /**
     * 上传文档到数据集
     *
     * @param datasetId 数据集ID
     * @param file 本地文件
     * @param fileName 展示名称
     * @return RAGFlow 文档ID
     */
    public String uploadDocument(String datasetId, Path file, String fileName)
    {
        String boundary = "----erp" + UUID.randomUUID().toString().replace("-", "");
        byte[] payload;
        try
        {
            payload = buildMultipart(boundary, file, fileName);
        }
        catch (IOException e)
        {
            throw new RagFlowException("读取上传文件失败", e);
        }
        HttpRequest request = builder("/api/v1/datasets/" + datasetId + "/documents")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        JSONObject response = executeRaw(request);
        Object raw = response.get("data");
        if (raw instanceof JSONArray)
        {
            JSONArray array = (JSONArray) raw;
            if (array.isEmpty())
            {
                throw new RagFlowException("上传文档未返回文档ID");
            }
            return array.getJSONObject(0).getString("id");
        }
        if (raw instanceof JSONObject)
        {
            JSONObject object = (JSONObject) raw;
            if (object.containsKey("id"))
            {
                return object.getString("id");
            }
            JSONArray array = object.getJSONArray("data");
            if (array != null && !array.isEmpty())
            {
                return array.getJSONObject(0).getString("id");
            }
            throw new RagFlowException("上传文档未返回文档ID");
        }
        if (response.containsKey("id"))
        {
            return response.getString("id");
        }
        throw new RagFlowException("上传文档未返回文档ID");
    }

    private byte[] buildMultipart(String boundary, Path file, String fileName) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(Files.readAllBytes(file));
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /**
     * 触发文档解析
     *
     * @param datasetId 数据集ID
     * @param documentIds RAGFlow 文档ID集合
     */
    public void parseDocuments(String datasetId, List<String> documentIds)
    {
        if (documentIds == null || documentIds.isEmpty())
        {
            return;
        }
        JSONObject body = new JSONObject();
        body.put("document_ids", documentIds);
        postJson("/api/v1/datasets/" + datasetId + "/documents/parse", body);
    }

    /**
     * 查询文档解析状态
     *
     * @param datasetId 数据集ID
     * @param documentId RAGFlow 文档ID
     * @return 状态字符串（UNSTART/RUNNING/DONE/FAIL/CANCEL）
     */
    public String getDocumentStatus(String datasetId, String documentId)
    {
        JSONObject data = getJson("/api/v1/datasets/" + datasetId + "/documents?id=" + urlEncode(documentId)
                + "&page=1&page_size=10");
        JSONArray docs = extractDocs(data);
        if (docs == null || docs.isEmpty())
        {
            return null;
        }
        for (int i = 0; i < docs.size(); i++)
        {
            JSONObject doc = docs.getJSONObject(i);
            if (documentId.equals(doc.getString("id")))
            {
                String run = doc.getString("run");
                if (StringUtils.isNotEmpty(run))
                {
                    return run;
                }
                return doc.getString("status");
            }
        }
        return null;
    }

    /**
     * 查询文档统计信息（分块数量）
     *
     * @param datasetId 数据集ID
     * @param documentId RAGFlow 文档ID
     * @return 分块数量，未知返回 0
     */
    public int getChunkCount(String datasetId, String documentId)
    {
        JSONObject data = getJson("/api/v1/datasets/" + datasetId + "/documents?id=" + urlEncode(documentId)
                + "&page=1&page_size=10");
        JSONArray docs = extractDocs(data);
        if (docs == null)
        {
            return 0;
        }
        for (int i = 0; i < docs.size(); i++)
        {
            JSONObject doc = docs.getJSONObject(i);
            if (documentId.equals(doc.getString("id")))
            {
                Integer chunkCount = doc.getInteger("chunk_count");
                return chunkCount == null ? 0 : chunkCount;
            }
        }
        return 0;
    }

    private JSONArray extractDocs(JSONObject data)
    {
        if (data == null)
        {
            return null;
        }
        Object raw = data.get("data");
        if (raw instanceof JSONObject)
        {
            return ((JSONObject) raw).getJSONArray("docs");
        }
        if (data.containsKey("docs"))
        {
            return data.getJSONArray("docs");
        }
        return data.getJSONArray("data");
    }

    /**
     * 删除文档
     *
     * @param datasetId 数据集ID
     * @param documentIds RAGFlow 文档ID集合
     */
    public void deleteDocuments(String datasetId, List<String> documentIds)
    {
        if (documentIds == null || documentIds.isEmpty())
        {
            return;
        }
        JSONObject body = new JSONObject();
        body.put("ids", documentIds);
        deleteJson("/api/v1/datasets/" + datasetId + "/documents", body);
    }

    /**
     * 检索验证
     *
     * @param datasetIds 数据集ID集合
     * @param documentIds 文档ID集合
     * @param question 问题
     * @return 命中分块
     */
    public JSONArray retrieval(List<String> datasetIds, List<String> documentIds, String question)
    {
        JSONObject body = new JSONObject();
        body.put("dataset_ids", datasetIds);
        body.put("document_ids", documentIds);
        body.put("question", question);
        body.put("page_size", properties.getPageSize());
        body.put("similarity_threshold", properties.getSimilarityThreshold());
        body.put("vector_similarity_weight", properties.getVectorSimilarityWeight());
        JSONObject data = postJson("/api/v1/retrieval", body);
        if (data == null)
        {
            return new JSONArray();
        }
        JSONArray chunks = data.getJSONArray("chunks");
        return chunks == null ? new JSONArray() : chunks;
    }

    /**
     * 流式问答
     *
     * @param body 请求体
     * @param handler 流式回调
     */
    public void streamChat(JSONObject body, ChatStreamHandler handler)
    {
        HttpRequest request = builder("/api/v1/chat/completions")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(Math.max(10, properties.getReadTimeoutSeconds())))
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        try
        {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200)
            {
                throw new RagFlowException("RAGFlow 服务返回状态码 " + response.statusCode());
            }
            try (Stream<String> lines = response.body())
            {
                Iterator<String> iterator = lines.iterator();
                while (iterator.hasNext())
                {
                    String line = iterator.next();
                    if (line == null || line.isEmpty() || !line.startsWith("data:"))
                    {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty())
                    {
                        continue;
                    }
                    if ("true".equals(payload) || "[DONE]".equals(payload))
                    {
                        handler.onDone();
                        return;
                    }
                    JSONObject chunk;
                    try
                    {
                        chunk = JSON.parseObject(payload);
                    }
                    catch (RuntimeException parseError)
                    {
                        continue;
                    }
                    Integer code = chunk.getInteger("code");
                    if (code != null && code != 0)
                    {
                        throw new RagFlowException(StringUtils.nvl(chunk.getString("message"), "RAGFlow 返回错误"));
                    }
                    JSONObject data = chunk.getJSONObject("data");
                    if (data == null)
                    {
                        continue;
                    }
                    String answer = data.getString("answer");
                    if (StringUtils.isNotEmpty(answer))
                    {
                        handler.onDelta(answer);
                    }
                    JSONObject reference = data.getJSONObject("reference");
                    if (reference != null)
                    {
                        JSONArray chunks = reference.getJSONArray("chunks");
                        if (chunks != null && !chunks.isEmpty())
                        {
                            handler.onReference(chunks);
                        }
                    }
                    Boolean done = data.getBoolean("done");
                    if (Boolean.TRUE.equals(done))
                    {
                        handler.onDone();
                        return;
                    }
                }
            }
            handler.onDone();
        }
        catch (IOException e)
        {
            throw new RagFlowException("调用 RAGFlow 失败：" + e.getClass().getSimpleName(), e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RagFlowException("调用 RAGFlow 被中断", e);
        }
    }

    /**
     * 读取 RAGFlow 助手详情。
     *
     * @param chatId 助手 ID
     * @return 助手信息
     */
    public JSONObject getChat(String chatId)
    {
        HttpRequest request = builder("/api/v1/chats/" + chatId).GET().build();
        return execute(request, false);
    }

    /**
     * 判断 RAGFlow 数据集是否存在。
     *
     * @param datasetId 数据集 ID
     * @return 存在返回 true
     */
    public boolean datasetExists(String datasetId)
    {
        if (StringUtils.isEmpty(datasetId))
        {
            return false;
        }
        try
        {
            HttpRequest request = builder("/api/v1/datasets/" + datasetId).GET().build();
            execute(request, false);
            return true;
        }
        catch (RagFlowException e)
        {
            return false;
        }
    }

    /**
     * 创建绑定指定数据集集合的 RAGFlow 助手。
     *
     * @param name 助手名称
     * @param datasetIds 数据集 ID 集合
     * @param systemPrompt 系统提示词
     * @param emptyResponse 空回答文案
     * @return 助手 ID；创建失败返回 null
     */
    public String createChat(String name, List<String> datasetIds, String systemPrompt, String emptyResponse)
    {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("dataset_ids", datasetIds);
        JSONObject promptConfig = new JSONObject();
        promptConfig.put("system", systemPrompt);
        promptConfig.put("empty_response", emptyResponse);
        body.put("prompt_config", promptConfig);
        try
        {
            JSONObject data = postJson("/api/v1/chats", body);
            return data == null ? null : data.getString("id");
        }
        catch (RagFlowException e)
        {
            log.warn("创建 RAGFlow 助手失败 name={}: {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * 列出当前账号可见的 RAGFlow 助手。
     *
     * @return 助手数组；无数据返回空数组
     */
    public JSONArray listChats()
    {
        HttpRequest request = builder("/api/v1/chats?page=1&page_size=100")
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .GET()
                .build();
        JSONObject json = executeRaw(request);
        Object data = json.get("data");
        if (data instanceof JSONObject)
        {
            JSONArray chats = ((JSONObject) data).getJSONArray("chats");
            return chats == null ? new JSONArray() : chats;
        }
        return new JSONArray();
    }

    private HttpRequest.Builder builder(String path)
    {
        String base = StringUtils.nvl(properties.getBaseUrl(), "").trim();
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        return HttpRequest.newBuilder(URI.create(base + path))
                .header("Authorization", "Bearer " + StringUtils.nvl(properties.getApiKey(), ""))
                .header("Accept", "application/json");
    }

    private JSONObject getJson(String path)
    {
        HttpRequest request = builder(path)
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .GET()
                .build();
        return execute(request, false);
    }

    private JSONObject postJson(String path, JSONObject body)
    {
        HttpRequest request = builder(path)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        return execute(request, true);
    }

    private JSONObject deleteJson(String path, JSONObject body)
    {
        HttpRequest request = builder(path)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                .build();
        return execute(request, true);
    }

    /**
     * 执行请求并取出 data 节点（data 为数组时返回 null，由调用方使用 executeRaw）
     *
     * @param request 请求
     * @param requireData 是否要求返回 data
     * @return 响应中的 data 节点
     */
    private JSONObject execute(HttpRequest request, boolean requireData)
    {
        JSONObject json = executeRaw(request);
        JSONObject data = null;
        Object raw = json.get("data");
        if (raw instanceof JSONObject)
        {
            data = (JSONObject) raw;
        }
        if (requireData && data == null && !json.containsKey("data"))
        {
            throw new RagFlowException("RAGFlow 未返回数据");
        }
        return data;
    }

    /**
     * 执行请求，带指数退避重试，返回完整响应体
     *
     * @param request 请求
     * @return 完整响应 JSON
     */
    private JSONObject executeRaw(HttpRequest request)
    {
        int maxAttempt = Math.max(1, properties.getRetryMax());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempt; attempt++)
        {
            try
            {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 401 || response.statusCode() == 403)
                {
                    throw new RagFlowException("RAGFlow 鉴权失败，请检查服务端 API Key 配置");
                }
                if (response.statusCode() >= 500)
                {
                    throw new RagFlowException("RAGFlow 服务异常，状态码 " + response.statusCode());
                }
                JSONObject json = JSON.parseObject(response.body());
                if (json == null)
                {
                    throw new RagFlowException("RAGFlow 返回内容无法解析");
                }
                Integer code = json.getInteger("code");
                if (code != null && code != 0)
                {
                    throw new RagFlowException(StringUtils.nvl(json.getString("message"), "RAGFlow 返回业务错误"));
                }
                return json;
            }
            catch (RagFlowException e)
            {
                last = e;
            }
            catch (IOException e)
            {
                last = new RagFlowException("调用 RAGFlow 失败：" + e.getClass().getSimpleName(), e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RagFlowException("调用 RAGFlow 被中断", e);
            }
            if (attempt < maxAttempt)
            {
                try
                {
                    Thread.sleep(200L * (1L << (attempt - 1)));
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    throw new RagFlowException("调用 RAGFlow 被中断", ie);
                }
            }
        }
        throw last == null ? new RagFlowException("调用 RAGFlow 失败") : last;
    }

    private String urlEncode(String value)
    {
        try
        {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        catch (RuntimeException e)
        {
            return value;
        }
    }
}

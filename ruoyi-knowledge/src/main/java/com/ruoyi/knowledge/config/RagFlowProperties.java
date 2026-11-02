package com.ruoyi.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAGFlow 客户端配置
 *
 * <p>支持通过环境变量配置：RAGFLOW_BASE_URL、RAGFLOW_API_KEY、RAGFLOW_CHAT_ID、
 * RAGFLOW_TIMEOUT_SECONDS、RAGFLOW_RETRY_MAX。</p>
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ragflow")
public class RagFlowProperties
{
    /** 服务地址，例如 http://127.0.0.1:9380 */
    private String baseUrl = "http://127.0.0.1:9380";

    /** 服务端 API Key，禁止下发前端 */
    private String apiKey = "";

    /** 固定企业助手 Chat ID */
    private String chatId = "";

    /** 连接超时（秒） */
    private int connectTimeoutSeconds = 5;

    /** 读取超时（秒） */
    private int readTimeoutSeconds = 90;

    /** 最大重试次数 */
    private int retryMax = 3;

    /** 分块方法 */
    private String chunkMethod = "naive";

    /** 检索返回条数 */
    private int pageSize = 10;

    /** 相似度阈值 */
    private double similarityThreshold = 0.2;

    /** 向量相似度权重 */
    private double vectorSimilarityWeight = 0.3;

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getChatId()
    {
        return chatId;
    }

    public void setChatId(String chatId)
    {
        this.chatId = chatId;
    }

    public int getConnectTimeoutSeconds()
    {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds)
    {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds()
    {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds)
    {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public int getRetryMax()
    {
        return retryMax;
    }

    public void setRetryMax(int retryMax)
    {
        this.retryMax = retryMax;
    }

    public String getChunkMethod()
    {
        return chunkMethod;
    }

    public void setChunkMethod(String chunkMethod)
    {
        this.chunkMethod = chunkMethod;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }

    public double getSimilarityThreshold()
    {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold)
    {
        this.similarityThreshold = similarityThreshold;
    }

    public double getVectorSimilarityWeight()
    {
        return vectorSimilarityWeight;
    }

    public void setVectorSimilarityWeight(double vectorSimilarityWeight)
    {
        this.vectorSimilarityWeight = vectorSimilarityWeight;
    }
}

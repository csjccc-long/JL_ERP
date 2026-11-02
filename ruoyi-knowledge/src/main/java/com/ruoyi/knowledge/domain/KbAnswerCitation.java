package com.ruoyi.knowledge.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 回答引用对象 kb_answer_citation
 *
 * @author ruoyi
 */
public class KbAnswerCitation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 消息ID */
    private Long messageId;

    /** 来源类型 */
    private String sourceType;

    /** 来源ID */
    private Long sourceId;

    /** RAGFlow Chunk ID */
    private String ragChunkId;

    /** RAGFlow 文档ID */
    private String ragDocumentId;

    /** 页码 */
    private Integer pageNumber;

    /** 命中片段 */
    private String snippet;

    /** 相似度 */
    private BigDecimal similarityScore;

    /** 引用顺序 */
    private Integer citationOrder;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public Long getMessageId()
    {
        return messageId;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setRagChunkId(String ragChunkId)
    {
        this.ragChunkId = ragChunkId;
    }

    public String getRagChunkId()
    {
        return ragChunkId;
    }

    public void setRagDocumentId(String ragDocumentId)
    {
        this.ragDocumentId = ragDocumentId;
    }

    public String getRagDocumentId()
    {
        return ragDocumentId;
    }

    public void setPageNumber(Integer pageNumber)
    {
        this.pageNumber = pageNumber;
    }

    public Integer getPageNumber()
    {
        return pageNumber;
    }

    public void setSnippet(String snippet)
    {
        this.snippet = snippet;
    }

    public String getSnippet()
    {
        return snippet;
    }

    public void setSimilarityScore(BigDecimal similarityScore)
    {
        this.similarityScore = similarityScore;
    }

    public BigDecimal getSimilarityScore()
    {
        return similarityScore;
    }

    public void setCitationOrder(Integer citationOrder)
    {
        this.citationOrder = citationOrder;
    }

    public Integer getCitationOrder()
    {
        return citationOrder;
    }
}

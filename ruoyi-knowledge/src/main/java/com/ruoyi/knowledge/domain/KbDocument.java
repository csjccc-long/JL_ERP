package com.ruoyi.knowledge.domain;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 知识文档对象 kb_document
 *
 * @author ruoyi
 */
public class KbDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 业务关联（非数据库字段） */
    private List<KbObjectLink> links;

    /** 主键ID */
    private Long id;

    /** 空间ID */
    private Long spaceId;

    /** 文档名称 */
    private String documentName;

    /** 稳定键 */
    private String stableKey;

    /** 版本号 */
    private Integer documentVersion;

    /** 是否当前有效版本 */
    private Integer isCurrent;

    /** 存储路径 */
    private String filePath;

    /** 文件大小 */
    private Long fileSize;

    /** MIME类型 */
    private String mimeType;

    /** SHA-256 */
    private String checksumSha256;

    /** RAGFlow 文档ID */
    private String ragDocumentId;

    /** 解析状态 */
    private String parseStatus;

    /** 分块数量 */
    private Integer chunkCount;

    /** Token数量 */
    private Integer tokenCount;

    /** 错误信息 */
    private String errorMessage;

    /** 上传人 */
    private Long uploadedBy;

    /** 就绪时间 */
    private Date readyTime;

    /**  */
    private Long deptId;

    /**  */
    private Integer version;

    public void setLinks(List<KbObjectLink> links)
    {
        this.links = links;
    }

    public List<KbObjectLink> getLinks()
    {
        return links;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setSpaceId(Long spaceId)
    {
        this.spaceId = spaceId;
    }

    public Long getSpaceId()
    {
        return spaceId;
    }

    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setStableKey(String stableKey)
    {
        this.stableKey = stableKey;
    }

    public String getStableKey()
    {
        return stableKey;
    }

    public void setDocumentVersion(Integer documentVersion)
    {
        this.documentVersion = documentVersion;
    }

    public Integer getDocumentVersion()
    {
        return documentVersion;
    }

    public void setIsCurrent(Integer isCurrent)
    {
        this.isCurrent = isCurrent;
    }

    public Integer getIsCurrent()
    {
        return isCurrent;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setChecksumSha256(String checksumSha256)
    {
        this.checksumSha256 = checksumSha256;
    }

    public String getChecksumSha256()
    {
        return checksumSha256;
    }

    public void setRagDocumentId(String ragDocumentId)
    {
        this.ragDocumentId = ragDocumentId;
    }

    public String getRagDocumentId()
    {
        return ragDocumentId;
    }

    public void setParseStatus(String parseStatus)
    {
        this.parseStatus = parseStatus;
    }

    public String getParseStatus()
    {
        return parseStatus;
    }

    public void setChunkCount(Integer chunkCount)
    {
        this.chunkCount = chunkCount;
    }

    public Integer getChunkCount()
    {
        return chunkCount;
    }

    public void setTokenCount(Integer tokenCount)
    {
        this.tokenCount = tokenCount;
    }

    public Integer getTokenCount()
    {
        return tokenCount;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setUploadedBy(Long uploadedBy)
    {
        this.uploadedBy = uploadedBy;
    }

    public Long getUploadedBy()
    {
        return uploadedBy;
    }

    public void setReadyTime(Date readyTime)
    {
        this.readyTime = readyTime;
    }

    public Date getReadyTime()
    {
        return readyTime;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public Integer getVersion()
    {
        return version;
    }
}

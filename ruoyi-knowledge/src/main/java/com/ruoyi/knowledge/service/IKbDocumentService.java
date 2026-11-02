package com.ruoyi.knowledge.service;

import java.nio.file.Path;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbObjectLink;

/**
 * 知识文档服务
 *
 * @author ruoyi
 */
public interface IKbDocumentService
{
    /**
     * 查询文档列表（按空间权限过滤）
     *
     * @param query 查询条件
     * @return 文档集合
     */
    List<KbDocument> selectKbDocumentList(KbDocument query);

    /**
     * 查询文档详情
     *
     * @param id 文档ID
     * @return 文档
     */
    KbDocument selectKbDocumentById(Long id);

    /**
     * 上传文档并触发索引
     *
     * @param file 上传文件
     * @param spaceId 空间ID
     * @param stableKey 稳定键（为空时按文件名生成，实现版本替换）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param relationType 关联类型
     * @return 文档
     */
    KbDocument uploadDocument(MultipartFile file, Long spaceId, String stableKey, String businessType,
            Long businessId, String relationType);

    /**
     * 重新索引
     *
     * @param id 文档ID
     * @return 文档
     */
    KbDocument retryIndex(Long id);

    /**
     * 停用文档
     *
     * @param id 文档ID
     * @return 影响行数
     */
    int disableDocument(Long id);

    /**
     * 解析文档本地文件路径（鉴权后返回）
     *
     * @param id 文档ID
     * @return 文件路径
     */
    Path resolveDocumentFile(Long id);

    /**
     * 关联业务对象
     *
     * @param id 文档ID
     * @param link 关联
     * @return 影响行数
     */
    int linkDocument(Long id, KbObjectLink link);

    /**
     * 取消关联
     *
     * @param id 文档ID
     * @param linkId 关联ID
     * @return 影响行数
     */
    int unlinkDocument(Long id, Long linkId);

    /**
     * 查询文档关联
     *
     * @param id 文档ID
     * @return 关联集合
     */
    List<KbObjectLink> selectLinks(Long id);

    /**
     * 查询关联指定业务对象的文档
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 文档集合
     */
    List<KbDocument> selectDocumentsByBusiness(String businessType, Long businessId);
}

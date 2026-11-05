package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbDocument;

/**
 * 知识文档Mapper接口
 *
 * @author ruoyi
 */
public interface KbDocumentMapper
{
    KbDocument selectById(@Param("id") Long id);

    List<KbDocument> selectList(KbDocument query);

    int insert(KbDocument entity);

    int update(KbDocument entity);

    int deleteById(@Param("id") Long id);

    /**
     * 按可访问空间分页查询文档
     *
     * @param query 查询条件
     * @param spaceIds 可访问空间ID集合
     * @return 文档集合
     */
    List<KbDocument> selectReadableList(@Param("query") KbDocument query, @Param("spaceIds") List<Long> spaceIds);

    /**
     * 按 RAGFlow 文档ID查询
     *
     * @param ragDocumentId RAGFlow 文档ID
     * @return 文档
     */
    KbDocument selectByRagDocumentId(@Param("ragDocumentId") String ragDocumentId);

    /**
     * 查询同一稳定键的当前版本
     *
     * @param spaceId 空间ID
     * @param stableKey 稳定键
     * @return 文档
     */
    KbDocument selectCurrentByStableKey(@Param("spaceId") Long spaceId, @Param("stableKey") String stableKey);

    /**
     * 查询同一稳定键的最大版本号
     *
     * @param spaceId 空间ID
     * @param stableKey 稳定键
     * @return 版本号，无记录返回 0
     */
    Integer selectMaxVersion(@Param("spaceId") Long spaceId, @Param("stableKey") String stableKey);

    /**
     * 清除同一稳定键的当前版本标记
     *
     * @param spaceId 空间ID
     * @param stableKey 稳定键
     * @return 影响行数
     */
    int clearCurrentByStableKey(@Param("spaceId") Long spaceId, @Param("stableKey") String stableKey);

    /**
     * 设置当前版本标记
     *
     * @param id 文档ID
     * @param isCurrent 是否当前版本
     * @return 影响行数
     */
    int updateCurrentFlag(@Param("id") Long id, @Param("isCurrent") int isCurrent);

    /**
     * 标记索引中
     *
     * @param id 文档ID
     * @param ragDocumentId RAGFlow 文档ID
     * @return 影响行数
     */
    int markIndexing(@Param("id") Long id, @Param("ragDocumentId") String ragDocumentId);

    /**
     * 标记就绪
     *
     * @param id 文档ID
     * @param chunkCount 分块数量
     * @param tokenCount Token 数量
     * @return 影响行数
     */
    int markReady(@Param("id") Long id, @Param("chunkCount") Integer chunkCount, @Param("tokenCount") Integer tokenCount);

    /**
     * 标记失败
     *
     * @param id 文档ID
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    int markFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    /**
     * 标记停用
     *
     * @param id 文档ID
     * @return 影响行数
     */
    int markDisabled(@Param("id") Long id);

    /**
     * 标记为已上传（恢复停用的文档，但本地原始文件缺失时使用）
     *
     * @param id 文档ID
     * @return 影响行数
     */
    int markUploaded(@Param("id") Long id);

    /**
     * 查询关联指定业务对象的文档
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param spaceIds 可访问空间ID集合
     * @return 文档集合
     */
    List<KbDocument> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId,
            @Param("spaceIds") List<Long> spaceIds);
}

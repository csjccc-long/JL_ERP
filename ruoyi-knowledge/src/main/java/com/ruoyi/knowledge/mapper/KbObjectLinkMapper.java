package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbObjectLink;

/**
 * 文档业务关联Mapper接口
 *
 * @author ruoyi
 */
public interface KbObjectLinkMapper
{
    KbObjectLink selectById(@Param("id") Long id);

    List<KbObjectLink> selectList(KbObjectLink query);

    int insert(KbObjectLink entity);

    int update(KbObjectLink entity);

    int deleteById(@Param("id") Long id);

    /**
     * 查询业务对象的文档关联
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 关联集合
     */
    List<KbObjectLink> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 查询文档的关联
     *
     * @param documentId 文档ID
     * @return 关联集合
     */
    List<KbObjectLink> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 删除文档全部关联
     *
     * @param documentId 文档ID
     * @return 影响行数
     */
    int deleteByDocumentId(@Param("documentId") Long documentId);
}

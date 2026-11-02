package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbObjectLink;

/**
 * 知识权限与检索范围查询
 *
 * @author ruoyi
 */
public interface KbAuthMapper
{
    /**
     * 查询用户可访问的知识空间ID
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @param admin 是否系统管理员（1 是）
     * @return 空间ID集合
     */
    List<Long> selectReadableSpaceIds(@Param("userId") Long userId, @Param("deptId") Long deptId,
            @Param("admin") int admin);

    /**
     * 查询空间内当前有效的就绪文档
     *
     * @param spaceIds 空间ID集合
     * @return 文档集合
     */
    List<KbDocument> selectReadyDocuments(@Param("spaceIds") List<Long> spaceIds);

    /**
     * 查询文档的业务关联
     *
     * @param documentIds 文档ID集合
     * @return 关联集合
     */
    List<KbObjectLink> selectLinksByDocumentIds(@Param("documentIds") List<Long> documentIds);

    /**
     * 查询空间可管理性（负责人或 MANAGE 授权）
     *
     * @param spaceId 空间ID
     * @param userId 用户ID
     * @param deptId 部门ID
     * @param admin 是否系统管理员（1 是）
     * @return 数量
     */
    int countManageableSpace(@Param("spaceId") Long spaceId, @Param("userId") Long userId,
            @Param("deptId") Long deptId, @Param("admin") int admin);
}

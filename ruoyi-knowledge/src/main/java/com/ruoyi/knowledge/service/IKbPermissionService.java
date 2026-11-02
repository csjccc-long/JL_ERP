package com.ruoyi.knowledge.service;

import java.util.List;
import com.ruoyi.knowledge.domain.KbDocument;

/**
 * 知识权限服务
 *
 * @author ruoyi
 */
public interface IKbPermissionService
{
    /**
     * 当前用户可访问的空间ID
     *
     * @return 空间ID集合
     */
    List<Long> readableSpaceIds();

    /**
     * 当前用户可检索的文档（空间 ACL ∩ 业务对象可见性 ∩ READY 且当前版本）
     *
     * @return 文档集合
     */
    List<KbDocument> retrievableDocuments();

    /**
     * 是否可管理空间
     *
     * @param spaceId 空间ID
     * @return 可管理返回 true
     */
    boolean canManageSpace(Long spaceId);

    /**
     * 是否可读空间
     *
     * @param spaceId 空间ID
     * @return 可读返回 true
     */
    boolean canReadSpace(Long spaceId);
}

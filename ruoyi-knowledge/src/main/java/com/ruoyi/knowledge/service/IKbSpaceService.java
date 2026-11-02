package com.ruoyi.knowledge.service;

import java.util.List;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.domain.KbSpaceAcl;

/**
 * 知识空间服务
 *
 * @author ruoyi
 */
public interface IKbSpaceService
{
    /**
     * 查询当前用户可见的知识空间
     *
     * @param query 查询条件
     * @return 空间集合
     */
    List<KbSpace> selectKbSpaceList(KbSpace query);

    /**
     * 查询知识空间
     *
     * @param id 空间ID
     * @return 空间
     */
    KbSpace selectKbSpaceById(Long id);

    /**
     * 新增知识空间（同时创建 RAGFlow 数据集）
     *
     * @param space 空间
     * @return 影响行数
     */
    int insertKbSpace(KbSpace space);

    /**
     * 修改知识空间
     *
     * @param space 空间
     * @return 影响行数
     */
    int updateKbSpace(KbSpace space);

    /**
     * 停用知识空间并删除数据集
     *
     * @param id 空间ID
     * @return 影响行数
     */
    int disableKbSpace(Long id);

    /**
     * 查询空间授权
     *
     * @param spaceId 空间ID
     * @return 授权集合
     */
    List<KbSpaceAcl> selectAclList(Long spaceId);

    /**
     * 新增空间授权
     *
     * @param spaceId 空间ID
     * @param acl 授权
     * @return 影响行数
     */
    int insertAcl(Long spaceId, KbSpaceAcl acl);

    /**
     * 删除空间授权
     *
     * @param spaceId 空间ID
     * @param aclId 授权ID
     * @return 影响行数
     */
    int deleteAcl(Long spaceId, Long aclId);
}

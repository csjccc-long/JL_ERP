package com.ruoyi.knowledge.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.constant.KbConstants;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.domain.KbSpaceAcl;
import com.ruoyi.knowledge.mapper.KbSpaceAclMapper;
import com.ruoyi.knowledge.mapper.KbSpaceMapper;
import com.ruoyi.knowledge.ragflow.RagFlowClient;
import com.ruoyi.knowledge.ragflow.RagFlowException;
import com.ruoyi.knowledge.service.IKbPermissionService;
import com.ruoyi.knowledge.service.IKbSpaceService;

/**
 * 知识空间服务实现
 *
 * @author ruoyi
 */
@Service
public class KbSpaceServiceImpl implements IKbSpaceService
{
    private static final Logger log = LoggerFactory.getLogger(KbSpaceServiceImpl.class);

    @Autowired
    private KbSpaceMapper spaceMapper;

    @Autowired
    private KbSpaceAclMapper spaceAclMapper;

    @Autowired
    private RagFlowClient ragFlowClient;

    @Autowired
    private IKbPermissionService permissionService;

    @Override
    public List<KbSpace> selectKbSpaceList(KbSpace query)
    {
        List<Long> spaceIds = permissionService.readableSpaceIds();
        if (spaceIds.isEmpty())
        {
            return new ArrayList<KbSpace>();
        }
        return spaceMapper.selectReadableList(query, spaceIds);
    }

    @Override
    public KbSpace selectKbSpaceById(Long id)
    {
        KbSpace space = spaceMapper.selectById(id);
        if (space == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        if (!permissionService.canReadSpace(id))
        {
            throw new ServiceException("无权访问该知识空间", 403);
        }
        return space;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertKbSpace(KbSpace space)
    {
        if (StringUtils.isEmpty(space.getSpaceName()))
        {
            throw new ServiceException("空间名称不能为空");
        }
        if (StringUtils.isEmpty(space.getSpaceType()))
        {
            space.setSpaceType(KbConstants.SPACE_TYPE_POLICY);
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (space.getOwnerId() == null)
        {
            space.setOwnerId(loginUser.getUserId());
        }
        if (space.getDeptId() == null)
        {
            space.setDeptId(loginUser.getDeptId());
        }
        space.setSpaceStatus(KbConstants.SPACE_ACTIVE);
        space.setVersion(0);
        space.setCreateBy(loginUser.getUsername());
        space.setCreateTime(new Date());
        String datasetId;
        try
        {
            datasetId = ragFlowClient.createDataset(space.getSpaceName(),
                    StringUtils.nvl(space.getDescription(), "ERP 知识空间"));
        }
        catch (RagFlowException e)
        {
            log.error("创建 RAGFlow 数据集失败: {}", e.getMessage());
            throw new ServiceException("知识服务不可用，暂时无法创建知识空间，请稍后重试");
        }
        space.setRagDatasetId(datasetId);
        return spaceMapper.insert(space);
    }

    @Override
    public int updateKbSpace(KbSpace space)
    {
        requireManage(space.getId());
        KbSpace existing = spaceMapper.selectById(space.getId());
        if (existing == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        space.setRagDatasetId(existing.getRagDatasetId());
        space.setSpaceStatus(existing.getSpaceStatus());
        space.setUpdateTime(new Date());
        return spaceMapper.update(space);
    }

    @Override
    public int disableKbSpace(Long id)
    {
        requireManage(id);
        KbSpace existing = spaceMapper.selectById(id);
        if (existing == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        if (StringUtils.isNotEmpty(existing.getRagDatasetId()))
        {
            List<String> datasetIds = new ArrayList<String>();
            datasetIds.add(existing.getRagDatasetId());
            try
            {
                ragFlowClient.deleteDatasets(datasetIds);
            }
            catch (RagFlowException e)
            {
                log.warn("删除 RAGFlow 数据集失败，仅停用本地空间: {}", e.getMessage());
            }
        }
        KbSpace update = new KbSpace();
        update.setId(id);
        update.setSpaceStatus(KbConstants.SPACE_DISABLED);
        update.setUpdateTime(new Date());
        return spaceMapper.update(update);
    }

    @Override
    public List<KbSpaceAcl> selectAclList(Long spaceId)
    {
        selectKbSpaceById(spaceId);
        KbSpaceAcl query = new KbSpaceAcl();
        query.setSpaceId(spaceId);
        return spaceAclMapper.selectList(query);
    }

    @Override
    public int insertAcl(Long spaceId, KbSpaceAcl acl)
    {
        requireManage(spaceId);
        if (StringUtils.isEmpty(acl.getSubjectType()) || acl.getSubjectId() == null)
        {
            throw new ServiceException("授权对象不能为空");
        }
        if (StringUtils.isEmpty(acl.getPermissionLevel()))
        {
            acl.setPermissionLevel(KbConstants.PERM_READ);
        }
        acl.setSpaceId(spaceId);
        acl.setCreateBy(SecurityUtils.getUsername());
        acl.setCreateTime(new Date());
        return spaceAclMapper.insert(acl);
    }

    @Override
    public int deleteAcl(Long spaceId, Long aclId)
    {
        requireManage(spaceId);
        KbSpaceAcl acl = spaceAclMapper.selectById(aclId);
        if (acl == null || !spaceId.equals(acl.getSpaceId()))
        {
            throw new ServiceException("授权记录不存在");
        }
        return spaceAclMapper.deleteById(aclId);
    }

    private void requireManage(Long spaceId)
    {
        if (!permissionService.canManageSpace(spaceId))
        {
            throw new ServiceException("无权管理该知识空间", 403);
        }
    }
}

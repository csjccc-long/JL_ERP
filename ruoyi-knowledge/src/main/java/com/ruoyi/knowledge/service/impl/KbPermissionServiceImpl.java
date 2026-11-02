package com.ruoyi.knowledge.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.service.IBusinessObjectService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbObjectLink;
import com.ruoyi.knowledge.mapper.KbAuthMapper;
import com.ruoyi.knowledge.service.IKbPermissionService;

/**
 * 知识权限服务实现
 *
 * @author ruoyi
 */
@Service
public class KbPermissionServiceImpl implements IKbPermissionService
{
    @Autowired
    private KbAuthMapper authMapper;

    @Autowired(required = false)
    private List<IBusinessObjectService> businessObjectServices;

    @Override
    public List<Long> readableSpaceIds()
    {
        LoginUser loginUser = currentUser();
        int admin = SecurityUtils.isAdmin(loginUser.getUserId()) ? 1 : 0;
        List<Long> ids = authMapper.selectReadableSpaceIds(loginUser.getUserId(), loginUser.getDeptId(), admin);
        return ids == null ? new ArrayList<Long>() : ids;
    }

    @Override
    public boolean canReadSpace(Long spaceId)
    {
        return spaceId != null && readableSpaceIds().contains(spaceId);
    }

    @Override
    public boolean canManageSpace(Long spaceId)
    {
        if (spaceId == null)
        {
            return false;
        }
        LoginUser loginUser = currentUser();
        int admin = SecurityUtils.isAdmin(loginUser.getUserId()) ? 1 : 0;
        return authMapper.countManageableSpace(spaceId, loginUser.getUserId(), loginUser.getDeptId(), admin) > 0;
    }

    @Override
    public List<KbDocument> retrievableDocuments()
    {
        List<Long> spaceIds = readableSpaceIds();
        if (spaceIds.isEmpty())
        {
            return new ArrayList<KbDocument>();
        }
        List<KbDocument> documents = authMapper.selectReadyDocuments(spaceIds);
        if (documents == null || documents.isEmpty())
        {
            return new ArrayList<KbDocument>();
        }
        List<Long> documentIds = new ArrayList<Long>();
        for (KbDocument document : documents)
        {
            documentIds.add(document.getId());
        }
        List<KbObjectLink> links = authMapper.selectLinksByDocumentIds(documentIds);
        Map<Long, List<KbObjectLink>> linkMap = new HashMap<Long, List<KbObjectLink>>();
        if (links != null)
        {
            for (KbObjectLink link : links)
            {
                linkMap.computeIfAbsent(link.getDocumentId(), key -> new ArrayList<KbObjectLink>()).add(link);
            }
        }
        List<KbDocument> result = new ArrayList<KbDocument>();
        Set<String> cache = new HashSet<String>();
        for (KbDocument document : documents)
        {
            List<KbObjectLink> documentLinks = linkMap.get(document.getId());
            if (documentLinks == null || documentLinks.isEmpty())
            {
                result.add(document);
                continue;
            }
            boolean visible = true;
            for (KbObjectLink link : documentLinks)
            {
                String key = link.getBusinessType() + ":" + link.getBusinessId();
                boolean linkVisible;
                if (cache.contains(key))
                {
                    linkVisible = true;
                }
                else
                {
                    linkVisible = isBusinessVisible(link.getBusinessType(), link.getBusinessId());
                    if (linkVisible)
                    {
                        cache.add(key);
                    }
                }
                if (!linkVisible)
                {
                    visible = false;
                    break;
                }
            }
            if (visible)
            {
                result.add(document);
            }
        }
        return result;
    }

    private boolean isBusinessVisible(String businessType, Long businessId)
    {
        if (businessObjectServices == null)
        {
            return false;
        }
        for (IBusinessObjectService service : businessObjectServices)
        {
            if (service.supports(businessType))
            {
                return service.canView(businessType, businessId);
            }
        }
        return false;
    }

    private LoginUser currentUser()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null)
        {
            throw new ServiceException("获取用户信息异常");
        }
        return loginUser;
    }
}

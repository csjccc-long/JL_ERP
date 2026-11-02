package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpPurchaseRequest;
import com.ruoyi.erp.domain.ErpPurchaseRequestItem;
import com.ruoyi.erp.mapper.ErpPurchaseRequestMapper;
import com.ruoyi.erp.mapper.ErpPurchaseRequestItemMapper;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.service.IErpPurchaseRequestService;
import com.ruoyi.erp.util.ErpNoGenerator;

@Service
public class ErpPurchaseRequestServiceImpl implements IErpPurchaseRequestService
{
    @Autowired
    private ErpPurchaseRequestMapper erpPurchaseRequestMapper;

    @Autowired
    private ErpNoGenerator erpNoGenerator;

    @Autowired
    private ErpPurchaseRequestItemMapper erpPurchaseRequestItemMapper;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<ErpPurchaseRequest> selectErpPurchaseRequestList(ErpPurchaseRequest erpPurchaseRequest)
    {
        return erpPurchaseRequestMapper.selectErpPurchaseRequestList(erpPurchaseRequest);
    }

    @Override
    public ErpPurchaseRequest selectErpPurchaseRequestById(Long id)
    {
        ErpPurchaseRequest entity = erpPurchaseRequestMapper.selectById(id);
        if (entity != null)
        {
            entity.setItems(erpPurchaseRequestItemMapper.selectByParentId(id));
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest)
    {
        if (StringUtils.isEmpty(erpPurchaseRequest.getRequestNo()))
        {
            erpPurchaseRequest.setRequestNo(erpNoGenerator.requestNo());
        }
        if (StringUtils.isEmpty(erpPurchaseRequest.getRequestStatus()))
        {
            erpPurchaseRequest.setRequestStatus(ErpStatus.REQUEST_DRAFT);
        }
        if (StringUtils.isEmpty(erpPurchaseRequest.getTitle()))
        {
            throw new com.ruoyi.common.exception.ServiceException("请填写采购申请标题");
        }
        if (erpPurchaseRequest.getTargetWarehouseId() == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("请选择需求仓库");
        }
        if (erpPurchaseRequest.getRequiredDate() == null)
        {
            erpPurchaseRequest.setRequiredDate(new java.util.Date());
        }
        if (erpPurchaseRequest.getApplicantId() == null)
        {
            erpPurchaseRequest.setApplicantId(SecurityUtils.getUserId());
        }
        if (erpPurchaseRequest.getDeptId() == null)
        {
            erpPurchaseRequest.setDeptId(SecurityUtils.getDeptId());
        }
        int rows = erpPurchaseRequestMapper.insertErpPurchaseRequest(erpPurchaseRequest);
        insertErpPurchaseRequestItemList(erpPurchaseRequest);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest)
    {
        erpPurchaseRequestItemMapper.deleteByParentId(erpPurchaseRequest.getId());
        int rows = erpPurchaseRequestMapper.updateErpPurchaseRequest(erpPurchaseRequest);
        insertErpPurchaseRequestItemList(erpPurchaseRequest);
        return rows;
    }

    @Override
    public int deleteErpPurchaseRequestByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            erpPurchaseRequestItemMapper.deleteByParentId(id);
        }
        return erpPurchaseRequestMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpPurchaseRequestById(Long id)
    {
        return erpPurchaseRequestMapper.deleteById(id);
    }

    private void insertErpPurchaseRequestItemList(ErpPurchaseRequest erpPurchaseRequest)
    {
        List<ErpPurchaseRequestItem> items = erpPurchaseRequest.getItems();
        if (items == null || items.isEmpty())
        {
            return;
        }
        for (ErpPurchaseRequestItem item : items)
        {
            item.setRequestId(erpPurchaseRequest.getId());
            erpPurchaseRequestItemMapper.insertErpPurchaseRequestItem(item);
        }
    }
}
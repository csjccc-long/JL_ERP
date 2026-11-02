package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpPurchaseOrder;
import com.ruoyi.erp.domain.ErpPurchaseOrderItem;
import com.ruoyi.erp.mapper.ErpPurchaseOrderMapper;
import com.ruoyi.erp.mapper.ErpPurchaseOrderItemMapper;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.service.IErpPurchaseOrderService;
import com.ruoyi.erp.util.ErpNoGenerator;

@Service
public class ErpPurchaseOrderServiceImpl implements IErpPurchaseOrderService
{
    @Autowired
    private ErpPurchaseOrderMapper erpPurchaseOrderMapper;

    @Autowired
    private ErpNoGenerator erpNoGenerator;

    @Autowired
    private ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<ErpPurchaseOrder> selectErpPurchaseOrderList(ErpPurchaseOrder erpPurchaseOrder)
    {
        return erpPurchaseOrderMapper.selectErpPurchaseOrderList(erpPurchaseOrder);
    }

    @Override
    public ErpPurchaseOrder selectErpPurchaseOrderById(Long id)
    {
        ErpPurchaseOrder entity = erpPurchaseOrderMapper.selectById(id);
        if (entity != null)
        {
            entity.setItems(erpPurchaseOrderItemMapper.selectByParentId(id));
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder)
    {
        if (StringUtils.isEmpty(erpPurchaseOrder.getOrderNo()))
        {
            erpPurchaseOrder.setOrderNo(erpNoGenerator.orderNo());
        }
        if (StringUtils.isEmpty(erpPurchaseOrder.getOrderStatus()))
        {
            erpPurchaseOrder.setOrderStatus(ErpStatus.ORDER_DRAFT);
        }
        if (erpPurchaseOrder.getSupplierId() == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("请选择供应商");
        }
        if (erpPurchaseOrder.getOrderDate() == null)
        {
            erpPurchaseOrder.setOrderDate(new java.util.Date());
        }
        if (erpPurchaseOrder.getExpectedDate() == null)
        {
            erpPurchaseOrder.setExpectedDate(erpPurchaseOrder.getOrderDate());
        }
        if (erpPurchaseOrder.getTotalAmount() == null)
        {
            erpPurchaseOrder.setTotalAmount(java.math.BigDecimal.ZERO.setScale(2));
        }
        if (erpPurchaseOrder.getDeptId() == null)
        {
            erpPurchaseOrder.setDeptId(SecurityUtils.getDeptId());
        }
        int rows = erpPurchaseOrderMapper.insertErpPurchaseOrder(erpPurchaseOrder);
        insertErpPurchaseOrderItemList(erpPurchaseOrder);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder)
    {
        erpPurchaseOrderItemMapper.deleteByParentId(erpPurchaseOrder.getId());
        int rows = erpPurchaseOrderMapper.updateErpPurchaseOrder(erpPurchaseOrder);
        insertErpPurchaseOrderItemList(erpPurchaseOrder);
        return rows;
    }

    @Override
    public int deleteErpPurchaseOrderByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            erpPurchaseOrderItemMapper.deleteByParentId(id);
        }
        return erpPurchaseOrderMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpPurchaseOrderById(Long id)
    {
        return erpPurchaseOrderMapper.deleteById(id);
    }

    private void insertErpPurchaseOrderItemList(ErpPurchaseOrder erpPurchaseOrder)
    {
        List<ErpPurchaseOrderItem> items = erpPurchaseOrder.getItems();
        if (items == null || items.isEmpty())
        {
            return;
        }
        for (ErpPurchaseOrderItem item : items)
        {
            item.setOrderId(erpPurchaseOrder.getId());
            erpPurchaseOrderItemMapper.insertErpPurchaseOrderItem(item);
        }
    }
}
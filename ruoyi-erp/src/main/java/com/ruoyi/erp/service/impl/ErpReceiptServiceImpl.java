package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpReceipt;
import com.ruoyi.erp.domain.ErpReceiptItem;
import com.ruoyi.erp.mapper.ErpReceiptMapper;
import com.ruoyi.erp.mapper.ErpReceiptItemMapper;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.service.IErpReceiptService;
import com.ruoyi.erp.util.ErpNoGenerator;

@Service
public class ErpReceiptServiceImpl implements IErpReceiptService
{
    @Autowired
    private ErpReceiptMapper erpReceiptMapper;

    @Autowired
    private ErpNoGenerator erpNoGenerator;

    @Autowired
    private ErpReceiptItemMapper erpReceiptItemMapper;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<ErpReceipt> selectErpReceiptList(ErpReceipt erpReceipt)
    {
        return erpReceiptMapper.selectErpReceiptList(erpReceipt);
    }

    @Override
    public ErpReceipt selectErpReceiptById(Long id)
    {
        ErpReceipt entity = erpReceiptMapper.selectById(id);
        if (entity != null)
        {
            entity.setItems(erpReceiptItemMapper.selectByParentId(id));
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpReceipt(ErpReceipt erpReceipt)
    {
        if (StringUtils.isEmpty(erpReceipt.getReceiptNo()))
        {
            erpReceipt.setReceiptNo(erpNoGenerator.receiptNo());
        }
        if (StringUtils.isEmpty(erpReceipt.getReceiptStatus()))
        {
            erpReceipt.setReceiptStatus(ErpStatus.RECEIPT_DRAFT);
        }
        if (StringUtils.isEmpty(erpReceipt.getIdempotencyKey()))
        {
            erpReceipt.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        }
        if (erpReceipt.getReceiptDate() == null)
        {
            erpReceipt.setReceiptDate(new java.util.Date());
        }
        if (erpReceipt.getDeptId() == null)
        {
            erpReceipt.setDeptId(SecurityUtils.getDeptId());
        }
        int rows = erpReceiptMapper.insertErpReceipt(erpReceipt);
        insertErpReceiptItemList(erpReceipt);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpReceipt(ErpReceipt erpReceipt)
    {
        erpReceiptItemMapper.deleteByParentId(erpReceipt.getId());
        int rows = erpReceiptMapper.updateErpReceipt(erpReceipt);
        insertErpReceiptItemList(erpReceipt);
        return rows;
    }

    @Override
    public int deleteErpReceiptByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            erpReceiptItemMapper.deleteByParentId(id);
        }
        return erpReceiptMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpReceiptById(Long id)
    {
        return erpReceiptMapper.deleteById(id);
    }

    private void insertErpReceiptItemList(ErpReceipt erpReceipt)
    {
        List<ErpReceiptItem> items = erpReceipt.getItems();
        if (items == null || items.isEmpty())
        {
            return;
        }
        for (ErpReceiptItem item : items)
        {
            item.setReceiptId(erpReceipt.getId());
            erpReceiptItemMapper.insertErpReceiptItem(item);
        }
    }
}
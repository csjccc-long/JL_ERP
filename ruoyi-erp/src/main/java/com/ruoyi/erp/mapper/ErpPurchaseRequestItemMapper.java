package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseRequestItem;

public interface ErpPurchaseRequestItemMapper
{
    List<ErpPurchaseRequestItem> selectByParentId(Long requestId);

    int insertErpPurchaseRequestItem(ErpPurchaseRequestItem erpPurchaseRequestItem);

    int deleteByParentId(Long requestId);
}
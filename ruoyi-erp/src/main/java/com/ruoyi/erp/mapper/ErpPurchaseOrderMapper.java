package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseOrder;

public interface ErpPurchaseOrderMapper
{
    ErpPurchaseOrder selectById(Long id);

    List<ErpPurchaseOrder> selectErpPurchaseOrderList(ErpPurchaseOrder erpPurchaseOrder);

    int insertErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder);

    int updateErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseOrder;

public interface IErpPurchaseOrderService
{
    public List<ErpPurchaseOrder> selectErpPurchaseOrderList(ErpPurchaseOrder erpPurchaseOrder);

    public ErpPurchaseOrder selectErpPurchaseOrderById(Long id);

    public int insertErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder);

    public int updateErpPurchaseOrder(ErpPurchaseOrder erpPurchaseOrder);

    public int deleteErpPurchaseOrderByIds(Long[] ids);

    public int deleteErpPurchaseOrderById(Long id);
}
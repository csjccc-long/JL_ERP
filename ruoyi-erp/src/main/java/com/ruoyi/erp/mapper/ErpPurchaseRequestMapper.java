package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseRequest;

public interface ErpPurchaseRequestMapper
{
    ErpPurchaseRequest selectById(Long id);

    List<ErpPurchaseRequest> selectErpPurchaseRequestList(ErpPurchaseRequest erpPurchaseRequest);

    int insertErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest);

    int updateErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
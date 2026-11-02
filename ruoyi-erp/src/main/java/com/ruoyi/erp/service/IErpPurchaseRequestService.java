package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpPurchaseRequest;

public interface IErpPurchaseRequestService
{
    public List<ErpPurchaseRequest> selectErpPurchaseRequestList(ErpPurchaseRequest erpPurchaseRequest);

    public ErpPurchaseRequest selectErpPurchaseRequestById(Long id);

    public int insertErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest);

    public int updateErpPurchaseRequest(ErpPurchaseRequest erpPurchaseRequest);

    public int deleteErpPurchaseRequestByIds(Long[] ids);

    public int deleteErpPurchaseRequestById(Long id);
}
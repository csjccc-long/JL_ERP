package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpReceipt;

public interface IErpReceiptService
{
    public List<ErpReceipt> selectErpReceiptList(ErpReceipt erpReceipt);

    public ErpReceipt selectErpReceiptById(Long id);

    public int insertErpReceipt(ErpReceipt erpReceipt);

    public int updateErpReceipt(ErpReceipt erpReceipt);

    public int deleteErpReceiptByIds(Long[] ids);

    public int deleteErpReceiptById(Long id);
}
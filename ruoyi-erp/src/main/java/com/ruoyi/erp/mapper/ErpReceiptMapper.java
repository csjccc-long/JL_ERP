package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpReceipt;

public interface ErpReceiptMapper
{
    ErpReceipt selectById(Long id);

    List<ErpReceipt> selectErpReceiptList(ErpReceipt erpReceipt);

    int insertErpReceipt(ErpReceipt erpReceipt);

    int updateErpReceipt(ErpReceipt erpReceipt);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
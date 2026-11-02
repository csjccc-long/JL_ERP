package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpReceiptItem;

public interface ErpReceiptItemMapper
{
    List<ErpReceiptItem> selectByParentId(Long receiptId);

    int insertErpReceiptItem(ErpReceiptItem erpReceiptItem);

    int deleteByParentId(Long receiptId);
}
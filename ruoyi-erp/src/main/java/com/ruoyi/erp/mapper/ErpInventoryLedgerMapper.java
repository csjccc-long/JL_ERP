package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryLedger;

public interface ErpInventoryLedgerMapper
{
    ErpInventoryLedger selectById(Long id);

    List<ErpInventoryLedger> selectErpInventoryLedgerList(ErpInventoryLedger erpInventoryLedger);

    int insertErpInventoryLedger(ErpInventoryLedger erpInventoryLedger);

    int updateErpInventoryLedger(ErpInventoryLedger erpInventoryLedger);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryLedger;

public interface IErpInventoryLedgerService
{
    public List<ErpInventoryLedger> selectErpInventoryLedgerList(ErpInventoryLedger erpInventoryLedger);

    public ErpInventoryLedger selectErpInventoryLedgerById(Long id);

    public int insertErpInventoryLedger(ErpInventoryLedger erpInventoryLedger);

    public int updateErpInventoryLedger(ErpInventoryLedger erpInventoryLedger);

    public int deleteErpInventoryLedgerByIds(Long[] ids);

    public int deleteErpInventoryLedgerById(Long id);
}
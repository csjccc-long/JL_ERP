package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryBalance;

public interface IErpInventoryBalanceService
{
    public List<ErpInventoryBalance> selectErpInventoryBalanceList(ErpInventoryBalance erpInventoryBalance);

    public ErpInventoryBalance selectErpInventoryBalanceById(Long id);

    public int insertErpInventoryBalance(ErpInventoryBalance erpInventoryBalance);

    public int updateErpInventoryBalance(ErpInventoryBalance erpInventoryBalance);

    public int deleteErpInventoryBalanceByIds(Long[] ids);

    public int deleteErpInventoryBalanceById(Long id);
}
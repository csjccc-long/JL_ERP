package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpInventoryBalance;

public interface ErpInventoryBalanceMapper
{
    ErpInventoryBalance selectById(Long id);

    List<ErpInventoryBalance> selectErpInventoryBalanceList(ErpInventoryBalance erpInventoryBalance);

    int insertErpInventoryBalance(ErpInventoryBalance erpInventoryBalance);

    int updateErpInventoryBalance(ErpInventoryBalance erpInventoryBalance);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
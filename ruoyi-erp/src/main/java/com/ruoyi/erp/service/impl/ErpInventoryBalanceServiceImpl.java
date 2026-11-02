package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpInventoryBalance;
import com.ruoyi.erp.mapper.ErpInventoryBalanceMapper;
import com.ruoyi.erp.service.IErpInventoryBalanceService;

@Service
public class ErpInventoryBalanceServiceImpl implements IErpInventoryBalanceService
{
    @Autowired
    private ErpInventoryBalanceMapper erpInventoryBalanceMapper;

    @Override
    public List<ErpInventoryBalance> selectErpInventoryBalanceList(ErpInventoryBalance erpInventoryBalance)
    {
        return erpInventoryBalanceMapper.selectErpInventoryBalanceList(erpInventoryBalance);
    }

    @Override
    public ErpInventoryBalance selectErpInventoryBalanceById(Long id)
    {
        ErpInventoryBalance entity = erpInventoryBalanceMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpInventoryBalance(ErpInventoryBalance erpInventoryBalance)
    {
        int rows = erpInventoryBalanceMapper.insertErpInventoryBalance(erpInventoryBalance);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpInventoryBalance(ErpInventoryBalance erpInventoryBalance)
    {
        int rows = erpInventoryBalanceMapper.updateErpInventoryBalance(erpInventoryBalance);
        return rows;
    }

    @Override
    public int deleteErpInventoryBalanceByIds(Long[] ids)
    {
        return erpInventoryBalanceMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpInventoryBalanceById(Long id)
    {
        return erpInventoryBalanceMapper.deleteById(id);
    }
}
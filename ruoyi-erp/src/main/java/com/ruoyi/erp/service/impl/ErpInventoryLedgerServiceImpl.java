package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpInventoryLedger;
import com.ruoyi.erp.mapper.ErpInventoryLedgerMapper;
import com.ruoyi.erp.service.IErpInventoryLedgerService;

@Service
public class ErpInventoryLedgerServiceImpl implements IErpInventoryLedgerService
{
    @Autowired
    private ErpInventoryLedgerMapper erpInventoryLedgerMapper;

    @Override
    public List<ErpInventoryLedger> selectErpInventoryLedgerList(ErpInventoryLedger erpInventoryLedger)
    {
        return erpInventoryLedgerMapper.selectErpInventoryLedgerList(erpInventoryLedger);
    }

    @Override
    public ErpInventoryLedger selectErpInventoryLedgerById(Long id)
    {
        ErpInventoryLedger entity = erpInventoryLedgerMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpInventoryLedger(ErpInventoryLedger erpInventoryLedger)
    {
        int rows = erpInventoryLedgerMapper.insertErpInventoryLedger(erpInventoryLedger);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpInventoryLedger(ErpInventoryLedger erpInventoryLedger)
    {
        int rows = erpInventoryLedgerMapper.updateErpInventoryLedger(erpInventoryLedger);
        return rows;
    }

    @Override
    public int deleteErpInventoryLedgerByIds(Long[] ids)
    {
        return erpInventoryLedgerMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpInventoryLedgerById(Long id)
    {
        return erpInventoryLedgerMapper.deleteById(id);
    }
}
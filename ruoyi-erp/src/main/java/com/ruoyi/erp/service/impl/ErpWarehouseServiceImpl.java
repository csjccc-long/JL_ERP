package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpWarehouse;
import com.ruoyi.erp.mapper.ErpWarehouseMapper;
import com.ruoyi.erp.service.IErpWarehouseService;

@Service
public class ErpWarehouseServiceImpl implements IErpWarehouseService
{
    @Autowired
    private ErpWarehouseMapper erpWarehouseMapper;

    @Override
    public List<ErpWarehouse> selectErpWarehouseList(ErpWarehouse erpWarehouse)
    {
        return erpWarehouseMapper.selectErpWarehouseList(erpWarehouse);
    }

    @Override
    public ErpWarehouse selectErpWarehouseById(Long id)
    {
        ErpWarehouse entity = erpWarehouseMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpWarehouse(ErpWarehouse erpWarehouse)
    {
        int rows = erpWarehouseMapper.insertErpWarehouse(erpWarehouse);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpWarehouse(ErpWarehouse erpWarehouse)
    {
        int rows = erpWarehouseMapper.updateErpWarehouse(erpWarehouse);
        return rows;
    }

    @Override
    public int deleteErpWarehouseByIds(Long[] ids)
    {
        return erpWarehouseMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpWarehouseById(Long id)
    {
        return erpWarehouseMapper.deleteById(id);
    }
}
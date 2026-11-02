package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpWarehouse;

public interface IErpWarehouseService
{
    public List<ErpWarehouse> selectErpWarehouseList(ErpWarehouse erpWarehouse);

    public ErpWarehouse selectErpWarehouseById(Long id);

    public int insertErpWarehouse(ErpWarehouse erpWarehouse);

    public int updateErpWarehouse(ErpWarehouse erpWarehouse);

    public int deleteErpWarehouseByIds(Long[] ids);

    public int deleteErpWarehouseById(Long id);
}
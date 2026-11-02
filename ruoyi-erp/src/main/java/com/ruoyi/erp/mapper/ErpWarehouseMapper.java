package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpWarehouse;

public interface ErpWarehouseMapper
{
    ErpWarehouse selectById(Long id);

    List<ErpWarehouse> selectErpWarehouseList(ErpWarehouse erpWarehouse);

    int insertErpWarehouse(ErpWarehouse erpWarehouse);

    int updateErpWarehouse(ErpWarehouse erpWarehouse);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
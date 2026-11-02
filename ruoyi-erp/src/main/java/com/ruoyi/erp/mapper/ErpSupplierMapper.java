package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpSupplier;

public interface ErpSupplierMapper
{
    ErpSupplier selectById(Long id);

    List<ErpSupplier> selectErpSupplierList(ErpSupplier erpSupplier);

    int insertErpSupplier(ErpSupplier erpSupplier);

    int updateErpSupplier(ErpSupplier erpSupplier);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
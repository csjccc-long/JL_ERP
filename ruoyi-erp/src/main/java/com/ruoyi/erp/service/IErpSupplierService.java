package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpSupplier;

public interface IErpSupplierService
{
    public List<ErpSupplier> selectErpSupplierList(ErpSupplier erpSupplier);

    public ErpSupplier selectErpSupplierById(Long id);

    public int insertErpSupplier(ErpSupplier erpSupplier);

    public int updateErpSupplier(ErpSupplier erpSupplier);

    public int deleteErpSupplierByIds(Long[] ids);

    public int deleteErpSupplierById(Long id);
}
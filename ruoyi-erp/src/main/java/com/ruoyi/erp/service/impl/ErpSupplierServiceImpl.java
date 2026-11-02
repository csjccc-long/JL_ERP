package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpSupplier;
import com.ruoyi.erp.mapper.ErpSupplierMapper;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.service.IErpSupplierService;
import com.ruoyi.erp.util.ErpNoGenerator;

@Service
public class ErpSupplierServiceImpl implements IErpSupplierService
{
    @Autowired
    private ErpSupplierMapper erpSupplierMapper;

    @Autowired
    private ErpNoGenerator erpNoGenerator;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<ErpSupplier> selectErpSupplierList(ErpSupplier erpSupplier)
    {
        return erpSupplierMapper.selectErpSupplierList(erpSupplier);
    }

    @Override
    public ErpSupplier selectErpSupplierById(Long id)
    {
        ErpSupplier entity = erpSupplierMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpSupplier(ErpSupplier erpSupplier)
    {
        if (StringUtils.isEmpty(erpSupplier.getSupplierCode()))
        {
            erpSupplier.setSupplierCode(erpNoGenerator.supplierCode());
        }
        if (StringUtils.isEmpty(erpSupplier.getAdmissionStatus()))
        {
            erpSupplier.setAdmissionStatus(ErpStatus.SUPPLIER_PENDING);
        }
        if (erpSupplier.getDeptId() == null)
        {
            erpSupplier.setDeptId(SecurityUtils.getDeptId());
        }
        int rows = erpSupplierMapper.insertErpSupplier(erpSupplier);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpSupplier(ErpSupplier erpSupplier)
    {
        int rows = erpSupplierMapper.updateErpSupplier(erpSupplier);
        return rows;
    }

    @Override
    public int deleteErpSupplierByIds(Long[] ids)
    {
        return erpSupplierMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpSupplierById(Long id)
    {
        return erpSupplierMapper.deleteById(id);
    }
}
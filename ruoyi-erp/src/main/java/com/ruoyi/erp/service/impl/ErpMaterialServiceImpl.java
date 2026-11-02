package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpMaterial;
import com.ruoyi.erp.mapper.ErpMaterialMapper;
import com.ruoyi.erp.service.IErpMaterialService;

@Service
public class ErpMaterialServiceImpl implements IErpMaterialService
{
    @Autowired
    private ErpMaterialMapper erpMaterialMapper;

    @Override
    public List<ErpMaterial> selectErpMaterialList(ErpMaterial erpMaterial)
    {
        return erpMaterialMapper.selectErpMaterialList(erpMaterial);
    }

    @Override
    public ErpMaterial selectErpMaterialById(Long id)
    {
        ErpMaterial entity = erpMaterialMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpMaterial(ErpMaterial erpMaterial)
    {
        int rows = erpMaterialMapper.insertErpMaterial(erpMaterial);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpMaterial(ErpMaterial erpMaterial)
    {
        int rows = erpMaterialMapper.updateErpMaterial(erpMaterial);
        return rows;
    }

    @Override
    public int deleteErpMaterialByIds(Long[] ids)
    {
        return erpMaterialMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpMaterialById(Long id)
    {
        return erpMaterialMapper.deleteById(id);
    }
}
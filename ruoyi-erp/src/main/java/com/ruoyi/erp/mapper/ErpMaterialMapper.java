package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpMaterial;

public interface ErpMaterialMapper
{
    ErpMaterial selectById(Long id);

    List<ErpMaterial> selectErpMaterialList(ErpMaterial erpMaterial);

    int insertErpMaterial(ErpMaterial erpMaterial);

    int updateErpMaterial(ErpMaterial erpMaterial);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
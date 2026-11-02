package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpMaterial;

public interface IErpMaterialService
{
    public List<ErpMaterial> selectErpMaterialList(ErpMaterial erpMaterial);

    public ErpMaterial selectErpMaterialById(Long id);

    public int insertErpMaterial(ErpMaterial erpMaterial);

    public int updateErpMaterial(ErpMaterial erpMaterial);

    public int deleteErpMaterialByIds(Long[] ids);

    public int deleteErpMaterialById(Long id);
}
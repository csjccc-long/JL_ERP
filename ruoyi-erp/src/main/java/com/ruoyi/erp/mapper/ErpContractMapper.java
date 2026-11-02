package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpContract;

public interface ErpContractMapper
{
    ErpContract selectById(Long id);

    List<ErpContract> selectErpContractList(ErpContract erpContract);

    int insertErpContract(ErpContract erpContract);

    int updateErpContract(ErpContract erpContract);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
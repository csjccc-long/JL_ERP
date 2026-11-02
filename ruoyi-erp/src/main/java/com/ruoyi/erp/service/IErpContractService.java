package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpContract;

public interface IErpContractService
{
    public List<ErpContract> selectErpContractList(ErpContract erpContract);

    public ErpContract selectErpContractById(Long id);

    public int insertErpContract(ErpContract erpContract);

    public int updateErpContract(ErpContract erpContract);

    public int deleteErpContractByIds(Long[] ids);

    public int deleteErpContractById(Long id);
}
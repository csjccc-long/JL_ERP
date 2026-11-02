package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpContract;
import com.ruoyi.erp.mapper.ErpContractMapper;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.service.IErpContractService;
import com.ruoyi.erp.util.ErpNoGenerator;

@Service
public class ErpContractServiceImpl implements IErpContractService
{
    @Autowired
    private ErpContractMapper erpContractMapper;

    @Autowired
    private ErpNoGenerator erpNoGenerator;

    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<ErpContract> selectErpContractList(ErpContract erpContract)
    {
        return erpContractMapper.selectErpContractList(erpContract);
    }

    @Override
    public ErpContract selectErpContractById(Long id)
    {
        ErpContract entity = erpContractMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpContract(ErpContract erpContract)
    {
        if (StringUtils.isEmpty(erpContract.getContractNo()))
        {
            erpContract.setContractNo(erpNoGenerator.contractNo());
        }
        if (StringUtils.isEmpty(erpContract.getContractStatus()))
        {
            erpContract.setContractStatus(ErpStatus.CONTRACT_DRAFT);
        }
        if (erpContract.getDeptId() == null)
        {
            erpContract.setDeptId(SecurityUtils.getDeptId());
        }
        if (erpContract.getOwnerId() == null)
        {
            erpContract.setOwnerId(SecurityUtils.getUserId());
        }
        int rows = erpContractMapper.insertErpContract(erpContract);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpContract(ErpContract erpContract)
    {
        int rows = erpContractMapper.updateErpContract(erpContract);
        return rows;
    }

    @Override
    public int deleteErpContractByIds(Long[] ids)
    {
        return erpContractMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpContractById(Long id)
    {
        return erpContractMapper.deleteById(id);
    }
}
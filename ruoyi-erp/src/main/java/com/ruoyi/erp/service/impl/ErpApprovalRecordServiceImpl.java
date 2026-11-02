package com.ruoyi.erp.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.erp.domain.ErpApprovalRecord;
import com.ruoyi.erp.mapper.ErpApprovalRecordMapper;
import com.ruoyi.erp.service.IErpApprovalRecordService;

@Service
public class ErpApprovalRecordServiceImpl implements IErpApprovalRecordService
{
    @Autowired
    private ErpApprovalRecordMapper erpApprovalRecordMapper;

    @Override
    public List<ErpApprovalRecord> selectErpApprovalRecordList(ErpApprovalRecord erpApprovalRecord)
    {
        return erpApprovalRecordMapper.selectErpApprovalRecordList(erpApprovalRecord);
    }

    @Override
    public ErpApprovalRecord selectErpApprovalRecordById(Long id)
    {
        ErpApprovalRecord entity = erpApprovalRecordMapper.selectById(id);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertErpApprovalRecord(ErpApprovalRecord erpApprovalRecord)
    {
        int rows = erpApprovalRecordMapper.insertErpApprovalRecord(erpApprovalRecord);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateErpApprovalRecord(ErpApprovalRecord erpApprovalRecord)
    {
        int rows = erpApprovalRecordMapper.updateErpApprovalRecord(erpApprovalRecord);
        return rows;
    }

    @Override
    public int deleteErpApprovalRecordByIds(Long[] ids)
    {
        return erpApprovalRecordMapper.deleteByIds(ids);
    }

    @Override
    public int deleteErpApprovalRecordById(Long id)
    {
        return erpApprovalRecordMapper.deleteById(id);
    }
}
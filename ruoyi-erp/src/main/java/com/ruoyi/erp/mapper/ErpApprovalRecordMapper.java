package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.domain.ErpApprovalRecord;

public interface ErpApprovalRecordMapper
{
    ErpApprovalRecord selectById(Long id);

    List<ErpApprovalRecord> selectErpApprovalRecordList(ErpApprovalRecord erpApprovalRecord);

    int insertErpApprovalRecord(ErpApprovalRecord erpApprovalRecord);

    int updateErpApprovalRecord(ErpApprovalRecord erpApprovalRecord);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);
}
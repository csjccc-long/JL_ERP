package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpApprovalRecord;

public interface IErpApprovalRecordService
{
    public List<ErpApprovalRecord> selectErpApprovalRecordList(ErpApprovalRecord erpApprovalRecord);

    public ErpApprovalRecord selectErpApprovalRecordById(Long id);

    public int insertErpApprovalRecord(ErpApprovalRecord erpApprovalRecord);

    public int updateErpApprovalRecord(ErpApprovalRecord erpApprovalRecord);

    public int deleteErpApprovalRecordByIds(Long[] ids);

    public int deleteErpApprovalRecordById(Long id);
}
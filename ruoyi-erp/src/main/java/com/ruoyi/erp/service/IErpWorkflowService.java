package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.domain.ErpApprovalRecord;

/**
 * ERP 业务流程服务（提交、审批、入库确认、冲销）
 *
 * @author ruoyi
 */
public interface IErpWorkflowService
{
    /** 提交供应商准入审核 */
    void submitSupplier(Long id);

    /** 供应商准入审核 */
    void auditSupplier(Long id, String admissionStatus, String riskLevel, String opinion);

    /** 提交合同审批 */
    void submitContract(Long id);

    /** 合同审批 */
    void approveContract(Long id, boolean pass, String opinion);

    /** 合同终止 */
    void terminateContract(Long id, String opinion);

    /** 提交采购申请 */
    void submitRequest(Long id);

    /** 采购申请审批 */
    void approveRequest(Long id, boolean pass, String opinion);

    /** 提交采购订单 */
    void submitOrder(Long id);

    /** 采购订单审批 */
    void approveOrder(Long id, boolean pass, String opinion);

    /** 取消采购订单 */
    void cancelOrder(Long id, String opinion);

    /** 到货入库确认（单事务扣减、库存结转、幂等） */
    void confirmReceipt(Long id);

    /** 入库冲销 */
    void reverseReceipt(Long id, String opinion);

    /** 查询业务单据审批轨迹 */
    List<ErpApprovalRecord> selectApprovalRecords(String businessType, Long businessId);
}

package com.ruoyi.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.domain.ErpApprovalRecord;
import com.ruoyi.erp.domain.ErpContract;
import com.ruoyi.erp.domain.ErpInventoryBalance;
import com.ruoyi.erp.domain.ErpInventoryLedger;
import com.ruoyi.erp.domain.ErpPurchaseOrder;
import com.ruoyi.erp.domain.ErpPurchaseOrderItem;
import com.ruoyi.erp.domain.ErpPurchaseRequest;
import com.ruoyi.erp.domain.ErpReceipt;
import com.ruoyi.erp.domain.ErpReceiptItem;
import com.ruoyi.erp.domain.ErpSupplier;
import com.ruoyi.erp.mapper.ErpApprovalRecordMapper;
import com.ruoyi.erp.mapper.ErpContractMapper;
import com.ruoyi.erp.mapper.ErpInventoryBalanceMapper;
import com.ruoyi.erp.mapper.ErpInventoryLedgerMapper;
import com.ruoyi.erp.mapper.ErpPurchaseOrderItemMapper;
import com.ruoyi.erp.mapper.ErpPurchaseOrderMapper;
import com.ruoyi.erp.mapper.ErpPurchaseRequestMapper;
import com.ruoyi.erp.mapper.ErpReceiptItemMapper;
import com.ruoyi.erp.mapper.ErpReceiptMapper;
import com.ruoyi.erp.mapper.ErpSupplierMapper;
import com.ruoyi.erp.service.IErpWorkflowService;

/**
 * ERP 业务流程服务实现
 *
 * @author ruoyi
 */
@Service
public class ErpWorkflowServiceImpl implements IErpWorkflowService
{
    @Autowired
    private ErpSupplierMapper supplierMapper;

    @Autowired
    private ErpContractMapper contractMapper;

    @Autowired
    private ErpPurchaseRequestMapper requestMapper;

    @Autowired
    private ErpPurchaseOrderMapper orderMapper;

    @Autowired
    private ErpPurchaseOrderItemMapper orderItemMapper;

    @Autowired
    private ErpReceiptMapper receiptMapper;

    @Autowired
    private ErpReceiptItemMapper receiptItemMapper;

    @Autowired
    private ErpInventoryBalanceMapper balanceMapper;

    @Autowired
    private ErpInventoryLedgerMapper ledgerMapper;

    @Autowired
    private ErpApprovalRecordMapper approvalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSupplier(Long id)
    {
        ErpSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null)
        {
            throw new ServiceException("供应商不存在");
        }
        if (ErpStatus.SUPPLIER_APPROVED.equals(supplier.getAdmissionStatus()))
        {
            throw new ServiceException("供应商已通过准入审核");
        }
        if (ErpStatus.SUPPLIER_SUSPENDED.equals(supplier.getAdmissionStatus()))
        {
            throw new ServiceException("供应商已暂停合作，请先恢复后重新提交");
        }
        String from = supplier.getAdmissionStatus();
        supplier.setAdmissionStatus(ErpStatus.SUPPLIER_PENDING);
        supplier.setUpdateBy(currentUsername());
        supplierMapper.updateErpSupplier(supplier);
        record(ErpStatus.BIZ_SUPPLIER, id, ErpStatus.ACTION_SUBMIT, from, ErpStatus.SUPPLIER_PENDING, "提交准入审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplier(Long id, String admissionStatus, String riskLevel, String opinion)
    {
        ErpSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null)
        {
            throw new ServiceException("供应商不存在");
        }
        if (!ErpStatus.SUPPLIER_APPROVED.equals(admissionStatus) && !ErpStatus.SUPPLIER_REJECTED.equals(admissionStatus)
                && !ErpStatus.SUPPLIER_SUSPENDED.equals(admissionStatus) && !ErpStatus.SUPPLIER_PENDING.equals(admissionStatus))
        {
            throw new ServiceException("准入状态不合法");
        }
        String from = supplier.getAdmissionStatus();
        if (ErpStatus.SUPPLIER_APPROVED.equals(admissionStatus))
        {
            supplier.setAdmissionTime(new Date());
        }
        if (StringUtils.isNotEmpty(riskLevel))
        {
            supplier.setRiskLevel(riskLevel);
        }
        supplier.setAdmissionStatus(admissionStatus);
        supplier.setUpdateBy(currentUsername());
        supplierMapper.updateErpSupplier(supplier);
        record(ErpStatus.BIZ_SUPPLIER, id,
                ErpStatus.SUPPLIER_APPROVED.equals(admissionStatus) ? ErpStatus.ACTION_APPROVE : ErpStatus.ACTION_REJECT,
                from, admissionStatus, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitContract(Long id)
    {
        ErpContract contract = contractMapper.selectById(id);
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        if (!ErpStatus.CONTRACT_DRAFT.equals(contract.getContractStatus()))
        {
            throw new ServiceException("仅草稿状态的合同可以提交审批");
        }
        String from = contract.getContractStatus();
        contract.setContractStatus(ErpStatus.CONTRACT_PENDING);
        contract.setUpdateBy(currentUsername());
        contractMapper.updateErpContract(contract);
        record(ErpStatus.BIZ_CONTRACT, id, ErpStatus.ACTION_SUBMIT, from, ErpStatus.CONTRACT_PENDING, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveContract(Long id, boolean pass, String opinion)
    {
        ErpContract contract = contractMapper.selectById(id);
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        if (!ErpStatus.CONTRACT_PENDING.equals(contract.getContractStatus()))
        {
            throw new ServiceException("仅待审批状态的合同可以审批");
        }
        String from = contract.getContractStatus();
        String to = pass ? ErpStatus.CONTRACT_ACTIVE : ErpStatus.CONTRACT_DRAFT;
        contract.setContractStatus(to);
        contract.setUpdateBy(currentUsername());
        contractMapper.updateErpContract(contract);
        record(ErpStatus.BIZ_CONTRACT, id, pass ? ErpStatus.ACTION_APPROVE : ErpStatus.ACTION_REJECT, from, to, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateContract(Long id, String opinion)
    {
        ErpContract contract = contractMapper.selectById(id);
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        if (!ErpStatus.CONTRACT_ACTIVE.equals(contract.getContractStatus()))
        {
            throw new ServiceException("仅生效中的合同可以终止");
        }
        String from = contract.getContractStatus();
        contract.setContractStatus(ErpStatus.CONTRACT_TERMINATED);
        contract.setUpdateBy(currentUsername());
        contractMapper.updateErpContract(contract);
        record(ErpStatus.BIZ_CONTRACT, id, ErpStatus.ACTION_TERMINATE, from, ErpStatus.CONTRACT_TERMINATED, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRequest(Long id)
    {
        ErpPurchaseRequest request = requestMapper.selectById(id);
        if (request == null)
        {
            throw new ServiceException("采购申请不存在");
        }
        if (!ErpStatus.REQUEST_DRAFT.equals(request.getRequestStatus()))
        {
            throw new ServiceException("仅草稿状态的采购申请可以提交");
        }
        String from = request.getRequestStatus();
        request.setRequestStatus(ErpStatus.REQUEST_PENDING);
        request.setUpdateBy(currentUsername());
        requestMapper.updateErpPurchaseRequest(request);
        record(ErpStatus.BIZ_REQUEST, id, ErpStatus.ACTION_SUBMIT, from, ErpStatus.REQUEST_PENDING, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRequest(Long id, boolean pass, String opinion)
    {
        ErpPurchaseRequest request = requestMapper.selectById(id);
        if (request == null)
        {
            throw new ServiceException("采购申请不存在");
        }
        if (!ErpStatus.REQUEST_PENDING.equals(request.getRequestStatus()))
        {
            throw new ServiceException("仅待审批状态的采购申请可以审批");
        }
        String from = request.getRequestStatus();
        String to = pass ? ErpStatus.REQUEST_APPROVED : ErpStatus.REQUEST_REJECTED;
        request.setRequestStatus(to);
        request.setUpdateBy(currentUsername());
        requestMapper.updateErpPurchaseRequest(request);
        record(ErpStatus.BIZ_REQUEST, id, pass ? ErpStatus.ACTION_APPROVE : ErpStatus.ACTION_REJECT, from, to, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long id)
    {
        ErpPurchaseOrder order = orderMapper.selectById(id);
        if (order == null)
        {
            throw new ServiceException("采购订单不存在");
        }
        if (!ErpStatus.ORDER_DRAFT.equals(order.getOrderStatus()))
        {
            throw new ServiceException("仅草稿状态的采购订单可以提交");
        }
        ErpSupplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier == null)
        {
            throw new ServiceException("采购订单未指定有效供应商");
        }
        if (!ErpStatus.SUPPLIER_APPROVED.equals(supplier.getAdmissionStatus()))
        {
            throw new ServiceException("供应商未通过准入审核，不能下单");
        }
        String from = order.getOrderStatus();
        order.setOrderStatus(ErpStatus.ORDER_PENDING);
        order.setUpdateBy(currentUsername());
        orderMapper.updateErpPurchaseOrder(order);
        record(ErpStatus.BIZ_ORDER, id, ErpStatus.ACTION_SUBMIT, from, ErpStatus.ORDER_PENDING, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long id, boolean pass, String opinion)
    {
        ErpPurchaseOrder order = orderMapper.selectById(id);
        if (order == null)
        {
            throw new ServiceException("采购订单不存在");
        }
        if (!ErpStatus.ORDER_PENDING.equals(order.getOrderStatus()))
        {
            throw new ServiceException("仅待审批状态的采购订单可以审批");
        }
        String from = order.getOrderStatus();
        String to = pass ? ErpStatus.ORDER_APPROVED : ErpStatus.ORDER_REJECTED;
        order.setOrderStatus(to);
        order.setApprovalComment(opinion);
        order.setUpdateBy(currentUsername());
        orderMapper.updateErpPurchaseOrder(order);
        record(ErpStatus.BIZ_ORDER, id, pass ? ErpStatus.ACTION_APPROVE : ErpStatus.ACTION_REJECT, from, to, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String opinion)
    {
        ErpPurchaseOrder order = orderMapper.selectById(id);
        if (order == null)
        {
            throw new ServiceException("采购订单不存在");
        }
        if (ErpStatus.ORDER_RECEIVED.equals(order.getOrderStatus()) || ErpStatus.ORDER_CANCELLED.equals(order.getOrderStatus()))
        {
            throw new ServiceException("已全部到货或已取消的订单不能取消");
        }
        List<ErpPurchaseOrderItem> items = orderItemMapper.selectByParentId(id);
        for (ErpPurchaseOrderItem item : items)
        {
            if (zero(item.getReceivedQuantity()).signum() > 0)
            {
                throw new ServiceException("订单已有到货记录，请先冲销入库单后再取消");
            }
        }
        String from = order.getOrderStatus();
        order.setOrderStatus(ErpStatus.ORDER_CANCELLED);
        order.setApprovalComment(opinion);
        order.setUpdateBy(currentUsername());
        orderMapper.updateErpPurchaseOrder(order);
        record(ErpStatus.BIZ_ORDER, id, ErpStatus.ACTION_CANCEL, from, ErpStatus.ORDER_CANCELLED, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long id)
    {
        ErpReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null)
        {
            throw new ServiceException("入库单不存在");
        }
        if (ErpStatus.RECEIPT_CONFIRMED.equals(receipt.getReceiptStatus()))
        {
            return;
        }
        if (ErpStatus.RECEIPT_REVERSED.equals(receipt.getReceiptStatus()))
        {
            throw new ServiceException("已冲销的入库单不能再确认");
        }
        ErpPurchaseOrder order = orderMapper.selectById(receipt.getOrderId());
        if (order == null)
        {
            throw new ServiceException("入库单关联的采购订单不存在");
        }
        List<ErpReceiptItem> items = receiptItemMapper.selectByParentId(id);
        if (items == null || items.isEmpty())
        {
            throw new ServiceException("入库单没有明细，无法确认");
        }
        for (ErpReceiptItem item : items)
        {
            BigDecimal qualified = zero(item.getQualifiedQuantity());
            if (qualified.signum() <= 0)
            {
                continue;
            }
            applyStock(receipt.getWarehouseId(), item.getMaterialId(), qualified, zero(item.getUnitCost()),
                    ErpStatus.INVENTORY_RECEIPT, ErpStatus.BIZ_RECEIPT, receipt.getId(), item.getId());
            if (item.getOrderItemId() != null)
            {
                orderItemMapper.addReceivedQuantity(item.getOrderItemId(), qualified);
            }
        }
        receipt.setReceiptStatus(ErpStatus.RECEIPT_CONFIRMED);
        receipt.setConfirmedBy(SecurityUtils.getUserId());
        receipt.setConfirmedTime(new Date());
        receipt.setUpdateBy(currentUsername());
        receiptMapper.updateErpReceipt(receipt);
        refreshOrderStatus(order.getId());
        record(ErpStatus.BIZ_RECEIPT, id, ErpStatus.ACTION_APPROVE, ErpStatus.RECEIPT_DRAFT, ErpStatus.RECEIPT_CONFIRMED, "入库确认");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseReceipt(Long id, String opinion)
    {
        ErpReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null)
        {
            throw new ServiceException("入库单不存在");
        }
        if (!ErpStatus.RECEIPT_CONFIRMED.equals(receipt.getReceiptStatus()))
        {
            throw new ServiceException("仅已确认的入库单可以冲销");
        }
        List<ErpReceiptItem> items = receiptItemMapper.selectByParentId(id);
        for (ErpReceiptItem item : items)
        {
            BigDecimal qualified = zero(item.getQualifiedQuantity());
            if (qualified.signum() <= 0)
            {
                continue;
            }
            applyStock(receipt.getWarehouseId(), item.getMaterialId(), qualified.negate(), zero(item.getUnitCost()),
                    ErpStatus.INVENTORY_REVERSAL, ErpStatus.BIZ_RECEIPT, receipt.getId(), item.getId());
            if (item.getOrderItemId() != null)
            {
                orderItemMapper.addReceivedQuantity(item.getOrderItemId(), qualified.negate());
            }
        }
        receipt.setReceiptStatus(ErpStatus.RECEIPT_REVERSED);
        receipt.setUpdateBy(currentUsername());
        receiptMapper.updateErpReceipt(receipt);
        refreshOrderStatus(receipt.getOrderId());
        record(ErpStatus.BIZ_RECEIPT, id, ErpStatus.ACTION_REVERSE, ErpStatus.RECEIPT_CONFIRMED, ErpStatus.RECEIPT_REVERSED, opinion);
    }

    @Override
    public List<ErpApprovalRecord> selectApprovalRecords(String businessType, Long businessId)
    {
        ErpApprovalRecord query = new ErpApprovalRecord();
        query.setBusinessType(businessType);
        query.setBusinessId(businessId);
        return approvalRecordMapper.selectErpApprovalRecordList(query);
    }

    /**
     * 库存结转：写流水并更新余额
     */
    private void applyStock(Long warehouseId, Long materialId, BigDecimal change, BigDecimal unitCost,
            String businessType, String sourceType, Long sourceId, Long sourceItemId)
    {
        if (warehouseId == null || materialId == null)
        {
            throw new ServiceException("入库单缺少仓库或物料信息");
        }
        ErpInventoryBalance query = new ErpInventoryBalance();
        query.setWarehouseId(warehouseId);
        query.setMaterialId(materialId);
        List<ErpInventoryBalance> list = balanceMapper.selectErpInventoryBalanceList(query);
        ErpInventoryBalance balance = (list == null || list.isEmpty()) ? null : list.get(0);
        BigDecimal newQuantity;
        BigDecimal newAmount;
        if (balance == null)
        {
            if (change.signum() < 0)
            {
                throw new ServiceException("库存不足，无法冲销");
            }
            newQuantity = change;
            newAmount = change.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            balance = new ErpInventoryBalance();
            balance.setWarehouseId(warehouseId);
            balance.setMaterialId(materialId);
            balance.setAvailableQuantity(newQuantity);
            balance.setInventoryAmount(newAmount);
            balance.setAverageCost(unitCost.setScale(4, RoundingMode.HALF_UP));
            balance.setLastBusinessTime(new Date());
            balance.setCreateBy(currentUsername());
            balance.setRemark("库存初始化");
            balanceMapper.insertErpInventoryBalance(balance);
        }
        else
        {
            newQuantity = zero(balance.getAvailableQuantity()).add(change);
            if (newQuantity.signum() < 0)
            {
                throw new ServiceException("库存不足，无法冲销");
            }
            newAmount = zero(balance.getInventoryAmount()).add(change.multiply(unitCost)).setScale(2, RoundingMode.HALF_UP);
            if (newAmount.signum() < 0)
            {
                newAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal averageCost = newQuantity.signum() == 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : newAmount.divide(newQuantity, 4, RoundingMode.HALF_UP);
            balance.setAvailableQuantity(newQuantity);
            balance.setInventoryAmount(newAmount);
            balance.setAverageCost(averageCost);
            balance.setLastBusinessTime(new Date());
            balance.setUpdateBy(currentUsername());
            balanceMapper.updateErpInventoryBalance(balance);
        }
        ErpInventoryLedger ledger = new ErpInventoryLedger();
        ledger.setWarehouseId(warehouseId);
        ledger.setMaterialId(materialId);
        ledger.setBusinessType(businessType);
        ledger.setSourceType(sourceType);
        ledger.setSourceId(sourceId);
        ledger.setSourceItemId(sourceItemId);
        ledger.setChangeQuantity(change);
        ledger.setUnitCost(unitCost);
        ledger.setBalanceQuantity(balance.getAvailableQuantity());
        ledger.setOccurredTime(new Date());
        ledger.setCreateBy(currentUsername());
        ledgerMapper.insertErpInventoryLedger(ledger);
    }

    /**
     * 依据明细累计到货数量刷新订单状态
     */
    private void refreshOrderStatus(Long orderId)
    {
        if (orderId == null)
        {
            return;
        }
        ErpPurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null)
        {
            return;
        }
        if (ErpStatus.ORDER_CANCELLED.equals(order.getOrderStatus()))
        {
            return;
        }
        List<ErpPurchaseOrderItem> items = orderItemMapper.selectByParentId(orderId);
        boolean anyReceived = false;
        boolean allReceived = !items.isEmpty();
        for (ErpPurchaseOrderItem item : items)
        {
            BigDecimal received = zero(item.getReceivedQuantity());
            BigDecimal quantity = zero(item.getQuantity());
            if (received.signum() > 0)
            {
                anyReceived = true;
            }
            if (received.compareTo(quantity) < 0)
            {
                allReceived = false;
            }
        }
        String status = allReceived ? ErpStatus.ORDER_RECEIVED
                : (anyReceived ? ErpStatus.ORDER_PARTIALLY_RECEIVED : ErpStatus.ORDER_APPROVED);
        if (!status.equals(order.getOrderStatus()))
        {
            order.setOrderStatus(status);
            order.setUpdateBy(currentUsername());
            orderMapper.updateErpPurchaseOrder(order);
        }
    }

    private void record(String businessType, Long businessId, String action, String fromStatus, String toStatus, String opinion)
    {
        ErpApprovalRecord record = new ErpApprovalRecord();
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setOpinion(opinion);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(currentNickName());
        record.setOperateTime(new Date());
        record.setCreateBy(currentUsername());
        approvalRecordMapper.insertErpApprovalRecord(record);
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }

    private String currentNickName()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getUser() != null && StringUtils.isNotEmpty(loginUser.getUser().getNickName()))
            {
                return loginUser.getUser().getNickName();
            }
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }

    private BigDecimal zero(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}

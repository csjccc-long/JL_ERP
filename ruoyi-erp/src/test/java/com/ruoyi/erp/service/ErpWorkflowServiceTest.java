package com.ruoyi.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
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

/**
 * ERP 业务状态机与库存结转单元测试
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class ErpWorkflowServiceTest
{
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpContractMapper contractMapper;
    @Mock
    private ErpPurchaseRequestMapper requestMapper;
    @Mock
    private ErpPurchaseOrderMapper orderMapper;
    @Mock
    private ErpPurchaseOrderItemMapper orderItemMapper;
    @Mock
    private ErpReceiptMapper receiptMapper;
    @Mock
    private ErpReceiptItemMapper receiptItemMapper;
    @Mock
    private ErpInventoryBalanceMapper balanceMapper;
    @Mock
    private ErpInventoryLedgerMapper ledgerMapper;
    @Mock
    private ErpApprovalRecordMapper approvalRecordMapper;

    @InjectMocks
    private com.ruoyi.erp.service.impl.ErpWorkflowServiceImpl workflowService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    public void setUp()
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(201L);
        loginUser.setDeptId(200L);
        SysUser user = new SysUser();
        user.setUserName("caigou");
        user.setNickName("陈采购");
        loginUser.setUser(user);
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(201L);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("caigou");
        securityUtils.when(SecurityUtils::getLoginUser).thenReturn(loginUser);
        securityUtils.when(() -> SecurityUtils.isAdmin(any(Long.class))).thenReturn(false);
    }

    @AfterEach
    public void tearDown()
    {
        securityUtils.close();
    }

    private ErpSupplier supplier(Long id, String status)
    {
        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(id);
        supplier.setSupplierCode("SUP-0001");
        supplier.setAdmissionStatus(status);
        supplier.setRiskLevel("LOW");
        return supplier;
    }

    private ErpPurchaseOrder order(Long id, String status, Long supplierId)
    {
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setId(id);
        order.setOrderNo("PO-2026-0001");
        order.setOrderStatus(status);
        order.setSupplierId(supplierId);
        return order;
    }

    private ErpPurchaseOrderItem orderItem(Long id, String quantity, String received)
    {
        ErpPurchaseOrderItem item = new ErpPurchaseOrderItem();
        item.setId(id);
        item.setMaterialId(100L);
        item.setQuantity(new BigDecimal(quantity));
        item.setReceivedQuantity(new BigDecimal(received));
        return item;
    }

    @Test
    @DisplayName("供应商准入：状态非法时拒绝")
    public void auditSupplier_invalidStatus_throws()
    {
        when(supplierMapper.selectById(1L)).thenReturn(supplier(1L, ErpStatus.SUPPLIER_PENDING));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> workflowService.auditSupplier(1L, "UNKNOWN", "LOW", "同意"));
        assertEquals("准入状态不合法", ex.getMessage());
        verify(supplierMapper, never()).updateErpSupplier(any(ErpSupplier.class));
    }

    @Test
    @DisplayName("供应商准入：通过时写入准入时间与审批记录")
    public void auditSupplier_approved_setsAdmissionTime()
    {
        ErpSupplier supplier = supplier(1L, ErpStatus.SUPPLIER_PENDING);
        when(supplierMapper.selectById(1L)).thenReturn(supplier);
        workflowService.auditSupplier(1L, ErpStatus.SUPPLIER_APPROVED, "MEDIUM", "资料齐全");
        assertEquals(ErpStatus.SUPPLIER_APPROVED, supplier.getAdmissionStatus());
        assertEquals("MEDIUM", supplier.getRiskLevel());
        assertTrue(supplier.getAdmissionTime() != null);
        ArgumentCaptor<ErpApprovalRecord> captor = ArgumentCaptor.forClass(ErpApprovalRecord.class);
        verify(approvalRecordMapper).insertErpApprovalRecord(captor.capture());
        assertEquals(ErpStatus.BIZ_SUPPLIER, captor.getValue().getBusinessType());
        assertEquals(ErpStatus.ACTION_APPROVE, captor.getValue().getAction());
        assertEquals("资料齐全", captor.getValue().getOpinion());
    }

    @Test
    @DisplayName("合同审批：待审批通过后生效，驳回回到草稿")
    public void approveContract_transitions()
    {
        ErpContract contract = new ErpContract();
        contract.setId(1L);
        contract.setContractStatus(ErpStatus.CONTRACT_PENDING);
        when(contractMapper.selectById(1L)).thenReturn(contract);
        workflowService.approveContract(1L, true, "同意");
        assertEquals(ErpStatus.CONTRACT_ACTIVE, contract.getContractStatus());

        ErpContract rejected = new ErpContract();
        rejected.setId(2L);
        rejected.setContractStatus(ErpStatus.CONTRACT_PENDING);
        when(contractMapper.selectById(2L)).thenReturn(rejected);
        workflowService.approveContract(2L, false, "条款需修改");
        assertEquals(ErpStatus.CONTRACT_DRAFT, rejected.getContractStatus());
    }

    @Test
    @DisplayName("合同审批：非待审批状态拒绝审批")
    public void approveContract_wrongState_throws()
    {
        ErpContract contract = new ErpContract();
        contract.setId(1L);
        contract.setContractStatus(ErpStatus.CONTRACT_DRAFT);
        when(contractMapper.selectById(1L)).thenReturn(contract);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> workflowService.approveContract(1L, true, "同意"));
        assertEquals("仅待审批状态的合同可以审批", ex.getMessage());
    }

    @Test
    @DisplayName("合同提交：仅草稿可提交")
    public void submitContract_onlyDraft()
    {
        ErpContract contract = new ErpContract();
        contract.setId(1L);
        contract.setContractStatus(ErpStatus.CONTRACT_ACTIVE);
        when(contractMapper.selectById(1L)).thenReturn(contract);
        ServiceException ex = assertThrows(ServiceException.class, () -> workflowService.submitContract(1L));
        assertEquals("仅草稿状态的合同可以提交审批", ex.getMessage());
    }

    @Test
    @DisplayName("采购申请审批：通过置 APPROVED，驳回置 REJECTED")
    public void approveRequest_transitions()
    {
        ErpPurchaseRequest request = new ErpPurchaseRequest();
        request.setId(1L);
        request.setRequestStatus(ErpStatus.REQUEST_PENDING);
        when(requestMapper.selectById(1L)).thenReturn(request);
        workflowService.approveRequest(1L, true, "同意采购");
        assertEquals(ErpStatus.REQUEST_APPROVED, request.getRequestStatus());

        ErpPurchaseRequest rejected = new ErpPurchaseRequest();
        rejected.setId(2L);
        rejected.setRequestStatus(ErpStatus.REQUEST_PENDING);
        when(requestMapper.selectById(2L)).thenReturn(rejected);
        workflowService.approveRequest(2L, false, "预算不足");
        assertEquals(ErpStatus.REQUEST_REJECTED, rejected.getRequestStatus());
    }

    @Test
    @DisplayName("订单提交：供应商未通过准入时拒绝下单")
    public void submitOrder_unapprovedSupplier_throws()
    {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, ErpStatus.ORDER_DRAFT, 9L));
        when(supplierMapper.selectById(9L)).thenReturn(supplier(9L, ErpStatus.SUPPLIER_PENDING));
        ServiceException ex = assertThrows(ServiceException.class, () -> workflowService.submitOrder(1L));
        assertEquals("供应商未通过准入审核，不能下单", ex.getMessage());
        verify(orderMapper, never()).updateErpPurchaseOrder(any(ErpPurchaseOrder.class));
    }

    @Test
    @DisplayName("订单提交：供应商已准入时进入待审批")
    public void submitOrder_approvedSupplier_setsPending()
    {
        ErpPurchaseOrder order = order(1L, ErpStatus.ORDER_DRAFT, 9L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(supplierMapper.selectById(9L)).thenReturn(supplier(9L, ErpStatus.SUPPLIER_APPROVED));
        workflowService.submitOrder(1L);
        assertEquals(ErpStatus.ORDER_PENDING, order.getOrderStatus());
        verify(orderMapper).updateErpPurchaseOrder(order);
    }

    @Test
    @DisplayName("订单驳回：记录审批意见")
    public void approveOrder_reject_recordsComment()
    {
        ErpPurchaseOrder order = order(1L, ErpStatus.ORDER_PENDING, 9L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        workflowService.approveOrder(1L, false, "价格异常");
        assertEquals(ErpStatus.ORDER_REJECTED, order.getOrderStatus());
        assertEquals("价格异常", order.getApprovalComment());
    }

    @Test
    @DisplayName("订单取消：已有到货记录时拒绝取消")
    public void cancelOrder_withReceivedItems_throws()
    {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, ErpStatus.ORDER_APPROVED, 9L));
        List<ErpPurchaseOrderItem> items = new ArrayList<ErpPurchaseOrderItem>();
        items.add(orderItem(11L, "10", "2"));
        when(orderItemMapper.selectByParentId(1L)).thenReturn(items);
        ServiceException ex = assertThrows(ServiceException.class, () -> workflowService.cancelOrder(1L, "供应商断供"));
        assertEquals("订单已有到货记录，请先冲销入库单后再取消", ex.getMessage());
    }

    @Test
    @DisplayName("订单取消：无到货时置 CANCELLED")
    public void cancelOrder_withoutReceipt_setsCancelled()
    {
        ErpPurchaseOrder order = order(1L, ErpStatus.ORDER_APPROVED, 9L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectByParentId(1L)).thenReturn(Collections.singletonList(orderItem(11L, "10", "0")));
        workflowService.cancelOrder(1L, "计划变更");
        assertEquals(ErpStatus.ORDER_CANCELLED, order.getOrderStatus());
    }

    private ErpReceipt receipt(Long id, String status)
    {
        ErpReceipt receipt = new ErpReceipt();
        receipt.setId(id);
        receipt.setReceiptNo("RC-2026-0001");
        receipt.setReceiptStatus(status);
        receipt.setOrderId(10L);
        receipt.setWarehouseId(1L);
        return receipt;
    }

    private ErpReceiptItem receiptItem(Long id, String qualified, String cost, Long orderItemId)
    {
        ErpReceiptItem item = new ErpReceiptItem();
        item.setId(id);
        item.setMaterialId(100L);
        item.setQualifiedQuantity(new BigDecimal(qualified));
        item.setUnitCost(new BigDecimal(cost));
        item.setOrderItemId(orderItemId);
        return item;
    }

    @Test
    @DisplayName("入库确认：重复确认幂等，不产生库存流水")
    public void confirmReceipt_idempotent()
    {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(1L, ErpStatus.RECEIPT_CONFIRMED));
        workflowService.confirmReceipt(1L);
        verifyNoInteractions(balanceMapper, ledgerMapper);
        verify(receiptMapper, never()).updateErpReceipt(any(ErpReceipt.class));
    }

    @Test
    @DisplayName("入库确认：写入余额与流水，并按到货数量刷新订单状态")
    public void confirmReceipt_createsBalanceAndLedger()
    {
        ErpReceipt receipt = receipt(1L, ErpStatus.RECEIPT_DRAFT);
        when(receiptMapper.selectById(1L)).thenReturn(receipt);
        when(orderMapper.selectById(10L)).thenReturn(order(10L, ErpStatus.ORDER_APPROVED, 9L));
        when(receiptItemMapper.selectByParentId(1L))
                .thenReturn(Collections.singletonList(receiptItem(1001L, "5", "10", 11L)));
        when(balanceMapper.selectErpInventoryBalanceList(any(ErpInventoryBalance.class)))
                .thenReturn(new ArrayList<ErpInventoryBalance>());
        when(orderItemMapper.selectByParentId(10L)).thenReturn(Collections.singletonList(orderItem(11L, "5", "5")));

        workflowService.confirmReceipt(1L);

        ArgumentCaptor<ErpInventoryBalance> balanceCaptor = ArgumentCaptor.forClass(ErpInventoryBalance.class);
        verify(balanceMapper).insertErpInventoryBalance(balanceCaptor.capture());
        assertEquals(0, new BigDecimal("5").compareTo(balanceCaptor.getValue().getAvailableQuantity()));
        assertEquals(0, new BigDecimal("50.00").compareTo(balanceCaptor.getValue().getInventoryAmount()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(balanceCaptor.getValue().getAverageCost()));

        ArgumentCaptor<ErpInventoryLedger> ledgerCaptor = ArgumentCaptor.forClass(ErpInventoryLedger.class);
        verify(ledgerMapper).insertErpInventoryLedger(ledgerCaptor.capture());
        assertEquals(ErpStatus.INVENTORY_RECEIPT, ledgerCaptor.getValue().getBusinessType());
        assertEquals(0, new BigDecimal("5").compareTo(ledgerCaptor.getValue().getChangeQuantity()));
        assertEquals(0, new BigDecimal("5").compareTo(ledgerCaptor.getValue().getBalanceQuantity()));

        verify(orderItemMapper).addReceivedQuantity(11L, new BigDecimal("5"));
        assertEquals(ErpStatus.RECEIPT_CONFIRMED, receipt.getReceiptStatus());
        verify(orderMapper).updateErpPurchaseOrder(any(ErpPurchaseOrder.class));
    }

    @Test
    @DisplayName("入库冲销：库存不足时回滚并抛出异常")
    public void reverseReceipt_insufficientStock_throws()
    {
        ErpReceipt receipt = receipt(1L, ErpStatus.RECEIPT_CONFIRMED);
        when(receiptMapper.selectById(1L)).thenReturn(receipt);
        when(receiptItemMapper.selectByParentId(1L))
                .thenReturn(Collections.singletonList(receiptItem(1001L, "5", "10", 11L)));
        ErpInventoryBalance balance = new ErpInventoryBalance();
        balance.setId(1L);
        balance.setWarehouseId(1L);
        balance.setMaterialId(100L);
        balance.setAvailableQuantity(new BigDecimal("1"));
        balance.setInventoryAmount(new BigDecimal("10.00"));
        when(balanceMapper.selectErpInventoryBalanceList(any(ErpInventoryBalance.class)))
                .thenReturn(Collections.singletonList(balance));

        ServiceException ex = assertThrows(ServiceException.class, () -> workflowService.reverseReceipt(1L, "录错"));
        assertEquals("库存不足，无法冲销", ex.getMessage());
        verify(balanceMapper, never()).updateErpInventoryBalance(any(ErpInventoryBalance.class));
        verify(ledgerMapper, never()).insertErpInventoryLedger(any(ErpInventoryLedger.class));
    }

    @Test
    @DisplayName("入库冲销：已确认入库单冲销后状态为 REVERSED")
    public void reverseReceipt_success()
    {
        ErpReceipt receipt = receipt(1L, ErpStatus.RECEIPT_CONFIRMED);
        when(receiptMapper.selectById(1L)).thenReturn(receipt);
        when(receiptItemMapper.selectByParentId(1L))
                .thenReturn(Collections.singletonList(receiptItem(1001L, "2", "10", 11L)));
        ErpInventoryBalance balance = new ErpInventoryBalance();
        balance.setId(1L);
        balance.setWarehouseId(1L);
        balance.setMaterialId(100L);
        balance.setAvailableQuantity(new BigDecimal("10"));
        balance.setInventoryAmount(new BigDecimal("100.00"));
        when(balanceMapper.selectErpInventoryBalanceList(any(ErpInventoryBalance.class)))
                .thenReturn(Collections.singletonList(balance));
        when(orderMapper.selectById(10L)).thenReturn(order(10L, ErpStatus.ORDER_RECEIVED, 9L));

        workflowService.reverseReceipt(1L, "到货数量录错");

        assertEquals(ErpStatus.RECEIPT_REVERSED, receipt.getReceiptStatus());
        assertEquals(0, new BigDecimal("8").compareTo(balance.getAvailableQuantity()));
        verify(orderItemMapper).addReceivedQuantity(eq(11L), eq(new BigDecimal("-2")));
        assertEquals(ErpStatus.ORDER_RECEIVED, order(10L, ErpStatus.ORDER_RECEIVED, 9L).getOrderStatus());
    }
}

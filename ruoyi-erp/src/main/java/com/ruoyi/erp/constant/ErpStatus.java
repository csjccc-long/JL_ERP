package com.ruoyi.erp.constant;

/**
 * ERP 业务状态常量
 *
 * @author ruoyi
 */
public class ErpStatus
{
    private ErpStatus()
    {
    }

    /** 供应商准入状态 */
    public static final String SUPPLIER_PENDING = "PENDING";
    public static final String SUPPLIER_APPROVED = "APPROVED";
    public static final String SUPPLIER_REJECTED = "REJECTED";
    public static final String SUPPLIER_SUSPENDED = "SUSPENDED";

    /** 合同状态 */
    public static final String CONTRACT_DRAFT = "DRAFT";
    public static final String CONTRACT_PENDING = "PENDING_APPROVAL";
    public static final String CONTRACT_ACTIVE = "ACTIVE";
    public static final String CONTRACT_EXPIRED = "EXPIRED";
    public static final String CONTRACT_TERMINATED = "TERMINATED";

    /** 采购申请状态 */
    public static final String REQUEST_DRAFT = "DRAFT";
    public static final String REQUEST_PENDING = "PENDING_APPROVAL";
    public static final String REQUEST_APPROVED = "APPROVED";
    public static final String REQUEST_REJECTED = "REJECTED";
    public static final String REQUEST_CLOSED = "CLOSED";

    /** 采购订单状态 */
    public static final String ORDER_DRAFT = "DRAFT";
    public static final String ORDER_PENDING = "PENDING_APPROVAL";
    public static final String ORDER_APPROVED = "APPROVED";
    public static final String ORDER_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String ORDER_RECEIVED = "RECEIVED";
    public static final String ORDER_REJECTED = "REJECTED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    /** 入库单状态 */
    public static final String RECEIPT_DRAFT = "DRAFT";
    public static final String RECEIPT_CONFIRMED = "CONFIRMED";
    public static final String RECEIPT_REVERSED = "REVERSED";

    /** 库存业务类型 */
    public static final String INVENTORY_RECEIPT = "RECEIPT";
    public static final String INVENTORY_REVERSAL = "REVERSAL";

    /** 业务类型 */
    public static final String BIZ_SUPPLIER = "SUPPLIER";
    public static final String BIZ_CONTRACT = "CONTRACT";
    public static final String BIZ_REQUEST = "PURCHASE_REQUEST";
    public static final String BIZ_ORDER = "PURCHASE_ORDER";
    public static final String BIZ_RECEIPT = "RECEIPT";

    /** 审批动作 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ACTION_TERMINATE = "TERMINATE";
    public static final String ACTION_REVERSE = "REVERSE";
}

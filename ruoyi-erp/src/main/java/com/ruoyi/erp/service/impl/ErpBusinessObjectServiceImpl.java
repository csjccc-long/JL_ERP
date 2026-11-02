package com.ruoyi.erp.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.service.IBusinessObjectService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.erp.constant.ErpStatus;
import com.ruoyi.erp.domain.ErpContract;
import com.ruoyi.erp.domain.ErpPurchaseOrder;
import com.ruoyi.erp.domain.ErpPurchaseRequest;
import com.ruoyi.erp.domain.ErpReceipt;
import com.ruoyi.erp.domain.ErpSupplier;
import com.ruoyi.erp.mapper.ErpBusinessMapper;
import com.ruoyi.erp.mapper.ErpContractMapper;
import com.ruoyi.erp.mapper.ErpPurchaseOrderMapper;
import com.ruoyi.erp.mapper.ErpPurchaseRequestMapper;
import com.ruoyi.erp.mapper.ErpReceiptMapper;
import com.ruoyi.erp.mapper.ErpSupplierMapper;

/**
 * 业务对象可见性与结构化上下文实现
 *
 * <p>可见性规则：系统管理员可见全部；否则要求业务对象所属部门在当前用户数据范围内，
 * 或当前用户为该对象的负责人/申请人。</p>
 *
 * @author ruoyi
 */
@Service
public class ErpBusinessObjectServiceImpl implements IBusinessObjectService
{
    @Autowired
    private ErpSupplierMapper supplierMapper;

    @Autowired
    private ErpContractMapper contractMapper;

    @Autowired
    private ErpPurchaseRequestMapper purchaseRequestMapper;

    @Autowired
    private ErpPurchaseOrderMapper purchaseOrderMapper;

    @Autowired
    private ErpReceiptMapper receiptMapper;

    @Autowired
    private ErpBusinessMapper businessMapper;

    @Override
    public boolean supports(String businessType)
    {
        return ErpStatus.BIZ_SUPPLIER.equals(businessType) || ErpStatus.BIZ_CONTRACT.equals(businessType)
                || ErpStatus.BIZ_REQUEST.equals(businessType) || ErpStatus.BIZ_ORDER.equals(businessType)
                || ErpStatus.BIZ_RECEIPT.equals(businessType);
    }

    @Override
    public boolean canView(String businessType, Long businessId)
    {
        if (businessId == null || !supports(businessType))
        {
            return false;
        }
        Long deptId;
        Long ownerId;
        if (ErpStatus.BIZ_SUPPLIER.equals(businessType))
        {
            ErpSupplier supplier = supplierMapper.selectById(businessId);
            if (supplier == null) return false;
            deptId = supplier.getDeptId();
            ownerId = null;
        }
        else if (ErpStatus.BIZ_CONTRACT.equals(businessType))
        {
            ErpContract contract = contractMapper.selectById(businessId);
            if (contract == null) return false;
            deptId = contract.getDeptId();
            ownerId = contract.getOwnerId();
        }
        else if (ErpStatus.BIZ_REQUEST.equals(businessType))
        {
            ErpPurchaseRequest request = purchaseRequestMapper.selectById(businessId);
            if (request == null) return false;
            deptId = request.getDeptId();
            ownerId = request.getApplicantId();
        }
        else if (ErpStatus.BIZ_ORDER.equals(businessType))
        {
            ErpPurchaseOrder order = purchaseOrderMapper.selectById(businessId);
            if (order == null) return false;
            deptId = order.getDeptId();
            ownerId = null;
        }
        else
        {
            ErpReceipt receipt = receiptMapper.selectById(businessId);
            if (receipt == null) return false;
            deptId = receipt.getDeptId();
            ownerId = receipt.getConfirmedBy();
        }
        return inDataScope(deptId, ownerId);
    }

    private boolean inDataScope(Long deptId, Long ownerId)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null)
        {
            throw new ServiceException("获取用户信息异常");
        }
        Long userId = loginUser.getUserId();
        if (SecurityUtils.isAdmin(userId))
        {
            return true;
        }
        if (ownerId != null && ownerId.equals(userId))
        {
            return true;
        }
        Long userDeptId = loginUser.getDeptId();
        if (deptId == null || userDeptId == null)
        {
            return false;
        }
        if (deptId.equals(userDeptId))
        {
            return true;
        }
        List<Long> scope = businessMapper.selectDeptScope(userDeptId);
        return scope != null && scope.contains(deptId);
    }

    @Override
    public Map<String, Object> loadSummary(String businessType, Long businessId)
    {
        if (businessId == null || !canView(businessType, businessId))
        {
            return null;
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("businessType", businessType);
        summary.put("businessId", businessId);
        if (ErpStatus.BIZ_SUPPLIER.equals(businessType))
        {
            ErpSupplier supplier = supplierMapper.selectById(businessId);
            summary.put("supplierCode", supplier.getSupplierCode());
            summary.put("supplierName", supplier.getSupplierName());
            summary.put("admissionStatus", supplier.getAdmissionStatus());
            summary.put("riskLevel", supplier.getRiskLevel());
        }
        else if (ErpStatus.BIZ_CONTRACT.equals(businessType))
        {
            ErpContract contract = contractMapper.selectById(businessId);
            summary.put("contractNo", contract.getContractNo());
            summary.put("contractName", contract.getContractName());
            summary.put("contractStatus", contract.getContractStatus());
            summary.put("contractAmount", contract.getContractAmount());
            summary.put("startDate", contract.getStartDate());
            summary.put("endDate", contract.getEndDate());
        }
        else if (ErpStatus.BIZ_REQUEST.equals(businessType))
        {
            ErpPurchaseRequest request = purchaseRequestMapper.selectById(businessId);
            summary.put("requestNo", request.getRequestNo());
            summary.put("title", request.getTitle());
            summary.put("requestStatus", request.getRequestStatus());
            summary.put("estimatedAmount", request.getEstimatedAmount());
        }
        else if (ErpStatus.BIZ_ORDER.equals(businessType))
        {
            ErpPurchaseOrder order = purchaseOrderMapper.selectById(businessId);
            summary.put("orderNo", order.getOrderNo());
            summary.put("orderStatus", order.getOrderStatus());
            summary.put("totalAmount", order.getTotalAmount());
            summary.put("orderDate", order.getOrderDate());
        }
        else
        {
            ErpReceipt receipt = receiptMapper.selectById(businessId);
            summary.put("receiptNo", receipt.getReceiptNo());
            summary.put("receiptStatus", receipt.getReceiptStatus());
            summary.put("warehouseName", receipt.getWarehouseName());
            summary.put("receiptDate", receipt.getReceiptDate());
        }
        return summary;
    }
}

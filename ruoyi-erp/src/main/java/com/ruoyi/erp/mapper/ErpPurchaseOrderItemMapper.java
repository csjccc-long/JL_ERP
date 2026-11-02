package com.ruoyi.erp.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.erp.domain.ErpPurchaseOrderItem;

public interface ErpPurchaseOrderItemMapper
{
    List<ErpPurchaseOrderItem> selectByParentId(Long orderId);

    int insertErpPurchaseOrderItem(ErpPurchaseOrderItem erpPurchaseOrderItem);

    int deleteByParentId(Long orderId);

    /**
     * 累加到货数量
     *
     * @param id 订单明细ID
     * @param quantity 到货数量（可为负数用于冲销）
     * @return 影响行数
     */
    int addReceivedQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);
}
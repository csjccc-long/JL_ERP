package com.ruoyi.erp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.erp.mapper.ErpSequenceMapper;

/**
 * 单号生成器单元测试
 */
@ExtendWith(MockitoExtension.class)
public class ErpNoGeneratorTest
{
    @Mock
    private ErpSequenceMapper sequenceMapper;

    @InjectMocks
    private ErpNoGenerator generator;

    private String year()
    {
        return new SimpleDateFormat("yyyy").format(new Date());
    }

    @Test
    @DisplayName("空库时从 0001 开始")
    public void next_emptyDatabase_startsAtOne()
    {
        when(sequenceMapper.selectMaxNo("erp_supplier", "supplier_code", "SUP-")).thenReturn(null);
        assertEquals("SUP-0001", generator.supplierCode());
    }

    @Test
    @DisplayName("在最大单号基础上递增")
    public void next_existingMax_increments()
    {
        when(sequenceMapper.selectMaxNo("erp_supplier", "supplier_code", "SUP-")).thenReturn("SUP-0012");
        assertEquals("SUP-0013", generator.supplierCode());
    }

    @Test
    @DisplayName("末段不是数字时回落为 0001")
    public void next_nonNumericTail_startsAtOne()
    {
        when(sequenceMapper.selectMaxNo("erp_supplier", "supplier_code", "SUP-")).thenReturn("SUP-ABC");
        assertEquals("SUP-0001", generator.supplierCode());
    }

    @Test
    @DisplayName("按年度前缀生成合同/申请/订单/入库单号")
    public void yearlyPrefixes()
    {
        String y = year();
        when(sequenceMapper.selectMaxNo("erp_contract", "contract_no", "HT-" + y + "-")).thenReturn(null);
        when(sequenceMapper.selectMaxNo("erp_purchase_request", "request_no", "PR-" + y + "-")).thenReturn("PR-" + y + "-0009");
        when(sequenceMapper.selectMaxNo("erp_purchase_order", "order_no", "PO-" + y + "-")).thenReturn(null);
        when(sequenceMapper.selectMaxNo("erp_receipt", "receipt_no", "RC-" + y + "-")).thenReturn(null);
        assertEquals("HT-" + y + "-0001", generator.contractNo());
        assertEquals("PR-" + y + "-0010", generator.requestNo());
        assertEquals("PO-" + y + "-0001", generator.orderNo());
        assertEquals("RC-" + y + "-0001", generator.receiptNo());
    }

    @Test
    @DisplayName("序号超过四位时自然增长")
    public void next_overflowWidth_grows()
    {
        when(sequenceMapper.selectMaxNo("erp_supplier", "supplier_code", "SUP-")).thenReturn("SUP-9999");
        assertEquals("SUP-10000", generator.supplierCode());
    }
}

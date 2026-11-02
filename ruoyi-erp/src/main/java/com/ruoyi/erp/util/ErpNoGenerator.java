package com.ruoyi.erp.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ErpSequenceMapper;

/**
 * ERP 单号生成器（按前缀 + 序号递增）
 *
 * @author ruoyi
 */
@Component
public class ErpNoGenerator
{
    @Autowired
    private ErpSequenceMapper sequenceMapper;

    /**
     * 生成下一个单号
     *
     * @param table 表名（内部常量）
     * @param column 列名（内部常量）
     * @param prefix 前缀，例如 HT-2026-
     * @param width 序号位数
     * @return 单号
     */
    public String next(String table, String column, String prefix, int width)
    {
        String max = sequenceMapper.selectMaxNo(table, column, prefix);
        int serial = 1;
        if (StringUtils.isNotEmpty(max))
        {
            int idx = max.lastIndexOf('-');
            String tail = idx >= 0 ? max.substring(idx + 1) : "";
            try
            {
                serial = Integer.parseInt(tail) + 1;
            }
            catch (NumberFormatException e)
            {
                serial = 1;
            }
        }
        return prefix + String.format("%0" + width + "d", serial);
    }

    /** 供应商编码 SUP-0001 */
    public String supplierCode()
    {
        return next("erp_supplier", "supplier_code", "SUP-", 4);
    }

    /** 合同编号 HT-2026-0001 */
    public String contractNo()
    {
        return next("erp_contract", "contract_no", "HT-" + year() + "-", 4);
    }

    /** 采购申请单号 PR-2026-0001 */
    public String requestNo()
    {
        return next("erp_purchase_request", "request_no", "PR-" + year() + "-", 4);
    }

    /** 采购订单号 PO-2026-0001 */
    public String orderNo()
    {
        return next("erp_purchase_order", "order_no", "PO-" + year() + "-", 4);
    }

    /** 入库单号 RC-2026-0001 */
    public String receiptNo()
    {
        return next("erp_receipt", "receipt_no", "RC-" + year() + "-", 4);
    }

    private String year()
    {
        return new SimpleDateFormat("yyyy").format(new Date());
    }
}

package com.ruoyi.common.core.service;

import java.util.Map;

/**
 * 业务对象可见性与结构化上下文服务
 *
 * <p>由业务模块（如 ERP）实现，知识模块通过该接口校验业务对象可见性并读取只读上下文，
 * 从而保持“知识模块不依赖业务实体、业务模块不依赖知识模块”的双向解耦。</p>
 *
 * @author ruoyi
 */
public interface IBusinessObjectService
{
    /**
     * 是否支持该业务类型
     *
     * @param businessType 业务类型标识（如 SUPPLIER、CONTRACT、ORDER）
     * @return 支持返回 true
     */
    boolean supports(String businessType);

    /**
     * 当前登录用户是否可见该业务对象
     *
     * @param businessType 业务类型标识
     * @param businessId 业务对象ID
     * @return 可见返回 true
     */
    boolean canView(String businessType, Long businessId);

    /**
     * 读取业务对象只读摘要，用于组装 ERP_CONTEXT 只读块
     *
     * @param businessType 业务类型标识
     * @param businessId 业务对象ID
     * @return 摘要键值对，不存在或不可见时返回 null
     */
    Map<String, Object> loadSummary(String businessType, Long businessId);
}

package com.ruoyi.erp.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * ERP 数据完整性异常处理（不向前端暴露 SQL 与堆栈）
 *
 * @author ruoyi
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ErpExceptionHandler
{
    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult handleDuplicateKey(DuplicateKeyException e)
    {
        return AjaxResult.error("数据已存在（编码或单号重复），请检查后重试");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public AjaxResult handleDataIntegrity(DataIntegrityViolationException e)
    {
        return AjaxResult.error("数据不完整或关联数据仍被引用，操作未生效");
    }
}

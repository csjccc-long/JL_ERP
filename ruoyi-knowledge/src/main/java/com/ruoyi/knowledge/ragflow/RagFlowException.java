package com.ruoyi.knowledge.ragflow;

/**
 * RAGFlow 调用异常（对外统一转换为服务不可用，不返回堆栈）
 *
 * @author ruoyi
 */
public class RagFlowException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public RagFlowException(String message)
    {
        super(message);
    }

    public RagFlowException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

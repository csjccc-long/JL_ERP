package com.ruoyi.knowledge.ragflow;

import com.alibaba.fastjson2.JSONArray;

/**
 * RAGFlow 流式回答回调
 *
 * @author ruoyi
 */
public interface ChatStreamHandler
{
    /**
     * 增量回答
     *
     * @param delta 增量文本
     */
    void onDelta(String delta);

    /**
     * 引用片段
     *
     * @param chunks 引用分块数组
     */
    void onReference(JSONArray chunks);

    /**
     * 流式结束
     */
    void onDone();
}

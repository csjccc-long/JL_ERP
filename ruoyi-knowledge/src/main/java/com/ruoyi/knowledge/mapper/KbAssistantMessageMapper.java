package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbAssistantMessage;

/**
 * 助手消息Mapper接口
 *
 * @author ruoyi
 */
public interface KbAssistantMessageMapper
{
    KbAssistantMessage selectById(@Param("id") Long id);

    List<KbAssistantMessage> selectList(KbAssistantMessage query);

    int insert(KbAssistantMessage entity);

    int update(KbAssistantMessage entity);

    int deleteById(@Param("id") Long id);
}

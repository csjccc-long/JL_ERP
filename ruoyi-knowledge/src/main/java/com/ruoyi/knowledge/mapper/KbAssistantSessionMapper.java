package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbAssistantSession;

/**
 * 助手会话Mapper接口
 *
 * @author ruoyi
 */
public interface KbAssistantSessionMapper
{
    KbAssistantSession selectById(@Param("id") Long id);

    List<KbAssistantSession> selectList(KbAssistantSession query);

    int insert(KbAssistantSession entity);

    int update(KbAssistantSession entity);

    int deleteById(@Param("id") Long id);
}

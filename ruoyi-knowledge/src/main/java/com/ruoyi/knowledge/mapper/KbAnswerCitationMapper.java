package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbAnswerCitation;

/**
 * 回答引用Mapper接口
 *
 * @author ruoyi
 */
public interface KbAnswerCitationMapper
{
    KbAnswerCitation selectById(@Param("id") Long id);

    List<KbAnswerCitation> selectList(KbAnswerCitation query);

    int insert(KbAnswerCitation entity);

    int update(KbAnswerCitation entity);

    int deleteById(@Param("id") Long id);
}

package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbIndexJob;

/**
 * 索引任务Mapper接口
 *
 * @author ruoyi
 */
public interface KbIndexJobMapper
{
    KbIndexJob selectById(@Param("id") Long id);

    List<KbIndexJob> selectList(KbIndexJob query);

    int insert(KbIndexJob entity);

    int update(KbIndexJob entity);

    int deleteById(@Param("id") Long id);

    /**
     * 查询到期的索引任务
     *
     * @param limit 数量上限
     * @return 任务集合
     */
    List<KbIndexJob> selectDueJobs(@Param("limit") int limit);
}

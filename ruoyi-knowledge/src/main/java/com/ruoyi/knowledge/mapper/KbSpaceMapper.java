package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbSpace;

/**
 * 知识空间Mapper接口
 *
 * @author ruoyi
 */
public interface KbSpaceMapper
{
    KbSpace selectById(@Param("id") Long id);

    List<KbSpace> selectList(KbSpace query);

    /**
     * 查询用户可访问的空间列表
     *
     * @param query 查询条件
     * @param spaceIds 可访问空间ID集合
     * @return 空间集合
     */
    List<KbSpace> selectReadableList(@Param("query") KbSpace query, @Param("spaceIds") List<Long> spaceIds);

    int insert(KbSpace entity);

    int update(KbSpace entity);

    int deleteById(@Param("id") Long id);
}

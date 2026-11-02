package com.ruoyi.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.knowledge.domain.KbSpaceAcl;

/**
 * 知识空间授权Mapper接口
 *
 * @author ruoyi
 */
public interface KbSpaceAclMapper
{
    KbSpaceAcl selectById(@Param("id") Long id);

    List<KbSpaceAcl> selectList(KbSpaceAcl query);

    int insert(KbSpaceAcl entity);

    int update(KbSpaceAcl entity);

    int deleteById(@Param("id") Long id);
}

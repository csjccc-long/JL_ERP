package com.ruoyi.knowledge.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.domain.KbSpaceAcl;
import com.ruoyi.knowledge.service.IKbSpaceService;

/**
 * 知识空间
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/knowledge/space")
public class KbSpaceController extends BaseController
{
    @Autowired
    private IKbSpaceService spaceService;

    /**
     * 查询知识空间列表
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:list')")
    @GetMapping("/list")
    public TableDataInfo list(KbSpace query)
    {
        startPage();
        List<KbSpace> list = spaceService.selectKbSpaceList(query);
        return getDataTable(list);
    }

    /**
     * 查询全部可访问空间（不分页，用于下拉）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(spaceService.selectKbSpaceList(new KbSpace()));
    }

    /**
     * 查询知识空间详情
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(spaceService.selectKbSpaceById(id));
    }

    /**
     * 新增知识空间
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:add')")
    @Log(title = "知识空间", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KbSpace space)
    {
        return toAjax(spaceService.insertKbSpace(space));
    }

    /**
     * 修改知识空间
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:edit')")
    @Log(title = "知识空间", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KbSpace space)
    {
        return toAjax(spaceService.updateKbSpace(space));
    }

    /**
     * 停用知识空间
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:remove')")
    @Log(title = "知识空间", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}")
    public AjaxResult disable(@PathVariable Long id)
    {
        return toAjax(spaceService.disableKbSpace(id));
    }

    /**
     * 停用知识空间（规格路径）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:edit')")
    @Log(title = "知识空间停用", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/disable")
    public AjaxResult disableByPost(@PathVariable Long id)
    {
        return toAjax(spaceService.disableKbSpace(id));
    }

    /**
     * 查询空间授权
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:list')")
    @GetMapping("/acl/{spaceId}")
    public AjaxResult aclList(@PathVariable Long spaceId)
    {
        return success(spaceService.selectAclList(spaceId));
    }

    /**
     * 新增空间授权
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:edit')")
    @Log(title = "知识空间授权", businessType = BusinessType.INSERT)
    @PostMapping("/acl/{spaceId}")
    public AjaxResult addAcl(@PathVariable Long spaceId, @RequestBody KbSpaceAcl acl)
    {
        return toAjax(spaceService.insertAcl(spaceId, acl));
    }

    /**
     * 删除空间授权
     */
    @PreAuthorize("@ss.hasPermi('knowledge:space:edit')")
    @Log(title = "知识空间授权", businessType = BusinessType.DELETE)
    @DeleteMapping("/acl/{spaceId}/{aclId}")
    public AjaxResult deleteAcl(@PathVariable Long spaceId, @PathVariable Long aclId)
    {
        return toAjax(spaceService.deleteAcl(spaceId, aclId));
    }
}

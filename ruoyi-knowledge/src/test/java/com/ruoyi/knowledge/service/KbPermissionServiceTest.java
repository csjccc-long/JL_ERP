package com.ruoyi.knowledge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.service.IBusinessObjectService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbObjectLink;
import com.ruoyi.knowledge.mapper.KbAuthMapper;
import com.ruoyi.knowledge.service.impl.KbPermissionServiceImpl;

/**
 * 知识权限交集单元测试：空间 ACL ∩ 业务对象可见性 ∩ READY 且 is_current=1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KbPermissionServiceTest
{
    @Mock
    private KbAuthMapper authMapper;

    @Mock
    private IBusinessObjectService businessObjectService;

    @InjectMocks
    private KbPermissionServiceImpl permissionService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    public void setUp()
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(201L);
        loginUser.setDeptId(200L);
        SysUser user = new SysUser();
        user.setUserName("caigou");
        loginUser.setUser(user);
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getLoginUser).thenReturn(loginUser);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(201L);
        securityUtils.when(() -> SecurityUtils.isAdmin(anyLong())).thenReturn(false);
        ReflectionTestUtils.setField(permissionService, "businessObjectServices",
                new ArrayList<IBusinessObjectService>(Collections.singletonList(businessObjectService)));
    }

    @AfterEach
    public void tearDown()
    {
        securityUtils.close();
    }

    private KbDocument document(Long id, String name)
    {
        KbDocument document = new KbDocument();
        document.setId(id);
        document.setDocumentName(name);
        document.setParseStatus("READY");
        document.setIsCurrent(1);
        return document;
    }

    private KbObjectLink link(Long documentId, String businessType, Long businessId)
    {
        KbObjectLink link = new KbObjectLink();
        link.setDocumentId(documentId);
        link.setBusinessType(businessType);
        link.setBusinessId(businessId);
        return link;
    }

    @Test
    @DisplayName("无业务关联的 READY 文档直接可检索")
    public void retrievableDocuments_withoutLink_passes()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(Arrays.asList(1L));
        when(authMapper.selectReadyDocuments(Arrays.asList(1L)))
                .thenReturn(Collections.singletonList(document(1L, "采购制度")));
        when(authMapper.selectLinksByDocumentIds(Arrays.asList(1L))).thenReturn(new ArrayList<KbObjectLink>());
        List<KbDocument> result = permissionService.retrievableDocuments();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("关联业务对象可见的文档保留")
    public void retrievableDocuments_visibleLink_keeps()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(Arrays.asList(1L));
        when(authMapper.selectReadyDocuments(Arrays.asList(1L)))
                .thenReturn(Collections.singletonList(document(2L, "供应商执照")));
        when(authMapper.selectLinksByDocumentIds(Arrays.asList(2L)))
                .thenReturn(Collections.singletonList(link(2L, "SUPPLIER", 1L)));
        when(businessObjectService.supports("SUPPLIER")).thenReturn(true);
        when(businessObjectService.canView("SUPPLIER", 1L)).thenReturn(true);
        assertEquals(1, permissionService.retrievableDocuments().size());
    }

    @Test
    @DisplayName("任一关联业务对象不可见即剔除文档")
    public void retrievableDocuments_invisibleLink_filters()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(Arrays.asList(1L, 2L));
        when(authMapper.selectReadyDocuments(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(document(3L, "合同付款条款"), document(4L, "公开制度")));
        when(authMapper.selectLinksByDocumentIds(Arrays.asList(3L, 4L)))
                .thenReturn(Collections.singletonList(link(3L, "CONTRACT", 1L)));
        when(businessObjectService.supports("CONTRACT")).thenReturn(true);
        when(businessObjectService.canView("CONTRACT", 1L)).thenReturn(false);
        List<KbDocument> result = permissionService.retrievableDocuments();
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
    }

    @Test
    @DisplayName("没有可读空间时不产生任何可检索文档")
    public void retrievableDocuments_noReadableSpace_returnsEmpty()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(new ArrayList<Long>());
        assertTrue(permissionService.retrievableDocuments().isEmpty());
    }

    @Test
    @DisplayName("业务类型没有对应实现时视为不可见")
    public void retrievableDocuments_unsupportedType_filters()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(Arrays.asList(1L));
        when(authMapper.selectReadyDocuments(Arrays.asList(1L)))
                .thenReturn(Collections.singletonList(document(5L, "未知类型")));
        when(authMapper.selectLinksByDocumentIds(Arrays.asList(5L)))
                .thenReturn(Collections.singletonList(link(5L, "UNKNOWN", 9L)));
        when(businessObjectService.supports("UNKNOWN")).thenReturn(false);
        assertTrue(permissionService.retrievableDocuments().isEmpty());
    }

    @Test
    @DisplayName("空间读权限判断")
    public void canReadSpace_checksReadableIds()
    {
        when(authMapper.selectReadableSpaceIds(201L, 200L, 0)).thenReturn(Arrays.asList(1L, 2L));
        assertTrue(permissionService.canReadSpace(1L));
        assertFalse(permissionService.canReadSpace(3L));
        assertFalse(permissionService.canReadSpace(null));
    }

    @Test
    @DisplayName("空间管理权限依赖 MANAGE 授权")
    public void canManageSpace_checksManageGrant()
    {
        when(authMapper.countManageableSpace(eq(1L), eq(201L), eq(200L), eq(0))).thenReturn(1);
        when(authMapper.countManageableSpace(eq(2L), eq(201L), eq(200L), eq(0))).thenReturn(0);
        assertTrue(permissionService.canManageSpace(1L));
        assertFalse(permissionService.canManageSpace(2L));
        assertFalse(permissionService.canManageSpace(null));
    }
}

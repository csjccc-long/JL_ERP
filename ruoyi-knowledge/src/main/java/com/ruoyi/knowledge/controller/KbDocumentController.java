package com.ruoyi.knowledge.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbObjectLink;
import com.ruoyi.knowledge.service.IKbDocumentService;

/**
 * 知识文档
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/knowledge/document")
public class KbDocumentController extends BaseController
{
    @Autowired
    private IKbDocumentService documentService;

    /**
     * 查询文档列表
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(KbDocument query)
    {
        startPage();
        List<KbDocument> list = documentService.selectKbDocumentList(query);
        return getDataTable(list);
    }

    /**
     * 查询文档详情
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(documentService.selectKbDocumentById(id));
    }

    /**
     * 查询业务对象关联文档
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping("/by-business")
    public AjaxResult byBusiness(@RequestParam String businessType, @RequestParam Long businessId)
    {
        return success(documentService.selectDocumentsByBusiness(businessType, businessId));
    }

    /**
     * 上传文档
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:upload')")
    @Log(title = "知识文档", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file, @RequestParam Long spaceId,
            @RequestParam(required = false) String stableKey, @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId, @RequestParam(required = false) String relationType)
    {
        return success(documentService.uploadDocument(file, spaceId, stableKey, businessType, businessId, relationType));
    }

    /**
     * 重新索引
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:upload')")
    @Log(title = "知识文档", businessType = BusinessType.UPDATE)
    @PostMapping({ "/retry/{id}", "/{id}/retry" })
    public AjaxResult retry(@PathVariable Long id)
    {
        return success(documentService.retryIndex(id));
    }

    /**
     * 停用文档
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:remove')")
    @Log(title = "知识文档", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}")
    public AjaxResult disable(@PathVariable Long id)
    {
        return toAjax(documentService.disableDocument(id));
    }

    /**
     * 停用文档（规格路径）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:remove')")
    @Log(title = "知识文档停用", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/disable")
    public AjaxResult disableByPost(@PathVariable Long id)
    {
        return toAjax(documentService.disableDocument(id));
    }

    /**
     * 启用（恢复）已停用的文档
     */
    @PreAuthorize("@ss.hasAnyPermi('knowledge:document:upload,knowledge:document:remove')")
    @Log(title = "知识文档启用", businessType = BusinessType.UPDATE)
    @PostMapping({ "/{id}/enable", "/enable/{id}" })
    public AjaxResult enable(@PathVariable Long id)
    {
        return success(documentService.enableDocument(id));
    }

    /**
     * 下载文档（ERP 鉴权）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping({ "/download/{id}", "/{id}/download" })
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException
    {
        KbDocument document = documentService.selectKbDocumentById(id);
        Path path = documentService.resolveDocumentFile(id);
        return buildResponse(document, path, false);
    }

    /**
     * 预览文档（ERP 鉴权，内联展示）
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> preview(@PathVariable Long id) throws IOException
    {
        KbDocument document = documentService.selectKbDocumentById(id);
        Path path = documentService.resolveDocumentFile(id);
        return buildResponse(document, path, true);
    }

    private ResponseEntity<Resource> buildResponse(KbDocument document, Path path, boolean inline) throws IOException
    {
        String fileName = URLEncoder.encode(document.getDocumentName(), StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + fileName
                + "\"; filename*=UTF-8''" + fileName;
        MediaType mediaType = resolveMediaType(document, path);
        return ResponseEntity.ok().contentType(mediaType).header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(path));
    }

    /**
     * 解析文档真实的 Content-Type：库里存的是 octet-stream（浏览器上传时常给这个值）时，
     * 依次回退到文件探测和文件名扩展名，文本类型统一补 charset，避免浏览器把文件当成二进制或乱码展示。
     */
    private MediaType resolveMediaType(KbDocument document, Path path)
    {
        String contentType = document.getMimeType();
        if (StringUtils.isEmpty(contentType) || MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType))
        {
            String probed = null;
            try
            {
                probed = Files.probeContentType(path);
            }
            catch (IOException ignored)
            {
                probed = null;
            }
            if (StringUtils.isNotEmpty(probed))
            {
                contentType = probed;
            }
        }
        MediaType mediaType = null;
        if (StringUtils.isNotEmpty(contentType) && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType))
        {
            try
            {
                mediaType = MediaType.parseMediaType(contentType);
            }
            catch (RuntimeException ignored)
            {
                mediaType = null;
            }
        }
        if (mediaType == null)
        {
            mediaType = MediaTypeFactory.getMediaType(document.getDocumentName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        }
        if (mediaType.getCharset() == null && "text".equalsIgnoreCase(mediaType.getType()))
        {
            return new MediaType(mediaType.getType(), mediaType.getSubtype(), StandardCharsets.UTF_8);
        }
        return mediaType;
    }

    /**
     * 查询文档业务关联
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:list')")
    @GetMapping("/{id}/links")
    public AjaxResult links(@PathVariable Long id)
    {
        return success(documentService.selectLinks(id));
    }

    /**
     * 新增文档业务关联
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:upload')")
    @Log(title = "文档业务关联", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/link")
    public AjaxResult link(@PathVariable Long id, @RequestBody KbObjectLink link)
    {
        return toAjax(documentService.linkDocument(id, link));
    }

    /**
     * 取消文档业务关联
     */
    @PreAuthorize("@ss.hasPermi('knowledge:document:upload')")
    @Log(title = "文档业务关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}/link/{linkId}")
    public AjaxResult unlink(@PathVariable Long id, @PathVariable Long linkId)
    {
        return toAjax(documentService.unlinkDocument(id, linkId));
    }
}

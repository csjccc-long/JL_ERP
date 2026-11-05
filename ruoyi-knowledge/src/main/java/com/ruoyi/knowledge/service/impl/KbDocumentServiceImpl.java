package com.ruoyi.knowledge.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.constant.KbConstants;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbIndexJob;
import com.ruoyi.knowledge.domain.KbObjectLink;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.mapper.KbDocumentMapper;
import com.ruoyi.knowledge.mapper.KbIndexJobMapper;
import com.ruoyi.knowledge.mapper.KbObjectLinkMapper;
import com.ruoyi.knowledge.mapper.KbSpaceMapper;
import com.ruoyi.knowledge.ragflow.RagFlowClient;
import com.ruoyi.knowledge.ragflow.RagFlowException;
import com.ruoyi.knowledge.service.IKbDocumentService;
import com.ruoyi.knowledge.service.IKbPermissionService;
import com.ruoyi.knowledge.service.IKbSpaceService;

/**
 * 知识文档服务实现
 *
 * @author ruoyi
 */
@Service
public class KbDocumentServiceImpl implements IKbDocumentService
{
    private static final Logger log = LoggerFactory.getLogger(KbDocumentServiceImpl.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "txt",
            "png", "jpg", "jpeg");

    @Value("${erp.upload-path:${user.home}/erp-upload}")
    private String uploadPath;

    @Value("${erp.max-file-size:52428800}")
    private long uploadMaxSize;

    @Autowired
    private KbDocumentMapper documentMapper;

    @Autowired
    private KbObjectLinkMapper objectLinkMapper;

    @Autowired
    private KbIndexJobMapper indexJobMapper;

    @Autowired
    private KbSpaceMapper spaceMapper;

    @Autowired
    private RagFlowClient ragFlowClient;

    @Autowired
    private IKbPermissionService permissionService;

    @Autowired
    private IKbSpaceService spaceService;

    @Override
    public List<KbDocument> selectKbDocumentList(KbDocument query)
    {
        List<Long> spaceIds = permissionService.readableSpaceIds();
        if (spaceIds.isEmpty())
        {
            return new ArrayList<KbDocument>();
        }
        if (query.getSpaceId() != null && !spaceIds.contains(query.getSpaceId()))
        {
            throw new ServiceException("无权访问该知识空间", 403);
        }
        List<KbDocument> documents = documentMapper.selectReadableList(query, spaceIds);
        for (KbDocument document : documents)
        {
            document.setLinks(objectLinkMapper.selectByDocumentId(document.getId()));
        }
        return documents;
    }

    @Override
    public KbDocument selectKbDocumentById(Long id)
    {
        return requireReadable(id);
    }

    private KbDocument requireReadable(Long id)
    {
        KbDocument document = documentMapper.selectById(id);
        if (document == null)
        {
            throw new ServiceException("文档不存在", 404);
        }
        if (!permissionService.canReadSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权访问该文档", 403);
        }
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument uploadDocument(MultipartFile file, Long spaceId, String stableKey, String businessType,
            Long businessId, String relationType)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空", 400);
        }
        if (spaceId == null)
        {
            throw new ServiceException("请选择知识空间", 400);
        }
        if (!permissionService.canManageSpace(spaceId) && !permissionService.canReadSpace(spaceId))
        {
            throw new ServiceException("无权向该知识空间上传文档", 403);
        }
        KbSpace space = spaceMapper.selectById(spaceId);
        if (space == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        if (!KbConstants.SPACE_ACTIVE.equals(space.getSpaceStatus()))
        {
            throw new ServiceException("知识空间已停用，无法上传文档", 400);
        }
        if (file.getSize() > uploadMaxSize)
        {
            throw new ServiceException("上传文件超过大小限制 " + (uploadMaxSize / 1024 / 1024) + " MB");
        }
        String originalName = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalName))
        {
            throw new ServiceException("文件名不能为空", 400);
        }
        originalName = Paths.get(originalName).getFileName().toString();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension))
        {
            throw new ServiceException("不支持的文件类型，仅允许 PDF、DOC、DOCX、XLS、XLSX、TXT、PNG、JPG、JPEG", 400);
        }
        String contentType = file.getContentType();
        if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("application/x-")
                && !"application/x-zip-compressed".equals(contentType.toLowerCase(Locale.ROOT)))
        {
            throw new ServiceException("文件类型校验失败", 400);
        }
        String resolvedStableKey = StringUtils.isEmpty(stableKey) ? stableKeyOf(originalName) : stableKey;
        Integer maxVersion = documentMapper.selectMaxVersion(spaceId, resolvedStableKey);
        int version = (maxVersion == null ? 0 : maxVersion) + 1;
        boolean hasCurrent = documentMapper.selectCurrentByStableKey(spaceId, resolvedStableKey) != null;

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = Paths.get(uploadPath, "knowledge", String.valueOf(spaceId)).toAbsolutePath().normalize();
        Path target = directory.resolve(storedName);
        String checksum;
        try
        {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream())
            {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            checksum = sha256(target);
        }
        catch (IOException e)
        {
            log.error("保存上传文件失败: {}", e.getMessage(), e);
            throw new ServiceException("保存上传文件失败");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        KbDocument document = new KbDocument();
        document.setSpaceId(spaceId);
        document.setDocumentName(originalName);
        document.setStableKey(resolvedStableKey);
        document.setDocumentVersion(version);
        document.setIsCurrent(hasCurrent ? 0 : 1);
        document.setFilePath(target.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(StringUtils.nvl(contentType, "application/octet-stream"));
        document.setChecksumSha256(checksum);
        document.setParseStatus(KbConstants.DOC_UPLOADED);
        document.setChunkCount(0);
        document.setTokenCount(0);
        document.setUploadedBy(loginUser.getUserId());
        document.setDeptId(space.getDeptId());
        document.setVersion(0);
        document.setCreateBy(SecurityUtils.getUsername());
        document.setCreateTime(new Date());
        documentMapper.insert(document);

        if (StringUtils.isNotEmpty(businessType) && businessId != null)
        {
            KbObjectLink link = new KbObjectLink();
            link.setDocumentId(document.getId());
            link.setBusinessType(businessType);
            link.setBusinessId(businessId);
            link.setRelationType(StringUtils.isEmpty(relationType) ? "ATTACHMENT" : relationType);
            link.setCreateBy(SecurityUtils.getUsername());
            link.setCreateTime(new Date());
            objectLinkMapper.insert(link);
        }

        KbIndexJob job = new KbIndexJob();
        job.setDocumentId(document.getId());
        job.setJobStatus(KbConstants.JOB_PENDING);
        job.setAttemptCount(0);
        job.setCreateTime(new Date());
        indexJobMapper.insert(job);

        try
        {
            String ragDocumentId = ragFlowClient.uploadDocument(space.getRagDatasetId(), target, originalName);
            documentMapper.markIndexing(document.getId(), ragDocumentId);
            document.setRagDocumentId(ragDocumentId);
            document.setParseStatus(KbConstants.DOC_INDEXING);
            List<String> documentIds = new ArrayList<String>();
            documentIds.add(ragDocumentId);
            ragFlowClient.parseDocuments(space.getRagDatasetId(), documentIds);
        }
        catch (RagFlowException e)
        {
            log.warn("上传 RAGFlow 文档失败，文档待重试: {}", e.getMessage());
            documentMapper.markFailed(document.getId(), "知识服务暂不可用，等待重试");
            document.setParseStatus(KbConstants.DOC_FAILED);
        }
        return document;
    }

    private String stableKeyOf(String fileName)
    {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0)
        {
            base = base.substring(0, dot);
        }
        return base.length() > 60 ? base.substring(0, 60) : base;
    }

    private String extensionOf(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1)
        {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(Path path)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path))
            {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1)
                {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest())
            {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        catch (NoSuchAlgorithmException | IOException e)
        {
            throw new ServiceException("计算文件校验值失败");
        }
    }

    @Override
    public KbDocument retryIndex(Long id)
    {
        return retryIndex(id, false);
    }

    /**
     * 重新推送并解析文档
     *
     * @param id 文档ID
     * @param forceReupload 为 true 时忽略旧的 ragDocumentId，强制重新上传原件（停用后 RAGFlow 侧文档已被删除）
     */
    private KbDocument retryIndex(Long id, boolean forceReupload)
    {
        KbDocument document = requireReadable(id);
        if (!permissionService.canManageSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权重试该文档", 403);
        }
        KbSpace space = spaceMapper.selectById(document.getSpaceId());
        if (space == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        KbIndexJob query = new KbIndexJob();
        query.setDocumentId(document.getId());
        List<KbIndexJob> jobs = indexJobMapper.selectList(query);
        KbIndexJob job = jobs == null || jobs.isEmpty() ? null : jobs.get(0);
        if (job == null)
        {
            job = new KbIndexJob();
            job.setDocumentId(document.getId());
            job.setJobStatus(KbConstants.JOB_PENDING);
            job.setAttemptCount(0);
            job.setCreateTime(new Date());
            indexJobMapper.insert(job);
        }
        try
        {
            String ragDocumentId = forceReupload ? null : document.getRagDocumentId();
            if (StringUtils.isEmpty(ragDocumentId))
            {
                ragDocumentId = ragFlowClient.uploadDocument(space.getRagDatasetId(), resolveFilePath(document),
                        document.getDocumentName());
            }
            documentMapper.markIndexing(document.getId(), ragDocumentId);
            List<String> ids = new ArrayList<String>();
            ids.add(ragDocumentId);
            ragFlowClient.parseDocuments(space.getRagDatasetId(), ids);
            KbIndexJob jobUpdate = new KbIndexJob();
            jobUpdate.setId(job.getId());
            jobUpdate.setJobStatus(KbConstants.JOB_PENDING);
            jobUpdate.setNextRetryTime(null);
            jobUpdate.setErrorMessage(null);
            indexJobMapper.update(jobUpdate);
            document.setRagDocumentId(ragDocumentId);
            document.setParseStatus(KbConstants.DOC_INDEXING);
        }
        catch (RagFlowException e)
        {
            log.warn("重试索引失败: {}", e.getMessage());
            throw new ServiceException("知识服务暂不可用，请稍后重试", 503);
        }
        return document;
    }

    @Override
    public int disableDocument(Long id)
    {
        KbDocument document = requireReadable(id);
        if (!permissionService.canManageSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权停用该文档", 403);
        }
        KbSpace space = spaceMapper.selectById(document.getSpaceId());
        if (space != null && StringUtils.isNotEmpty(document.getRagDocumentId()))
        {
            List<String> ragIds = new ArrayList<String>();
            ragIds.add(document.getRagDocumentId());
            try
            {
                ragFlowClient.deleteDocuments(space.getRagDatasetId(), ragIds);
            }
            catch (RagFlowException e)
            {
                log.warn("删除 RAGFlow 文档失败，仅停用本地文档: {}", e.getMessage());
            }
        }
        return documentMapper.markDisabled(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument enableDocument(Long id)
    {
        KbDocument document = documentMapper.selectById(id);
        if (document == null)
        {
            throw new ServiceException("文档不存在", 404);
        }
        if (!permissionService.canManageSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权启用该文档", 403);
        }
        if (!KbConstants.DOC_DISABLED.equals(document.getParseStatus()))
        {
            // 未停用的文档无需重复启用
            return document;
        }
        KbSpace space = spaceMapper.selectById(document.getSpaceId());
        if (space == null)
        {
            throw new ServiceException("知识空间不存在", 404);
        }
        // 恢复为当前生效版本（停用时 is_current 被置 0）
        if (StringUtils.isNotEmpty(document.getStableKey()))
        {
            documentMapper.clearCurrentByStableKey(document.getSpaceId(), document.getStableKey());
        }
        documentMapper.updateCurrentFlag(id, 1);
        // 原始文件已丢失时无法重新推送，先恢复为"已上传"，等用户重新上传后再解析
        if (!Files.exists(resolveFilePath(document)))
        {
            log.warn("启用文档 {} 时原始文件缺失: {}", id, document.getFilePath());
            documentMapper.markUploaded(id);
            document.setParseStatus(KbConstants.DOC_UPLOADED);
            document.setRagDocumentId(null);
            document.setChunkCount(0);
            document.setTokenCount(0);
            return document;
        }
        // 停用已把 RAGFlow 文档删除，旧的 ragDocumentId 已失效，重新上传原件再解析
        return retryIndex(id, true);
    }

    /**
     * 解析文档磁盘路径：相对路径统一按上传根目录解析，避免落到进程工作目录
     */
    private Path resolveFilePath(KbDocument document)
    {
        Path path = Paths.get(document.getFilePath());
        if (!path.isAbsolute())
        {
            path = Paths.get(uploadPath).resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    @Override
    public Path resolveDocumentFile(Long id)
    {
        KbDocument document = requireReadable(id);
        if (!KbConstants.DOC_READY.equals(document.getParseStatus())
                && !KbConstants.DOC_INDEXING.equals(document.getParseStatus())
                && !KbConstants.DOC_UPLOADED.equals(document.getParseStatus()))
        {
            throw new ServiceException("文档当前不可下载", 400);
        }
        Path path = resolveFilePath(document);
        if (!Files.exists(path))
        {
            throw new ServiceException("文档文件不存在", 404);
        }
        return path;
    }

    @Override
    public int linkDocument(Long id, KbObjectLink link)
    {
        KbDocument document = requireReadable(id);
        if (!permissionService.canManageSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权维护该文档的业务关联", 403);
        }
        if (StringUtils.isEmpty(link.getBusinessType()) || link.getBusinessId() == null)
        {
            throw new ServiceException("业务对象不能为空");
        }
        link.setDocumentId(id);
        if (StringUtils.isEmpty(link.getRelationType()))
        {
            link.setRelationType("ATTACHMENT");
        }
        link.setCreateBy(SecurityUtils.getUsername());
        link.setCreateTime(new Date());
        return objectLinkMapper.insert(link);
    }

    @Override
    public int unlinkDocument(Long id, Long linkId)
    {
        KbDocument document = requireReadable(id);
        if (!permissionService.canManageSpace(document.getSpaceId()))
        {
            throw new ServiceException("无权维护该文档的业务关联", 403);
        }
        KbObjectLink link = objectLinkMapper.selectById(linkId);
        if (link == null || !id.equals(link.getDocumentId()))
        {
            throw new ServiceException("关联记录不存在", 404);
        }
        return objectLinkMapper.deleteById(linkId);
    }

    @Override
    public List<KbObjectLink> selectLinks(Long id)
    {
        requireReadable(id);
        return objectLinkMapper.selectByDocumentId(id);
    }

    @Override
    public List<KbDocument> selectDocumentsByBusiness(String businessType, Long businessId)
    {
        if (StringUtils.isEmpty(businessType) || businessId == null)
        {
            throw new ServiceException("业务对象不能为空");
        }
        List<Long> spaceIds = permissionService.readableSpaceIds();
        if (spaceIds.isEmpty())
        {
            return new ArrayList<KbDocument>();
        }
        return documentMapper.selectByBusiness(businessType, businessId, spaceIds);
    }
}

package com.ruoyi.knowledge.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.knowledge.constant.KbConstants;
import com.ruoyi.knowledge.domain.KbDocument;
import com.ruoyi.knowledge.domain.KbIndexJob;
import com.ruoyi.knowledge.domain.KbSpace;
import com.ruoyi.knowledge.mapper.KbDocumentMapper;
import com.ruoyi.knowledge.mapper.KbIndexJobMapper;
import com.ruoyi.knowledge.mapper.KbSpaceMapper;
import com.ruoyi.knowledge.ragflow.RagFlowClient;
import com.ruoyi.knowledge.ragflow.RagFlowException;

/**
 * 文档索引任务轮询与失败重试
 *
 * <p>后台每 30 秒轮询 RAGFlow 文档解析状态；失败按指数退避重试，超过 3 次标记失败并要求人工重试。
 * RAGFlow 不可用只影响索引状态，不影响 ERP 业务。</p>
 *
 * @author ruoyi
 */
@Service
public class KbIndexJobService
{
    private static final Logger log = LoggerFactory.getLogger(KbIndexJobService.class);

    /** 单次轮询处理任务上限 */
    private static final int BATCH_SIZE = 20;

    @Autowired
    private KbIndexJobMapper indexJobMapper;

    @Autowired
    private KbDocumentMapper documentMapper;

    @Autowired
    private KbSpaceMapper spaceMapper;

    @Autowired
    private RagFlowClient ragFlowClient;

    /**
     * 定时轮询索引任务
     */
    @Scheduled(initialDelay = 30000L, fixedDelay = 30000L)
    public void pollIndexJobs()
    {
        List<KbIndexJob> jobs = indexJobMapper.selectDueJobs(BATCH_SIZE);
        if (jobs == null || jobs.isEmpty())
        {
            return;
        }
        for (KbIndexJob job : jobs)
        {
            try
            {
                handle(job);
            }
            catch (RuntimeException e)
            {
                log.warn("处理索引任务 {} 失败: {}", job.getId(), e.getMessage());
            }
        }
    }

    private void handle(KbIndexJob job)
    {
        KbDocument document = documentMapper.selectById(job.getDocumentId());
        if (document == null)
        {
            finish(job, KbConstants.JOB_FAILED, "文档不存在");
            return;
        }
        if (KbConstants.DOC_READY.equals(document.getParseStatus()))
        {
            finish(job, KbConstants.JOB_SUCCESS, null);
            return;
        }
        if (KbConstants.DOC_DISABLED.equals(document.getParseStatus()))
        {
            finish(job, KbConstants.JOB_FAILED, "文档已停用");
            return;
        }
        KbSpace space = spaceMapper.selectById(document.getSpaceId());
        if (space == null || StringUtils.isEmpty(space.getRagDatasetId()))
        {
            finish(job, KbConstants.JOB_FAILED, "知识数据集不存在");
            return;
        }
        int attempt = job.getAttemptCount() == null ? 0 : job.getAttemptCount();
        try
        {
            String ragDocumentId = document.getRagDocumentId();
            if (StringUtils.isEmpty(ragDocumentId))
            {
                java.nio.file.Path file = java.nio.file.Paths.get(document.getFilePath());
                ragDocumentId = ragFlowClient.uploadDocument(space.getRagDatasetId(), file, document.getDocumentName());
                documentMapper.markIndexing(document.getId(), ragDocumentId);
                List<String> ids = new ArrayList<String>();
                ids.add(ragDocumentId);
                ragFlowClient.parseDocuments(space.getRagDatasetId(), ids);
                schedule(job, attempt, 15);
                return;
            }
            String status = ragFlowClient.getDocumentStatus(space.getRagDatasetId(), ragDocumentId);
            if ("DONE".equalsIgnoreCase(status))
            {
                int chunkCount = ragFlowClient.getChunkCount(space.getRagDatasetId(), ragDocumentId);
                documentMapper.markReady(document.getId(), chunkCount, 0);
                if (document.getIsCurrent() == null || document.getIsCurrent() == 0)
                {
                    documentMapper.clearCurrentByStableKey(document.getSpaceId(), document.getStableKey());
                    documentMapper.updateCurrentFlag(document.getId(), 1);
                }
                finish(job, KbConstants.JOB_SUCCESS, null);
                return;
            }
            if ("FAIL".equalsIgnoreCase(status) || "CANCEL".equalsIgnoreCase(status))
            {
                retry(job, document, space, attempt + 1, "RAGFlow 解析失败（状态 " + status + "）");
                return;
            }
            schedule(job, attempt, 15);
        }
        catch (RagFlowException e)
        {
            retry(job, document, space, attempt + 1, "知识服务暂不可用：" + e.getMessage());
        }
    }

    private void retry(KbIndexJob job, KbDocument document, KbSpace space, int attempt, String message)
    {
        if (attempt >= KbConstants.INDEX_MAX_ATTEMPT)
        {
            documentMapper.markFailed(document.getId(), message);
            finish(job, KbConstants.JOB_FAILED, message);
            return;
        }
        if (StringUtils.isNotEmpty(document.getRagDocumentId()))
        {
            try
            {
                List<String> ids = new ArrayList<String>();
                ids.add(document.getRagDocumentId());
                ragFlowClient.parseDocuments(space.getRagDatasetId(), ids);
            }
            catch (RagFlowException e)
            {
                log.warn("重新触发解析失败: {}", e.getMessage());
            }
        }
        KbIndexJob update = new KbIndexJob();
        update.setId(job.getId());
        update.setJobStatus(KbConstants.JOB_PENDING);
        update.setAttemptCount(attempt);
        update.setNextRetryTime(backoff(attempt));
        update.setErrorMessage(message);
        indexJobMapper.update(update);
    }

    private void schedule(KbIndexJob job, int attempt, int delaySeconds)
    {
        KbIndexJob update = new KbIndexJob();
        update.setId(job.getId());
        update.setJobStatus(KbConstants.JOB_RUNNING);
        update.setAttemptCount(attempt);
        update.setStartedTime(job.getStartedTime() == null ? new Date() : job.getStartedTime());
        update.setNextRetryTime(new Date(System.currentTimeMillis() + delaySeconds * 1000L));
        indexJobMapper.update(update);
    }

    private void finish(KbIndexJob job, String status, String message)
    {
        KbIndexJob update = new KbIndexJob();
        update.setId(job.getId());
        update.setJobStatus(status);
        update.setFinishedTime(new Date());
        update.setNextRetryTime(null);
        update.setErrorMessage(message);
        if (job.getAttemptCount() != null)
        {
            update.setAttemptCount(job.getAttemptCount());
        }
        indexJobMapper.update(update);
    }

    private Date backoff(int attempt)
    {
        long delay = 30L * (1L << Math.min(attempt, 5));
        return new Date(System.currentTimeMillis() + delay * 1000L);
    }
}

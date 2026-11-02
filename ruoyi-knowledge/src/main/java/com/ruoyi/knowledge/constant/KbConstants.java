package com.ruoyi.knowledge.constant;

/**
 * 知识中心常量
 *
 * @author ruoyi
 */
public class KbConstants
{
    /** 空间状态：启用 */
    public static final String SPACE_ACTIVE = "ACTIVE";
    /** 空间状态：停用 */
    public static final String SPACE_DISABLED = "DISABLED";

    /** 空间类型 */
    public static final String SPACE_TYPE_POLICY = "POLICY";
    public static final String SPACE_TYPE_SUPPLIER = "SUPPLIER";
    public static final String SPACE_TYPE_CONTRACT = "CONTRACT";
    public static final String SPACE_TYPE_PROCUREMENT = "PROCUREMENT";

    /** 授权对象类型 */
    public static final String SUBJECT_USER = "USER";
    public static final String SUBJECT_ROLE = "ROLE";
    public static final String SUBJECT_DEPT = "DEPT";

    /** 权限级别 */
    public static final String PERM_READ = "READ";
    public static final String PERM_WRITE = "WRITE";
    public static final String PERM_MANAGE = "MANAGE";

    /** 文档解析状态 */
    public static final String DOC_UPLOADED = "UPLOADED";
    public static final String DOC_INDEXING = "INDEXING";
    public static final String DOC_READY = "READY";
    public static final String DOC_FAILED = "FAILED";
    public static final String DOC_DISABLED = "DISABLED";

    /** 索引任务状态 */
    public static final String JOB_PENDING = "PENDING";
    public static final String JOB_RUNNING = "RUNNING";
    public static final String JOB_SUCCESS = "SUCCESS";
    public static final String JOB_FAILED = "FAILED";

    /** 问答结果状态 */
    public static final String AUDIT_SUCCESS = "SUCCESS";
    public static final String AUDIT_INSUFFICIENT = "INSUFFICIENT_EVIDENCE";
    public static final String AUDIT_UNAVAILABLE = "RAG_UNAVAILABLE";
    public static final String AUDIT_FORBIDDEN = "FORBIDDEN";
    public static final String AUDIT_FAILED = "FAILED";

    /** 引用来源类型 */
    public static final String CITATION_DOCUMENT = "DOCUMENT";
    public static final String CITATION_BUSINESS = "BUSINESS";

    /** 会话标题最大长度 */
    public static final int SESSION_TITLE_LENGTH = 60;

    /** 历史消息轮数上限 */
    public static final int HISTORY_ROUNDS = 10;

    /** 索引任务最大重试次数 */
    public static final int INDEX_MAX_ATTEMPT = 3;

    /** 会话状态 */
    public static final String SESSION_ACTIVE = "ACTIVE";
}

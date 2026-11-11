# 智能 ERP + 知识引擎（RAGFlow）融合版 · 部署与运维手册

本文档对应设计规格 `2026-11-01-intelligent-erp-rag-design.md`，描述融合后的单一项目如何初始化、启动、验收与排障。

## 1. 架构与模块

融合后的项目仍是一个若依（RuoYi V3.9.2 / Spring Boot 4.1.0 / Java 17+ / Vue3 + Vite）单体工程，新增两个业务模块：

| 模块 | 职责 | 关键内容 |
| --- | --- | --- |
| `ruoyi-erp` | 采购与库存业务基座 | 供应商准入、合同、采购申请、采购订单、到货入库、库存流水与余额、审批记录、驾驶舱 |
| `ruoyi-knowledge` | 知识引擎适配层 | 知识空间与 ACL、文档上传/版本/解析、RAGFlow REST 客户端、异步索引轮询、智能助手（SSE 流式）、问答审计 |
| `ruoyi-admin` | 启动模块 | 依赖上述两个模块，暴露 `/erp/**`、`/knowledge/**`、`/assistant/**` 接口 |
| `ruoyi-ui` | 前端 | `views/erp/**`、`views/knowledge/**`、`views/assistant/index.vue`、`components/AssistantPanel` |

依赖方向：`erp` 不依赖 `knowledge`；`knowledge` 通过 `IBusinessObjectService`（实现位于 `ruoyi-erp`：`com.ruoyi.erp.service.impl.ErpBusinessObjectServiceImpl`）以 `businessType + businessId` 关联 ERP 对象，实现"空间 ACL ∩ 业务对象可见性 ∩ 文档就绪"的权限交集。

## 2. 环境要求

| 组件 | 版本/说明 |
| --- | --- |
| JDK | 17 及以上（本次构建使用 JDK 23） |
| Maven | 3.9+（首次构建需联网下载依赖） |
| MySQL | 8.0+，`utf8mb4` |
| Redis | 5.0+（若依缓存与令牌） |
| Node.js | 18+ / 20+，pnpm 或 npm |
| RAGFlow | v0.26.4，HTTP 可达（默认 `http://127.0.0.1:9380`） |

RAGFlow 不是 ERP 业务的强依赖：知识服务不可用时，采购、审批、入库、库存等业务照常运行，助手提示"知识服务不可用"。

## 3. 环境变量

服务端配置集中在 `ruoyi-admin/src/main/resources/application.yml`，生产环境建议用环境变量或 `application-prod.yml` 覆盖：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `RAGFLOW_BASE_URL` | `http://127.0.0.1:9380` | RAGFlow API 基地址 |
| `RAGFLOW_API_KEY` | 空 | RAGFlow API Key，**仅服务端持有，不得下发到浏览器** |
| `RAGFLOW_CHAT_ID` | 空 | RAGFlow 助手（chat assistant）ID，用于流式问答 |
| `RAGFLOW_TIMEOUT_SECONDS` | `90` | 读取超时（连接超时固定 5s） |
| `RAGFLOW_RETRY_MAX` | `3` | 最大重试次数（指数退避 200ms 起） |
| `ERP_UPLOAD_PATH` | `D:/ruoyi/uploadPath/erp` | 文档落盘根目录，子目录 `knowledge/{spaceId}/` |
| `ERP_DB_URL` | `jdbc:mysql://localhost:3306/ry-vue?...` | 主库连接串（`application-druid.yml`） |
| `ERP_DB_USERNAME` | `root` | 主库账号 |
| `ERP_DB_PASSWORD` | `password` | 主库密码 |
| `ERP_REDIS_HOST` | `localhost` | Redis 地址 |
| `ERP_REDIS_PORT` | `6379` | Redis 端口 |
| `ERP_REDIS_DATABASE` | `0` | Redis 库索引 |
| `ERP_REDIS_PASSWORD` | 空 | Redis 密码 |
| `ERP_JWT_SECRET` | `abcdefghijklmnopqrstuvwxyz` | 令牌签名密钥，**生产环境必须修改** |

健康检查：`GET /actuator/health`（无需登录，只返回聚合状态，不暴露数据库/Redis 细节，也不触发任何模型调用）；`/actuator/health/liveness`、`/actuator/health/readiness` 供容器探针使用。

其他相关配置（`application.yml`）：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 52MB
      max-request-size: 60MB
erp:
  upload-path: ${ERP_UPLOAD_PATH:D:/ruoyi/uploadPath/erp}
  max-file-size: 52428800          # 50MB，与前端校验一致
  allowed-extensions: pdf,doc,docx,xls,xlsx,txt,png,jpg,jpeg
  low-stock-threshold: 10
ragflow:
  chunk-method: naive
  page-size: 10
  similarity-threshold: 0.2
  vector-similarity-weight: 0.3
```

## 4. 初始化步骤

1. 创建数据库并导入若依基础脚本（**顺序不可颠倒**）：

   ```bash
   mysql -uroot -p -e "create database ry-vue default charset utf8mb4 collate utf8mb4_general_ci;"
   mysql -uroot -p ry-vue < sql/ry_20260417.sql
   mysql -uroot -p ry-vue < sql/quartz.sql
   mysql -uroot -p ry-vue < sql/erp_knowledge_20261101.sql
   mysql -uroot -p ry-vue < sql/erp_checkin.sql          # 可选：个人中心每日打卡
   mysql -uroot -p ry-vue < sql/erp_day_task.sql         # 可选：打卡当日待办/自动化演示数据
   ```

   `erp_knowledge_20261101.sql` 可重复执行：先定向清理 `sys_menu(2000-3999)`、`sys_role(2001-2006)`、`sys_user(200-205)`、`sys_dept(200-204)`、`erp_\*` 字典与全部业务表，再重建并写入演示数据。

2. 修改 `application-druid.yml` 中的数据库账号、`application.yml` 中的 Redis 地址。

3. 配置 RAGFlow：

   - 启动 RAGFlow 并创建 API Key（页面：右上角头像 → API → API Key），写入 `RAGFLOW_API_KEY`；
   - 创建一个 chat assistant（绑定 chat 模型、开启引用"，用于 `/api/v1/chat/completions` 流式问答），把 assistant id 写入 `RAGFLOW_CHAT_ID`；
   - 数据集无需手工创建：新增知识空间时系统会调用 `POST /api/v1/datasets` 自动创建并回写 `kb_space.rag_dataset_id`。
   - 演示数据中的 3 个知识空间带有占位数据集 id（`demo-dataset-*`），首次使用前请在"知识空间"页面修改为真实空间，或手工执行
     `update kb_space set rag_dataset_id = (真实数据集ID) where id = N;`（`rag_dataset_id` 唯一，注意勿重复）。

4. 初始化 RAGFlow 模型：在 RAGFlow 中配置 embedding 模型（默认本地部署，不内置任何云端密钥）与 chat 模型。

5. 构建与启动：

   ```bash
   # 后端（Windows PowerShell 示例）
   $env:JAVA_HOME="C:\Program Files\Java\jdk-23"
   mvn -DskipTests package
   java -jar ruoyi-admin/target/ruoyi-admin.jar

   # 前端
   cd ruoyi-ui
   npm install
   npm run dev            # 开发：http://localhost:80，代理到后端 8080
   npm run build:prod     # 生产：产物在 ruoyi-ui/dist
   ```

## 5. 演示账号

初始密码均为 `admin123`（若依默认口令哈希）。演示用户与角色：

| 账号 | 姓名 | 部门 | 角色 | 用途 |
| --- | --- | --- | --- | --- |
| `admin` | 管理员 | 总公司 | 超级管理员 | 全量权限 |
| `caigou` | 陈采购 | 采购部 | 采购员 | 建供应商/合同/申请/订单 |
| `fuzeren` | 李负责人 | 采购部 | 采购负责人 | 审批准入、申请、订单 |
| `caiwu` | 赵财务 | 财务部 | 财务审计员 | 审计与付款条款查看 |
| `cgy` | 刘仓管 | 仓储部 | 仓管员 | 到货入库、库存查询 |
| `shenji` | 王审计 | 财务部 | 财务审计员 | 问答审计、操作日志 |
| `zhishi` | 郑知识 | 采购部 | 知识管理员 | 知识空间、文档、ACL |

演示数据包含：5 个物料、2 个仓库、3 家供应商（1 家待准入）、2 份合同（1 份 25 天后到期）、1 份已批准申请、4 张订单（含部分到货与待审批）、2 张入库单、库存余额与流水、11 条审批记录、3 个知识空间（含 ACL）、3 份示例文档与业务关联。业务单号固定（`SUP-000x`、`HT-2026-000x`、`PR-2026-000x`、`PO-2026-000x`、`RC-2026-000x`），测试可直接断言。

示例文档在数据库中为 `UPLOADED` 状态且 `rag_document_id` 为空，因此不参与检索（避免伪造引用）。要跑通问答链路，请在对应空间上传真实的 PDF/Word 文件，文档会自动进入 RAGFlow 解析并轮询到 `READY`。

## 6. 验收步骤

### 6.1 主流程（对应规格 15.4）

1. `caigou` 登录 → 采购管理 → 供应商 → 新增 → "准入" 提交（`APPROVED`）。
2. `fuzeren` 登录 → 供应商 → 准入审批通过（或驳回）。
3. `caigou` 新建合同 → 提交审批 → `fuzeren` 审批通过（合同变 `ACTIVE`）。
4. `caigou` 新建采购申请（含明细）→ 提交 → `fuzeren` 审批通过。
5. `caigou` 新建采购订单（选择供应商/合同/申请，含明细）→ 提交 → `fuzeren` 审批通过。
6. `cgy` 登录 → 到货入库 → 新建（选择订单自动带出明细，填写到货/合格/不合格数量）→ 确认入库。
7. 库存查询 → 余额增加、流水出现 `RECEIPT` 记录，订单状态变为 `PARTIALLY_RECEIVED` 或 `RECEIVED`。
8. 打开订单/合同/供应商的"知识"抽屉 → 提问合同质保条款与到货情况 → 答案包含结构化业务摘要与引用卡片 → 点击引用"查看原文"。
9. `shenji` 登录 → 知识中心 → 问答审计：可按追踪 ID、问题关键字、状态、时间范围检索，详情含空间/文档范围与错误信息。

### 6.2 故障验收

```text
停止 RAGFlow
  → 采购申请、订单、入库仍可完成（不报错、不阻塞）
  → 助手返回"知识服务不可用，请稍后重试"（SSE status=RAG_UNAVAILABLE）
  → 知识中心 → 文档列表出现 FAILED + 错误原因
  → 恢复 RAGFlow
  → 文档"重试"后轮询至 READY
  → 助手恢复问答
```

### 6.3 权限验收

- 无 `knowledge:space` ACL 的用户在空间列表看不到该空间；
- 有空间权限但无业务对象可见性的用户，其关联文档不会出现在检索与引用中；
- 非 `READY` 或非当前版本（`is_current=0`）的文档不参与检索；
- 引用原文预览/下载接口会再次校验 ERP 登录态与权限。

## 7. 故障排查

| 现象 | 排查 |
| --- | --- |
| 启动报 `Table 'ry-vue.erp_xxx' doesn't exist` | 未执行 `sql/erp_knowledge_20261101.sql` |
| 新增知识空间提示"知识服务不可用" | `RAGFLOW_BASE_URL` 不可达或 Key 错误；调用 `GET /assistant/health` 查看 `ragflowConfigured`/`ragflowAvailable` |
| 文档一直 `INDEXING` | RAGFlow 解析慢或 embedding 未配置；查看 `kb_index_job` 的 `attempt_count`、`next_retry_time`；超过 3 次会置 `FAILED` |
| 文档 `FAILED` | 查看 `kb_document.error_message`；确认扩展名在允许列表、RAGFlow 解析器支持该格式 |
| 助手返回 `INSUFFICIENT_EVIDENCE` | 检索命中不足（阈值 `similarity-threshold` 0.2）；确认空间内有 `READY` 文档且用户有业务可见性 |
| 助手返回 `FORBIDDEN` | 当前用户对指定空间或业务对象无权限 |
| 上传 413 | Nginx `client_max_body_size`、`spring.servlet.multipart.max-file-size`（52MB）需同时放开 |
| 分页/导出 401 | 前端 `VITE_APP_BASE_API` 与后端地址不一致，或 Redis 中令牌已失效 |
| 审计查询为空 | `kb_query_audit` 仅由助手问答写入；先完成一次提问 |

日志：`logs/sys-info.log`、`logs/sys-error.log`；RAGFlow 调用异常只记录状态码与消息，不记录 API Key，也不向前端返回堆栈。

## 8. 接口速览（前端与联调依据）

ERP 资源统一 CRUD：`GET /list`、`POST /export`、`GET /{id}`、`POST /`、`PUT /`、`DELETE /{ids}`。

| 资源 | 基路径 | 规格动作端点 |
| --- | --- | --- |
| 供应商 | `/erp/supplier` | `POST /{id}/submit`、`POST /{id}/approve`、`POST /{id}/reject` |
| 合同 | `/erp/contract` | `POST /{id}/submit`、`POST /{id}/activate`、`POST /{id}/reject`、`POST /{id}/terminate` |
| 采购申请 | `/erp/request` | `POST /{id}/submit`、`POST /{id}/approve`、`POST /{id}/reject` |
| 采购订单 | `/erp/order` | `POST /{id}/submit`、`POST /{id}/approve`、`POST /{id}/reject`、`POST /{id}/cancel` |
| 到货入库 | `/erp/receipt` | `POST /{id}/confirm`（幂等）、`POST /{id}/reverse` |
| 库存余额 | `/erp/inventory` | `GET /list` 及规格别名 `GET /balance/list` |
| 库存流水 | `/erp/inventory/ledger` | `GET /list` |
| 驾驶舱 | `/erp/dashboard` | `GET /overview`（别名 `/summary`）、`/pending`、`/low-stock`、`/receipt-trend?days=` |

工作流兼容端点（保留给既有前端与脚本）：`/erp/workflow/supplier/audit/{id}`、`/contract/submit|approve|terminate/{id}`、`/request/submit|approve/{id}`、`/order/submit|approve|cancel/{id}`、`/receipt/confirm|reverse/{id}`、`GET /approval/list?businessType=&businessId=`。

知识模块：`/knowledge/space`（含 `/{id}/disable`、`/acl/{spaceId}`）、`/knowledge/document`（`/upload`、`/{id}/download`、`/{id}/preview`、`/{id}/retry`、`/{id}/disable`、`/{id}/link`、`/by-business`）、`/knowledge/audit`（`/queries`、`/queries/{traceId}`、`/overview`）、`/assistant`（`/sessions`、`/ask` SSE、`/health`）。

所有写接口均带 `@Log`，写入若依操作日志 `sys_oper_log`，便于审计追溯。

## 9. 构建与测试

```bash
# 后端单元测试（状态机、库存结转、幂等、单号生成、知识权限交集）
$env:JAVA_HOME="C:\Program Files\Java\jdk-23"
mvn -pl ruoyi-erp,ruoyi-knowledge -am test

# 后端打包
mvn -DskipTests package

# 前端生产构建
cd ruoyi-ui; npm run build:prod
```


当前自动化测试：`ErpWorkflowServiceTest`（15 例，含重复入库幂等与冲销库存不足）、`ErpNoGeneratorTest`（5 例）、`KbPermissionServiceTest`（7 例，覆盖"空间 ACL ∩ 业务可见性 ∩ READY 且 is_current=1"交集）。前端为构建级验证，端到端流程按第 6 章人工验收。

### 本机快速启动 / 停止

仓库根目录提供两个脚本，启动后进程脱离当前终端运行，日志落在 `logs/app-<时间戳>.log`：

```powershell
# 启动（默认端口 18085，库口令 123456，JWT 密钥内置）
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-demo.ps1
# 自定义端口 / 口令
.\start-demo.ps1 -Port 8080 -DbPassword "yourpass"

# 停止（按命令行匹配 ruoyi-admin 的 java 进程）
powershell -NoProfile -ExecutionPolicy Bypass -File .\stop-demo.ps1
```

启动前确保 Redis 可用（本机验证环境用容器）：

```powershell
docker run -d --name erp-verify-redis -p 6379:6379 redis:7-alpine
```

前端开发模式：`cd ruoyi-ui; npm run dev`（构建产物用 `npm run build:prod`，已构建的 `dist` 可直接交给 Nginx）。


## 10. 目录速览

```text
JL_ERP/
├─ ruoyi-admin/          启动模块（application.yml、ragflow/erp 配置）
├─ ruoyi-erp/            采购与库存业务（domain/mapper/service/controller + ErpWorkflow）
├─ ruoyi-knowledge/      知识空间/文档/索引/助手/审计 + ragflow 客户端
├─ ruoyi-common/         IBusinessObjectService 等跨模块契约
├─ ruoyi-ui/             Vue3 前端（views/erp、views/knowledge、views/assistant、components/AssistantPanel）
├─ sql/erp_knowledge_20261101.sql  初始化 + 演示数据（可重复执行）
└─ docs/deployment.md    本手册
```

## 11. 实测验证记录（2026-11-02）

以下结果来自本机真实环境联调：MySQL 8（库 `ry-vue`）+ Redis 7（Docker）+ `ruoyi-admin.jar`（端口 18085），**RAGFlow 未部署**，用于同时验证"知识服务不可用时 ERP 业务不受影响"的降级要求。

| 验证项 | 结果 |
| --- | --- |
| `/actuator/health` | `{"groups":["liveness","readiness"],"status":"UP"}` |
| 登录 + 菜单权限 | admin/admin123 取得 token，`/getInfo` 返回 `*:*:*`，13 个业务菜单组件全部可加载 |
| 驾驶舱 | supplierTotal 3、contractActive 2、inventoryAmount 81050.00、orderOverdue 1、orderPending 1、requestPending 1、lowStockCount 1、receiptTotal 2 |
| 采购申请 CRUD | 新增生成 PR-2026-0007（DRAFT），明细回显 M-1001×2；缺仓库返回"请选择需求仓库" |
| 采购订单 CRUD | 新增生成 PO-2026-0005（DRAFT），明细 M-1001×3@100；缺供应商返回"请选择供应商" |
| 审批 → 入库 → 库存 → 冲销 | 对 PO-2026-0002 审批通过 → 建入库单（仓库 1，M-2001 到货 50）→ 确认两次（第二次幂等无副作用）→ 余额 50/41000.00/均价 820，流水 RECEIPT，订单置 RECEIVED，审批记录"入库确认" → 冲销后余额归零、订单回 APPROVED |
| 知识空间/文档 | `/knowledge/space/list` 3 条、`/knowledge/document/list` 3 条（演示文档为 UPLOADED，未接 RAGFlow 时不参与检索） |
| 助手 SSE（RAGFlow 不可用） | 200，`text/event-stream;charset=UTF-8`，按序收到 `meta` → `delta`（"当前没有可检索的文档，请先上传并完成索引"）→ `done`（INSUFFICIENT_EVIDENCE），流正常结束无截断 |
| 问答审计 | `/knowledge/audit/queries` 正常落库，`/knowledge/audit/overview` 返回 topQuestions 与 recentFailures |
| 单元测试 | `ErpWorkflowServiceTest` 15 + `ErpNoGeneratorTest` 5 + `KbPermissionServiceTest` 7，全绿；前后端构建均成功 |

实测中修复的缺陷（均已合入代码与 SQL）：

1. `kb_query_audit.status` 长度不足：`INSUFFICIENT_EVIDENCE`（22 字符）超 `varchar(20)` → 建表脚本改为 `varchar(32)`。
2. SSE 异步派发鉴权失败：`JwtAuthenticationTokenFilter` 默认跳过 ASYNC dispatch，`emitter.complete()` 时 `@PreAuthorize` 判匿名导致 chunked 响应缺结束块（`curl: (18)`）→ 覆盖 `shouldNotFilterAsyncDispatch()` 返回 `false`。
3. 明细写入列不存在：三张明细表的 insert 误写 `material_code/material_name/specification/unit/dept_id`（这些字段由 `left join erp_material` 提供）→ 改为只写真实列并补 `create_by/create_time`。

> 若再次执行本手册第 4 章的初始化脚本，库内数据会回到演示初始态（4 张订单、6 张申请、2 张入库单、3 份知识文档、0 条审计）。

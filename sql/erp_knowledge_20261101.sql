-- ----------------------------
-- 智能 ERP + 知识引擎（RAGFlow）融合模块初始化脚本
-- 数据库：MySQL 8.0+  字符集：utf8mb4
-- 依赖：须先执行 ry_20260417.sql 与 quartz.sql
-- 内容：一、业务表结构；二、字典数据；三、菜单数据；四、角色与授权；五、组织与用户；六~十三、演示数据
-- 说明：业务表使用 drop table if exists，可重复执行；sys_* 相关数据先做定向清理，同样支持重复执行。
-- ----------------------------

set names utf8mb4;

-- ----------------------------
-- 零、重复执行清理
-- ----------------------------
delete from sys_role_menu where role_id between 2001 and 2006;
delete from sys_user_role where user_id between 200 and 205;
delete from sys_role where role_id between 2001 and 2006;
delete from sys_user where user_id between 200 and 205;
delete from sys_menu where menu_id between 2000 and 3999;
delete from sys_dict_data where dict_code >= 100 and dict_type like 'erp\_%';
delete from sys_dict_type where dict_id >= 100 and dict_type like 'erp\_%';
delete from sys_dept where dept_id between 200 and 204;

-- ----------------------------
-- 一、业务表结构
-- ----------------------------
drop table if exists erp_supplier;
create table erp_supplier (
  id               bigint          not null auto_increment,
  supplier_code    varchar(32)     not null                  comment '供应商编码',
  supplier_name    varchar(200)    not null                  comment '供应商名称',
  credit_code      varchar(64)     not null                  comment '统一社会信用代码',
  supplier_category varchar(64)    default null              comment '供应商分类',
  contact_name     varchar(64)     default null              comment '联系人',
  contact_phone    varchar(32)     default null              comment '联系电话',
  email            varchar(128)    default null              comment '邮箱',
  address          varchar(500)    default null              comment '地址',
  bank_name        varchar(200)    default null              comment '开户银行',
  bank_account     varchar(128)    default null              comment '银行账号',
  tax_no           varchar(64)     default null              comment '税号',
  admission_status varchar(20)     not null                  comment '准入状态',
  risk_level       varchar(20)     not null default 'LOW'    comment '风险等级',
  owner_id         bigint          default null              comment '负责人',
  admission_time   datetime        default null              comment '准入时间',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_supplier_code (supplier_code),
  unique key uk_credit_code (credit_code),
  key idx_dept_status (dept_id, admission_status)
) engine=innodb comment = '供应商档案';

drop table if exists erp_material;
create table erp_material (
  id               bigint          not null auto_increment,
  material_code    varchar(32)     not null                  comment '物料编码',
  material_name    varchar(200)    not null                  comment '物料名称',
  specification    varchar(200)    default null              comment '规格型号',
  unit             varchar(20)     not null                  comment '单位',
  material_category varchar(64)    default null              comment '物料分类',
  status           char(1)         not null default '0'      comment '状态（0正常 1停用）',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_material_code (material_code)
) engine=innodb comment = '物料资料';

drop table if exists erp_warehouse;
create table erp_warehouse (
  id               bigint          not null auto_increment,
  warehouse_code   varchar(32)     not null                  comment '仓库编码',
  warehouse_name   varchar(100)    not null                  comment '仓库名称',
  manager_id       bigint          default null              comment '仓库负责人',
  status           char(1)         not null default '0'      comment '状态（0正常 1停用）',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_warehouse_code (warehouse_code)
) engine=innodb comment = '仓库资料';

drop table if exists erp_contract;
create table erp_contract (
  id               bigint          not null auto_increment,
  contract_no      varchar(32)     not null                  comment '合同编号',
  contract_name    varchar(200)    not null                  comment '合同名称',
  supplier_id      bigint          not null                  comment '供应商ID',
  start_date       date            not null                  comment '生效日期',
  end_date         date            not null                  comment '到期日期',
  contract_amount  decimal(18,2)   not null                  comment '合同金额',
  currency         char(3)         not null default 'CNY'    comment '币种',
  payment_terms    varchar(1000)   default null              comment '付款条件',
  contract_status  varchar(20)     not null                  comment '合同状态',
  owner_id         bigint          not null                  comment '负责人',
  signed_date      date            default null              comment '签订日期',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_contract_no (contract_no),
  key idx_supplier_id (supplier_id),
  key idx_status_end_date (contract_status, end_date)
) engine=innodb comment = '合同台账';

drop table if exists erp_purchase_request;
create table erp_purchase_request (
  id               bigint          not null auto_increment,
  request_no       varchar(32)     not null                  comment '申请单号',
  title            varchar(200)    not null                  comment '申请标题',
  applicant_id     bigint          not null                  comment '申请人',
  required_date    date            not null                  comment '需求日期',
  target_warehouse_id bigint       not null                  comment '目标仓库',
  request_reason   varchar(1000)   default null              comment '申请原因',
  estimated_amount decimal(18,2)   not null default 0        comment '预估金额',
  request_status   varchar(20)     not null                  comment '申请状态',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_request_no (request_no),
  key idx_dept_status (dept_id, request_status)
) engine=innodb comment = '采购申请';

drop table if exists erp_purchase_request_item;
create table erp_purchase_request_item (
  id               bigint          not null auto_increment,
  request_id       bigint          not null                  comment '申请ID',
  material_id      bigint          not null                  comment '物料ID',
  quantity         decimal(18,4)   not null                  comment '数量',
  estimated_price  decimal(18,2)   not null                  comment '预估单价',
  estimated_amount decimal(18,2)   not null                  comment '预估金额',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  key idx_request_id (request_id),
  key idx_material_id (material_id)
) engine=innodb comment = '采购申请明细';

drop table if exists erp_purchase_order;
create table erp_purchase_order (
  id               bigint          not null auto_increment,
  order_no         varchar(32)     not null                  comment '订单号',
  request_id       bigint          not null                  comment '采购申请ID',
  supplier_id      bigint          not null                  comment '供应商ID',
  contract_id      bigint          default null              comment '合同ID',
  order_date       date            not null                  comment '下单日期',
  expected_date    date            not null                  comment '期望到货日期',
  total_amount     decimal(18,2)   not null                  comment '订单总额',
  order_status     varchar(20)     not null                  comment '订单状态',
  approval_comment varchar(500)    default null              comment '审批意见',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_order_no (order_no),
  key idx_supplier_id (supplier_id),
  key idx_request_id (request_id),
  key idx_dept_status (dept_id, order_status)
) engine=innodb comment = '采购订单';

drop table if exists erp_purchase_order_item;
create table erp_purchase_order_item (
  id               bigint          not null auto_increment,
  order_id         bigint          not null                  comment '订单ID',
  material_id      bigint          not null                  comment '物料ID',
  quantity         decimal(18,4)   not null                  comment '订购数量',
  received_quantity decimal(18,4)  not null default 0        comment '累计到货数量',
  unit_price       decimal(18,2)   not null                  comment '单价',
  amount           decimal(18,2)   not null                  comment '金额',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_order_material (order_id, material_id),
  key idx_material_id (material_id)
) engine=innodb comment = '采购订单明细';

drop table if exists erp_receipt;
create table erp_receipt (
  id               bigint          not null auto_increment,
  receipt_no       varchar(32)     not null                  comment '入库单号',
  order_id         bigint          not null                  comment '订单ID',
  warehouse_id     bigint          not null                  comment '仓库ID',
  receipt_date     date            not null                  comment '到货日期',
  quality_note     varchar(1000)   default null              comment '质检说明',
  receipt_status   varchar(20)     not null                  comment '入库状态',
  confirmed_by     bigint          default null              comment '确认人',
  confirmed_time   datetime        default null              comment '确认时间',
  idempotency_key  varchar(64)     not null                  comment '幂等键',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_receipt_no (receipt_no),
  unique key uk_idempotency_key (idempotency_key),
  key idx_order_id (order_id),
  key idx_warehouse_id (warehouse_id)
) engine=innodb comment = '到货入库单';

drop table if exists erp_receipt_item;
create table erp_receipt_item (
  id               bigint          not null auto_increment,
  receipt_id       bigint          not null                  comment '入库单ID',
  order_item_id    bigint          not null                  comment '订单明细ID',
  material_id      bigint          not null                  comment '物料ID',
  received_quantity decimal(18,4)  not null                  comment '到货数量',
  qualified_quantity decimal(18,4) not null                  comment '合格数量',
  rejected_quantity decimal(18,4)  not null                  comment '不合格数量',
  unit_cost        decimal(18,2)   not null                  comment '单位成本',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  key idx_receipt_id (receipt_id),
  key idx_order_item_id (order_item_id)
) engine=innodb comment = '到货入库明细';

drop table if exists erp_inventory_ledger;
create table erp_inventory_ledger (
  id               bigint          not null auto_increment,
  warehouse_id     bigint          not null                  comment '仓库ID',
  material_id      bigint          not null                  comment '物料ID',
  business_type    varchar(20)     not null                  comment '业务类型（RECEIPT入库 REVERSAL冲销）',
  source_type      varchar(20)     not null                  comment '来源类型（RECEIPT）',
  source_id        bigint          not null                  comment '来源单据ID',
  source_item_id   bigint          not null                  comment '来源明细ID',
  change_quantity  decimal(18,4)   not null                  comment '变动数量',
  unit_cost        decimal(18,2)   not null                  comment '单位成本',
  balance_quantity decimal(18,4)   not null                  comment '变动后结存',
  occurred_time    datetime        not null                  comment '发生时间',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  key idx_warehouse_material (warehouse_id, material_id),
  unique key uk_source_item (source_type, source_item_id, business_type)
) engine=innodb comment = '库存流水';

drop table if exists erp_inventory_balance;
create table erp_inventory_balance (
  id               bigint          not null auto_increment,
  warehouse_id     bigint          not null                  comment '仓库ID',
  material_id      bigint          not null                  comment '物料ID',
  available_quantity decimal(18,4) not null                  comment '可用数量',
  average_cost     decimal(18,4)   not null                  comment '移动平均成本',
  inventory_amount decimal(18,2)   not null                  comment '库存金额',
  last_business_time datetime      default null              comment '最后业务时间',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_warehouse_material (warehouse_id, material_id)
) engine=innodb comment = '库存余额';

drop table if exists erp_approval_record;
create table erp_approval_record (
  id               bigint          not null auto_increment,
  business_type    varchar(20)     not null                  comment '业务类型',
  business_id      bigint          not null                  comment '业务ID',
  action           varchar(20)     not null                  comment '动作',
  from_status      varchar(20)     default null              comment '原状态',
  to_status        varchar(20)     not null                  comment '新状态',
  opinion          varchar(500)    default null              comment '审批意见',
  operator_id      bigint          not null                  comment '操作人ID',
  operator_name    varchar(64)     not null                  comment '操作人名称',
  operate_time     datetime        not null                  comment '操作时间',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  key idx_business (business_type, business_id, operate_time)
) engine=innodb comment = '审批记录';

drop table if exists kb_space;
create table kb_space (
  id               bigint          not null auto_increment,
  space_name       varchar(100)    not null                  comment '知识空间名称',
  space_type       varchar(20)     not null                  comment '空间类型',
  rag_dataset_id   varchar(64)     not null                  comment 'RAGFlow 数据集ID',
  description      varchar(500)    default null              comment '描述',
  owner_id         bigint          not null                  comment '负责人',
  space_status     varchar(20)     not null                  comment '空间状态',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  remark           varchar(500)    default null,
  version          int             not null default 0,
  primary key (id),
  unique key uk_rag_dataset_id (rag_dataset_id)
) engine=innodb comment = '知识空间';

drop table if exists kb_space_acl;
create table kb_space_acl (
  id               bigint          not null auto_increment,
  space_id         bigint          not null                  comment '空间ID',
  subject_type     varchar(20)     not null                  comment '授权对象类型',
  subject_id       bigint          not null                  comment '授权对象ID',
  permission_level varchar(20)     not null                  comment '权限级别',
  create_by        varchar(64)     default '',
  create_time      datetime,
  primary key (id),
  unique key uk_space_subject (space_id, subject_type, subject_id)
) engine=innodb comment = '知识空间授权';

drop table if exists kb_document;
create table kb_document (
  id               bigint          not null auto_increment,
  space_id         bigint          not null                  comment '空间ID',
  document_name    varchar(255)    not null                  comment '文档名称',
  stable_key       varchar(64)     not null                  comment '稳定键',
  document_version int             not null default 1        comment '版本号',
  is_current       tinyint         not null default 1        comment '是否当前有效版本',
  file_path        varchar(500)    not null                  comment '存储路径',
  file_size        bigint          not null                  comment '文件大小',
  mime_type        varchar(100)    not null                  comment 'MIME类型',
  checksum_sha256  char(64)        not null                  comment 'SHA-256',
  rag_document_id  varchar(64)     default null              comment 'RAGFlow 文档ID',
  parse_status     varchar(20)     not null                  comment '解析状态',
  chunk_count      int             not null default 0        comment '分块数量',
  token_count      int             not null default 0        comment 'Token数量',
  error_message    varchar(1000)   default null              comment '错误信息',
  uploaded_by      bigint          not null                  comment '上传人',
  ready_time       datetime        default null              comment '就绪时间',
  dept_id          bigint          not null default 0,
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  version          int             not null default 0,
  primary key (id),
  unique key uk_stable_version (space_id, stable_key, document_version),
  key idx_space_current (space_id, is_current),
  key idx_rag_document_id (rag_document_id)
) engine=innodb comment = '知识文档';

drop table if exists kb_object_link;
create table kb_object_link (
  id               bigint          not null auto_increment,
  document_id      bigint          not null                  comment '文档ID',
  business_type    varchar(20)     not null                  comment '业务类型',
  business_id      bigint          not null                  comment '业务ID',
  relation_type    varchar(20)     not null                  comment '关联类型',
  create_by        varchar(64)     default '',
  create_time      datetime,
  primary key (id),
  unique key uk_doc_business (document_id, business_type, business_id, relation_type),
  key idx_business (business_type, business_id),
  key idx_document (document_id)
) engine=innodb comment = '文档业务关联';

drop table if exists kb_index_job;
create table kb_index_job (
  id               bigint          not null auto_increment,
  document_id      bigint          not null                  comment '文档ID',
  job_status       varchar(20)     not null                  comment '任务状态',
  attempt_count    int             not null default 0        comment '尝试次数',
  next_retry_time  datetime        default null              comment '下次重试时间',
  started_time     datetime        default null              comment '开始时间',
  finished_time    datetime        default null              comment '完成时间',
  error_message    varchar(1000)   default null              comment '错误信息',
  create_time      datetime,
  update_time      datetime,
  primary key (id),
  unique key uk_document_id (document_id)
) engine=innodb comment = '索引任务';

drop table if exists kb_assistant_session;
create table kb_assistant_session (
  id               bigint          not null auto_increment,
  user_id          bigint          not null                  comment '用户ID',
  title            varchar(200)    not null                  comment '会话标题',
  context_type     varchar(20)     default null              comment '上下文类型',
  context_id       bigint          default null              comment '上下文ID',
  session_status   varchar(20)     not null default 'ACTIVE' comment '会话状态',
  create_by        varchar(64)     default '',
  create_time      datetime,
  update_by        varchar(64)     default '',
  update_time      datetime,
  primary key (id),
  key idx_user_id (user_id, create_time)
) engine=innodb comment = '助手会话';

drop table if exists kb_assistant_message;
create table kb_assistant_message (
  id               bigint          not null auto_increment,
  session_id       bigint          not null                  comment '会话ID',
  role             varchar(20)     not null                  comment '角色',
  content          longtext        not null                  comment '内容',
  model_name       varchar(100)    default null              comment '模型',
  latency_ms       int             default null              comment '耗时',
  trace_id         char(32)        not null                  comment '追踪ID',
  created_time     datetime        not null                  comment '创建时间',
  primary key (id),
  key idx_session_id (session_id, id)
) engine=innodb comment = '助手消息';

drop table if exists kb_answer_citation;
create table kb_answer_citation (
  id               bigint          not null auto_increment,
  message_id       bigint          not null                  comment '消息ID',
  source_type      varchar(20)     not null                  comment '来源类型',
  source_id        bigint          not null                  comment '来源ID',
  rag_chunk_id     varchar(64)     default null              comment 'RAGFlow Chunk ID',
  rag_document_id  varchar(64)     default null              comment 'RAGFlow 文档ID',
  page_number      int             default null              comment '页码',
  snippet          text                                      comment '命中片段',
  similarity_score decimal(9,6)    default null              comment '相似度',
  citation_order   int             not null                  comment '引用顺序',
  primary key (id),
  key idx_message_id (message_id, citation_order)
) engine=innodb comment = '回答引用';

drop table if exists kb_query_audit;
create table kb_query_audit (
  id               bigint          not null auto_increment,
  trace_id         char(32)        not null                  comment '追踪ID',
  user_id          bigint          not null                  comment '用户ID',
  dept_id          bigint          default null              comment '部门ID',
  question         text            not null                  comment '问题',
  context_type     varchar(20)     default null              comment '上下文类型',
  context_id       bigint          default null              comment '上下文ID',
  space_ids_json   json            not null                  comment '空间范围',
  document_ids_json json           not null                  comment '文档范围',
  status           varchar(32)     not null                  comment '结果状态',
  model_name       varchar(100)    default null              comment '模型',
  latency_ms       int             default null              comment '耗时',
  error_message    varchar(1000)   default null              comment '错误信息',
  created_time     datetime        not null                  comment '创建时间',
  primary key (id),
  unique key uk_trace_id (trace_id),
  key idx_user_created (user_id, created_time),
  key idx_status (status)
) engine=innodb comment = '问答审计';

-- ----------------------------
-- 二、字典数据
-- ----------------------------
insert into sys_dict_type values(100, '供应商准入状态', 'erp_supplier_status', '0', 'admin', sysdate(), '', null, '供应商准入状态');
insert into sys_dict_data values(100, 1, '待准入', 'PENDING', 'erp_supplier_status', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '新建未准入');
insert into sys_dict_data values(101, 2, '已准入', 'APPROVED', 'erp_supplier_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '通过准入审核');
insert into sys_dict_data values(102, 3, '已拒绝', 'REJECTED', 'erp_supplier_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '准入被拒绝');
insert into sys_dict_data values(103, 4, '已暂停', 'SUSPENDED', 'erp_supplier_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '合作暂停');
insert into sys_dict_type values(101, '供应商风险等级', 'erp_supplier_risk', '0', 'admin', sysdate(), '', null, '供应商风险等级');
insert into sys_dict_data values(110, 1, '低', 'LOW', 'erp_supplier_risk', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '低风险');
insert into sys_dict_data values(111, 2, '中', 'MEDIUM', 'erp_supplier_risk', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '中风险');
insert into sys_dict_data values(112, 3, '高', 'HIGH', 'erp_supplier_risk', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '高风险');
insert into sys_dict_type values(102, '合同状态', 'erp_contract_status', '0', 'admin', sysdate(), '', null, '合同状态');
insert into sys_dict_data values(120, 1, '草稿', 'DRAFT', 'erp_contract_status', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '起草中');
insert into sys_dict_data values(121, 2, '待审批', 'PENDING_APPROVAL', 'erp_contract_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '等待审批');
insert into sys_dict_data values(122, 3, '生效中', 'ACTIVE', 'erp_contract_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '合同生效');
insert into sys_dict_data values(123, 4, '已到期', 'EXPIRED', 'erp_contract_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '合同到期');
insert into sys_dict_data values(124, 5, '已终止', 'TERMINATED', 'erp_contract_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '提前终止');
insert into sys_dict_type values(103, '采购申请状态', 'erp_request_status', '0', 'admin', sysdate(), '', null, '采购申请状态');
insert into sys_dict_data values(130, 1, '草稿', 'DRAFT', 'erp_request_status', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '未提交');
insert into sys_dict_data values(131, 2, '待审批', 'PENDING_APPROVAL', 'erp_request_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '等待审批');
insert into sys_dict_data values(132, 3, '已批准', 'APPROVED', 'erp_request_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '审批通过');
insert into sys_dict_data values(133, 4, '已驳回', 'REJECTED', 'erp_request_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '审批驳回');
insert into sys_dict_data values(134, 5, '已关闭', 'CLOSED', 'erp_request_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '已关闭');
insert into sys_dict_type values(104, '采购订单状态', 'erp_order_status', '0', 'admin', sysdate(), '', null, '采购订单状态');
insert into sys_dict_data values(140, 1, '草稿', 'DRAFT', 'erp_order_status', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '未提交');
insert into sys_dict_data values(141, 2, '待审批', 'PENDING_APPROVAL', 'erp_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '等待审批');
insert into sys_dict_data values(142, 3, '已批准', 'APPROVED', 'erp_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '审批通过');
insert into sys_dict_data values(143, 4, '部分到货', 'PARTIALLY_RECEIVED', 'erp_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '部分商品已入库');
insert into sys_dict_data values(144, 5, '已到货', 'RECEIVED', 'erp_order_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '全部入库');
insert into sys_dict_data values(145, 6, '已驳回', 'REJECTED', 'erp_order_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '审批驳回');
insert into sys_dict_data values(146, 7, '已取消', 'CANCELLED', 'erp_order_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '订单取消');
insert into sys_dict_type values(105, '入库单状态', 'erp_receipt_status', '0', 'admin', sysdate(), '', null, '入库单状态');
insert into sys_dict_data values(150, 1, '待确认', 'DRAFT', 'erp_receipt_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '未确认入库');
insert into sys_dict_data values(151, 2, '已确认', 'CONFIRMED', 'erp_receipt_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已记账');
insert into sys_dict_data values(152, 3, '已冲销', 'REVERSED', 'erp_receipt_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '已冲销');
insert into sys_dict_type values(106, '库存业务类型', 'erp_inventory_business', '0', 'admin', sysdate(), '', null, '库存业务类型');
insert into sys_dict_data values(160, 1, '入库', 'RECEIPT', 'erp_inventory_business', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '到货入库');
insert into sys_dict_data values(161, 2, '冲销', 'REVERSAL', 'erp_inventory_business', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '入库冲销');
insert into sys_dict_type values(107, '业务对象类型', 'erp_business_type', '0', 'admin', sysdate(), '', null, '业务对象类型');
insert into sys_dict_data values(170, 1, '供应商', 'SUPPLIER', 'erp_business_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '供应商档案');
insert into sys_dict_data values(171, 2, '合同', 'CONTRACT', 'erp_business_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '合同台账');
insert into sys_dict_data values(172, 3, '采购申请', 'PURCHASE_REQUEST', 'erp_business_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '采购申请单');
insert into sys_dict_data values(173, 4, '采购订单', 'PURCHASE_ORDER', 'erp_business_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '采购订单');
insert into sys_dict_data values(174, 5, '到货入库', 'RECEIPT', 'erp_business_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '到货入库单');
insert into sys_dict_type values(108, '知识空间类型', 'erp_space_type', '0', 'admin', sysdate(), '', null, '知识空间类型');
insert into sys_dict_data values(180, 1, '制度空间', 'POLICY', 'erp_space_type', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '制度规范类文档');
insert into sys_dict_data values(181, 2, '供应商空间', 'SUPPLIER', 'erp_space_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '供应商资质与评审');
insert into sys_dict_data values(182, 3, '合同空间', 'CONTRACT', 'erp_space_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '合同与协议附件');
insert into sys_dict_data values(183, 4, '采购空间', 'PROCUREMENT', 'erp_space_type', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '采购执行过程文档');
insert into sys_dict_type values(109, '知识空间状态', 'erp_space_status', '0', 'admin', sysdate(), '', null, '知识空间状态');
insert into sys_dict_data values(190, 1, '启用', 'ACTIVE', 'erp_space_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '正常可用');
insert into sys_dict_data values(191, 2, '停用', 'DISABLED', 'erp_space_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '已停用');
insert into sys_dict_type values(110, '文档解析状态', 'erp_document_status', '0', 'admin', sysdate(), '', null, '文档解析状态');
insert into sys_dict_data values(200, 1, '已上传', 'UPLOADED', 'erp_document_status', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '等待解析');
insert into sys_dict_data values(201, 2, '解析中', 'INDEXING', 'erp_document_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, 'RAGFlow 解析中');
insert into sys_dict_data values(202, 3, '已就绪', 'READY', 'erp_document_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '可被检索');
insert into sys_dict_data values(203, 4, '解析失败', 'FAILED', 'erp_document_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '解析失败');
insert into sys_dict_data values(204, 5, '已停用', 'DISABLED', 'erp_document_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '不再参与检索');
insert into sys_dict_type values(111, '授权对象类型', 'erp_acl_subject_type', '0', 'admin', sysdate(), '', null, '授权对象类型');
insert into sys_dict_data values(210, 1, '用户', 'USER', 'erp_acl_subject_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '系统用户');
insert into sys_dict_data values(211, 2, '角色', 'ROLE', 'erp_acl_subject_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '系统角色');
insert into sys_dict_data values(212, 3, '部门', 'DEPT', 'erp_acl_subject_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '组织部门');
insert into sys_dict_type values(112, '空间授权级别', 'erp_acl_permission', '0', 'admin', sysdate(), '', null, '空间授权级别');
insert into sys_dict_data values(220, 1, '只读', 'READ', 'erp_acl_permission', '', 'info', 'Y', '0', 'admin', sysdate(), '', null, '仅检索');
insert into sys_dict_data values(221, 2, '可写', 'WRITE', 'erp_acl_permission', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '可上传与解析');
insert into sys_dict_data values(222, 3, '管理', 'MANAGE', 'erp_acl_permission', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '可管理空间');
insert into sys_dict_type values(113, '问答审计状态', 'erp_audit_status', '0', 'admin', sysdate(), '', null, '问答审计状态');
insert into sys_dict_data values(230, 1, '成功', 'SUCCESS', 'erp_audit_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '正常回答');
insert into sys_dict_data values(231, 2, '无足够依据', 'INSUFFICIENT_EVIDENCE', 'erp_audit_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '未检索到足够证据');
insert into sys_dict_data values(232, 3, '引擎不可用', 'RAG_UNAVAILABLE', 'erp_audit_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, 'RAGFlow 不可用');
insert into sys_dict_data values(233, 4, '越权拒绝', 'FORBIDDEN', 'erp_audit_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '权限校验未通过');
insert into sys_dict_data values(234, 5, '失败', 'FAILED', 'erp_audit_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '其他失败');
insert into sys_dict_type values(114, '审批动作', 'erp_approval_action', '0', 'admin', sysdate(), '', null, '审批动作');
insert into sys_dict_data values(240, 1, '提交', 'SUBMIT', 'erp_approval_action', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '提交审批');
insert into sys_dict_data values(241, 2, '批准', 'APPROVE', 'erp_approval_action', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '审批通过');
insert into sys_dict_data values(242, 3, '驳回', 'REJECT', 'erp_approval_action', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '审批驳回');
insert into sys_dict_data values(243, 4, '取消', 'CANCEL', 'erp_approval_action', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '取消单据');
insert into sys_dict_data values(244, 5, '终止', 'TERMINATE', 'erp_approval_action', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '终止合同');
insert into sys_dict_data values(245, 6, '冲销', 'REVERSE', 'erp_approval_action', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '冲销入库');

-- ----------------------------
-- 三、菜单数据
-- ----------------------------
insert into sys_menu values(2000, '采购管理', 0, 5, 'erp', '', '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', null, '采购管理目录');
insert into sys_menu values(2001, '采购驾驶舱', 2000, 1, 'dashboard', 'erp/dashboard/index', '', '', 1, 0, 'C', '0', '0', 'erp:dashboard:view', 'dashboard', 'admin', sysdate(), '', null, '采购驾驶舱菜单');
insert into sys_menu values(2002, '供应商档案', 2000, 2, 'supplier', 'erp/supplier/index', '', '', 1, 0, 'C', '0', '0', 'erp:supplier:list', 'peoples', 'admin', sysdate(), '', null, '供应商档案菜单');
insert into sys_menu values(2003, '供应商查询', 2002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2004, '供应商新增', 2002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2005, '供应商修改', 2002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2006, '供应商删除', 2002, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2007, '供应商准入', 2002, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:approve', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2008, '供应商导出', 2002, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:supplier:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2010, '合同台账', 2000, 3, 'contract', 'erp/contract/index', '', '', 1, 0, 'C', '0', '0', 'erp:contract:list', 'form', 'admin', sysdate(), '', null, '合同台账菜单');
insert into sys_menu values(2011, '合同查询', 2010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2012, '合同新增', 2010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2013, '合同修改', 2010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2014, '合同删除', 2010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2015, '合同提交', 2010, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:submit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2016, '合同审批', 2010, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:approve', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2017, '合同终止', 2010, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:terminate', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2018, '合同导出', 2010, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:contract:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2020, '采购申请', 2000, 4, 'request', 'erp/purchase/request/index', '', '', 1, 0, 'C', '0', '0', 'erp:request:list', 'edit', 'admin', sysdate(), '', null, '采购申请菜单');
insert into sys_menu values(2021, '申请查询', 2020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2022, '申请新增', 2020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2023, '申请修改', 2020, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2024, '申请删除', 2020, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2025, '申请提交', 2020, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:submit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2026, '申请审批', 2020, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:approve', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2027, '申请导出', 2020, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:request:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2030, '采购订单', 2000, 5, 'order', 'erp/purchase/order/index', '', '', 1, 0, 'C', '0', '0', 'erp:order:list', 'list', 'admin', sysdate(), '', null, '采购订单菜单');
insert into sys_menu values(2031, '订单查询', 2030, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2032, '订单新增', 2030, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2033, '订单修改', 2030, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2034, '订单删除', 2030, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2035, '订单提交', 2030, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:submit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2036, '订单审批', 2030, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:approve', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2037, '订单取消', 2030, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:cancel', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2038, '订单导出', 2030, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:order:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2040, '到货入库', 2000, 6, 'receipt', 'erp/receipt/index', '', '', 1, 0, 'C', '0', '0', 'erp:receipt:list', 'download', 'admin', sysdate(), '', null, '到货入库菜单');
insert into sys_menu values(2041, '入库查询', 2040, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2042, '入库新增', 2040, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2043, '入库修改', 2040, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2044, '入库删除', 2040, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2045, '入库确认', 2040, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:confirm', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2046, '入库冲销', 2040, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:reverse', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2047, '入库导出', 2040, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:receipt:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2050, '库存查询', 2000, 7, 'inventory', 'erp/inventory/index', '', '', 1, 0, 'C', '0', '0', 'erp:inventory:list', 'table', 'admin', sysdate(), '', null, '库存查询菜单');
insert into sys_menu values(2051, '库存查询项', 2050, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2052, '库存导出', 2050, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2053, '库存流水', 2050, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:ledger', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2060, '基础资料', 2000, 8, 'base', '', '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', sysdate(), '', null, '基础资料目录');
insert into sys_menu values(2061, '物料资料', 2060, 1, 'material', 'erp/base/material/index', '', '', 1, 0, 'C', '0', '0', 'erp:material:list', 'component', 'admin', sysdate(), '', null, '物料资料菜单');
insert into sys_menu values(2062, '物料查询', 2061, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:material:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2063, '物料新增', 2061, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:material:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2064, '物料修改', 2061, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:material:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2065, '物料删除', 2061, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:material:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2066, '物料导出', 2061, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:material:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2070, '仓库资料', 2060, 2, 'warehouse', 'erp/base/warehouse/index', '', '', 1, 0, 'C', '0', '0', 'erp:warehouse:list', 'build', 'admin', sysdate(), '', null, '仓库资料菜单');
insert into sys_menu values(2071, '仓库查询', 2070, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:warehouse:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2072, '仓库新增', 2070, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:warehouse:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2073, '仓库修改', 2070, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:warehouse:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2074, '仓库删除', 2070, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:warehouse:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2075, '仓库导出', 2070, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:warehouse:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2054, '库存维护', 2050, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2055, '库存新增', 2050, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(2056, '库存删除', 2050, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3000, '知识中心', 0, 6, 'knowledge', '', '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '', null, '知识中心目录');
insert into sys_menu values(3001, '知识空间', 3000, 1, 'space', 'knowledge/space/index', '', '', 1, 0, 'C', '0', '0', 'knowledge:space:list', 'component', 'admin', sysdate(), '', null, '知识空间菜单');
insert into sys_menu values(3002, '空间查询', 3001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3003, '空间新增', 3001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3004, '空间修改', 3001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3005, '空间删除', 3001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3006, '空间授权', 3001, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:acl', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3007, '空间同步', 3001, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:space:sync', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3010, '知识文档', 3000, 2, 'document', 'knowledge/document/index', '', '', 1, 0, 'C', '0', '0', 'knowledge:document:list', 'document', 'admin', sysdate(), '', null, '知识文档菜单');
insert into sys_menu values(3011, '文档查询', 3010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3012, '文档上传', 3010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:upload', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3013, '文档修改', 3010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3014, '文档删除', 3010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3015, '重新解析', 3010, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:retry', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3016, '关联业务', 3010, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:link', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3017, '文档下载', 3010, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:document:download', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3020, '问答审计', 3000, 3, 'audit', 'knowledge/audit/index', '', '', 1, 0, 'C', '0', '0', 'knowledge:audit:list', 'log', 'admin', sysdate(), '', null, '问答审计菜单');
insert into sys_menu values(3021, '审计查询', 3020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:audit:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3022, '审计导出', 3020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:audit:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3100, '智能助手', 3000, 4, 'assistant', 'assistant/index', '', '', 1, 0, 'C', '0', '0', 'knowledge:assistant:ask', 'message', 'admin', sysdate(), '', null, '智能助手菜单');
insert into sys_menu values(3101, '助手会话', 3100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:assistant:ask', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(3102, '会话删除', 3100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'knowledge:assistant:ask', '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 四、角色与授权
-- ----------------------------
insert into sys_role values(2001, '采购负责人', 'purchase_manager', 1, 4, 1, 1, '0', '0', 'admin', sysdate(), '', null, '采购负责人角色');
insert into sys_role values(2002, '采购员', 'purchase_staff', 2, 4, 1, 1, '0', '0', 'admin', sysdate(), '', null, '采购员角色');
insert into sys_role values(2003, '部门负责人', 'dept_manager', 3, 4, 1, 1, '0', '0', 'admin', sysdate(), '', null, '部门负责人角色');
insert into sys_role values(2004, '仓管员', 'warehouse_keeper', 4, 4, 1, 1, '0', '0', 'admin', sysdate(), '', null, '仓管员角色');
insert into sys_role values(2005, '财务审计员', 'finance_auditor', 5, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '财务审计员角色');
insert into sys_role values(2006, '知识管理员', 'knowledge_admin', 6, 2, 1, 1, '0', '0', 'admin', sysdate(), '', null, '知识管理员角色');

-- 角色菜单授权
insert into sys_role_menu(role_id, menu_id) values (2001, 2000),(2001, 2001),(2001, 2002),(2001, 2003),(2001, 2004),(2001, 2005),(2001, 2007),(2001, 2008),(2001, 2010),(2001, 2011),(2001, 2012),(2001, 2013),(2001, 2015),(2001, 2016),(2001, 2017),(2001, 2018),(2001, 2020),(2001, 2021),(2001, 2022),(2001, 2023),(2001, 2025),(2001, 2026),(2001, 2027),(2001, 2030),(2001, 2031),(2001, 2032),(2001, 2033),(2001, 2035),(2001, 2036),(2001, 2037),(2001, 2038),(2001, 2040),(2001, 2041),(2001, 2045),(2001, 2050),(2001, 2051),(2001, 2052),(2001, 2053),(2001, 2060),(2001, 2061),(2001, 2062),(2001, 2070),(2001, 2071),(2001, 3000),(2001, 3001),(2001, 3002),(2001, 3010),(2001, 3011),(2001, 3017),(2001, 3100),(2001, 3101);
insert into sys_role_menu(role_id, menu_id) values (2002, 2000),(2002, 2001),(2002, 2002),(2002, 2003),(2002, 2004),(2002, 2005),(2002, 2008),(2002, 2010),(2002, 2011),(2002, 2012),(2002, 2013),(2002, 2015),(2002, 2018),(2002, 2020),(2002, 2021),(2002, 2022),(2002, 2023),(2002, 2025),(2002, 2027),(2002, 2030),(2002, 2031),(2002, 2032),(2002, 2033),(2002, 2035),(2002, 2038),(2002, 2040),(2002, 2041),(2002, 2042),(2002, 2050),(2002, 2051),(2002, 2053),(2002, 2060),(2002, 2061),(2002, 2062),(2002, 2070),(2002, 2071),(2002, 3000),(2002, 3010),(2002, 3011),(2002, 3017),(2002, 3100),(2002, 3101);
insert into sys_role_menu(role_id, menu_id) values (2003, 2000),(2003, 2001),(2003, 2002),(2003, 2003),(2003, 2010),(2003, 2011),(2003, 2016),(2003, 2020),(2003, 2021),(2003, 2026),(2003, 2030),(2003, 2031),(2003, 2036),(2003, 2040),(2003, 2041),(2003, 2050),(2003, 2051),(2003, 2060),(2003, 2061),(2003, 2062),(2003, 2070),(2003, 2071),(2003, 3000),(2003, 3100),(2003, 3101);
insert into sys_role_menu(role_id, menu_id) values (2004, 2000),(2004, 2001),(2004, 2030),(2004, 2031),(2004, 2040),(2004, 2041),(2004, 2042),(2004, 2043),(2004, 2045),(2004, 2050),(2004, 2051),(2004, 2052),(2004, 2053),(2004, 2060),(2004, 2061),(2004, 2062),(2004, 2070),(2004, 2071),(2004, 3000),(2004, 3100),(2004, 3101);
insert into sys_role_menu(role_id, menu_id) values (2005, 2000),(2005, 2001),(2005, 2002),(2005, 2003),(2005, 2008),(2005, 2010),(2005, 2011),(2005, 2018),(2005, 2020),(2005, 2021),(2005, 2030),(2005, 2031),(2005, 2040),(2005, 2041),(2005, 2050),(2005, 2051),(2005, 2052),(2005, 2053),(2005, 3000),(2005, 3020),(2005, 3021),(2005, 3022),(2005, 3100),(2005, 3101);
insert into sys_role_menu(role_id, menu_id) values (2006, 2000),(2006, 2002),(2006, 2003),(2006, 2010),(2006, 2011),(2006, 2030),(2006, 2031),(2006, 2040),(2006, 2041),(2006, 2050),(2006, 2051),(2006, 3000),(2006, 3001),(2006, 3002),(2006, 3003),(2006, 3004),(2006, 3005),(2006, 3006),(2006, 3007),(2006, 3010),(2006, 3011),(2006, 3012),(2006, 3013),(2006, 3014),(2006, 3015),(2006, 3016),(2006, 3017),(2006, 3020),(2006, 3021),(2006, 3022),(2006, 3100),(2006, 3101);

-- ----------------------------
-- 五、组织与用户
-- ----------------------------
insert into sys_dept values(200, 101, '0,100,101', '采购部', 1, '陈胜杰', '15800000001', 'caigou@demo.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(201, 101, '0,100,101', '仓储部', 2, '刘敏',   '15800000002', 'cangku@demo.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(202, 101, '0,100,101', '财务部', 3, '王强',   '15800000003', 'caiwu@demo.com',  '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(203, 101, '0,100,101', '生产部', 4, '赵磊',   '15800000004', 'shengchan@demo.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(204, 101, '0,100,101', '销售部', 5, '孙丽',   '15800000005', 'xiaoshou@demo.com', '0', '0', 'admin', sysdate(), '', null);

insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(200, 200, 'caigou',    '刘采购', '00', 'caigou@demo.com',    '15800000100', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '采购员');
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(201, 200, 'caigoufzr', '陈负责', '00', 'caigoufzr@demo.com', '15800000101', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '采购负责人');
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(202, 200, 'bmfzr',     '周经理', '00', 'bmfzr@demo.com',     '15800000102', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '部门负责人');
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(203, 201, 'cgy',       '刘仓管', '00', 'cgy@demo.com',       '15800000103', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '仓管员');
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(204, 202, 'shenji',    '王审计', '00', 'shenji@demo.com',    '15800000104', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '财务审计员');
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark) values
(205, 200, 'zhishi',    '郑知识', '00', 'zhishi@demo.com',    '15800000105', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '知识管理员');

insert into sys_user_role(user_id, role_id) values
(200, 2002), (201, 2001), (202, 2003), (203, 2004), (204, 2005), (205, 2006);

-- ----------------------------
-- 六、基础资料（物料 / 仓库 / 供应商）
-- ----------------------------
insert into erp_material(id, material_code, material_name, specification, unit, material_category, status, create_by, create_time, update_by, update_time, remark) values
(1, 'M-1001', '冷轧钢板',   '1.5mm*1250*2500', '吨', '金属材料', '0', 'admin', sysdate(), '', null, '常规生产用料'),
(2, 'M-1002', '铝合金型材', '6063-T5',         '米', '金属材料', '0', 'admin', sysdate(), '', null, '结构件用料'),
(3, 'M-2001', '伺服电机',   '750W 220V',       '台', '电气件',   '0', 'admin', sysdate(), '', null, '关键部件'),
(4, 'M-3001', '控制主板',   'ECB-V3.2',        '块', '电子件',   '0', 'admin', sysdate(), '', null, '含程序烧写'),
(5, 'M-9001', '包装纸箱',   '600*400*300',     '个', '包装材料', '0', 'admin', sysdate(), '', null, '外包装');

insert into erp_warehouse(id, warehouse_code, warehouse_name, manager_id, status, create_by, create_time, update_by, update_time, remark) values
(1, 'WH-SZ-01', '深圳主仓',   203, '0', 'admin', sysdate(), '', null, '原材料与成品主仓'),
(2, 'WH-CS-01', '长沙备件仓', 203, '0', 'admin', sysdate(), '', null, '售后备件仓');

insert into erp_supplier(id, supplier_code, supplier_name, credit_code, supplier_category, contact_name, contact_phone, email, address, bank_name, bank_account, tax_no, admission_status, risk_level, owner_id, admission_time, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, 'SUP-0001', '东莞市恒泰金属材料有限公司', '91441900MA4W1A2B3C', '金属材料', '李恒', '13800001001', 'liheng@hengta.com', '广东省东莞市长安镇工业大道 18 号', '中国银行东莞分行', '6222020000000001', '91441900MA4W1A2B3C', 'APPROVED', 'LOW', 201, DATE_SUB(sysdate(), INTERVAL 200 DAY), 200, 'admin', sysdate(), '', null, '长期合作钣金料供应商');
insert into erp_supplier(id, supplier_code, supplier_name, credit_code, supplier_category, contact_name, contact_phone, email, address, bank_name, bank_account, tax_no, admission_status, risk_level, owner_id, admission_time, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, 'SUP-0002', '苏州精密机电设备有限公司', '91320500MA1B2C3D4E', '电气件', '钱进', '13800001002', 'qianjin@szjm.com', '江苏省苏州市工业园区星湖街 66 号', '工商银行苏州分行', '6222020000000002', '91320500MA1B2C3D4E', 'APPROVED', 'MEDIUM', 201, DATE_SUB(sysdate(), INTERVAL 120 DAY), 200, 'admin', sysdate(), '', null, '伺服电机与电控件供应商');
insert into erp_supplier(id, supplier_code, supplier_name, credit_code, supplier_category, contact_name, contact_phone, email, address, bank_name, bank_account, tax_no, admission_status, risk_level, owner_id, admission_time, dept_id, create_by, create_time, update_by, update_time, remark) values
(3, 'SUP-0003', '广州包装制品有限公司', '91440100MA5D6E7F8G', '包装材料', '何包', '13800001003', 'hebao@gzpack.com', '广东省广州市白云区机场路 88 号', '建设银行广州分行', '6222020000000003', '91440100MA5D6E7F8G', 'PENDING', 'LOW', 200, null, 200, 'admin', sysdate(), '', null, '新引入包装供应商，待准入审核');

-- ----------------------------
-- 七、合同
-- ----------------------------
insert into erp_contract(id, contract_no, contract_name, supplier_id, start_date, end_date, contract_amount, currency, payment_terms, contract_status, owner_id, signed_date, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, 'HT-2026-0001', '2026 年度金属材料框架采购合同', 1, DATE_SUB(CURDATE(), INTERVAL 300 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 1200000.00, 'CNY', '月结 60 天，验收合格后付款', 'ACTIVE', 201, DATE_SUB(CURDATE(), INTERVAL 305 DAY), 200, 'admin', sysdate(), '', null, '年度框架合同，含价格联动条款');
insert into erp_contract(id, contract_no, contract_name, supplier_id, start_date, end_date, contract_amount, currency, payment_terms, contract_status, owner_id, signed_date, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, 'HT-2026-0002', '伺服电机及电控件采购合同', 2, DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 680000.00, 'CNY', '预付 30%，到货验收后付 70%', 'ACTIVE', 201, DATE_SUB(CURDATE(), INTERVAL 155 DAY), 200, 'admin', sysdate(), '', null, '合同即将到期，需评估续签');

-- ----------------------------
-- 八、采购申请
-- ----------------------------
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, 'PR-2026-0001', '三季度钣金件备料申请', 200, DATE_SUB(CURDATE(), INTERVAL 40 DAY), 1, '生产计划排产需要，补充冷轧钢板与铝合金型材库存', 45900.00, 'APPROVED', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 45 DAY), '', null, '已转采购订单 PO-2026-0001');
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, 'PR-2026-0002', '伺服电机例行补货申请', 200, DATE_ADD(CURDATE(), INTERVAL 15 DAY), 2, '装配线备件低于安全库存，需补货', 41000.00, 'APPROVED', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 20 DAY), '', null, '已转采购订单 PO-2026-0002');
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(3, 'PR-2026-0003', '研发试制物料申请', 200, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1, '新品试制需要少量控制主板与包装材料', 12000.00, 'DRAFT', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 3 DAY), '', null, '尚未提交');
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(4, 'PR-2026-0004', '控制主板季度采购申请', 200, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2, '售后备件补充，控制主板库存不足', 46000.00, 'APPROVED', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 35 DAY), '', null, '已转采购订单 PO-2026-0003');
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(5, 'PR-2026-0005', '包装材料年度集中采购申请', 200, DATE_ADD(CURDATE(), INTERVAL 25 DAY), 1, '包装材料集中采购以降低单价', 18000.00, 'PENDING_APPROVAL', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 5 DAY), '', null, '等待部门负责人审批');
insert into erp_purchase_request(id, request_no, title, applicant_id, required_date, target_warehouse_id, request_reason, estimated_amount, request_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(6, 'PR-2026-0006', '长沙备件仓补货申请', 200, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 2, '备件仓电机与主板库存偏低', 42800.00, 'APPROVED', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 70 DAY), '', null, '已转采购订单 PO-2026-0004');

insert into erp_purchase_request_item(request_id, material_id, quantity, estimated_price, estimated_amount) values
(1, 1, 600.0000, 42.50, 25500.00),
(1, 2, 300.0000, 68.00, 20400.00),
(2, 3, 50.0000, 820.00, 41000.00),
(3, 4, 5.0000, 1150.00, 5750.00),
(3, 5, 500.0000, 12.50, 6250.00),
(4, 4, 40.0000, 1150.00, 46000.00),
(5, 5, 1440.0000, 12.50, 18000.00),
(6, 3, 40.0000, 820.00, 32800.00),
(6, 4, 8.0000, 1250.00, 10000.00);

-- ----------------------------
-- 九、采购订单
-- ----------------------------
insert into erp_purchase_order(id, order_no, request_id, supplier_id, contract_id, order_date, expected_date, total_amount, order_status, approval_comment, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, 'PO-2026-0001', 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), 45900.00, 'PARTIALLY_RECEIVED', '价格与框架合同一致，同意下单', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 40 DAY), '', null, '已部分到货，来源申请 PR-2026-0001');
insert into erp_purchase_order(id, order_no, request_id, supplier_id, contract_id, order_date, expected_date, total_amount, order_status, approval_comment, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, 'PO-2026-0002', 2, 2, 2, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 41000.00, 'PENDING_APPROVAL', null, 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 15 DAY), '', null, '等待采购负责人审批');
insert into erp_purchase_order(id, order_no, request_id, supplier_id, contract_id, order_date, expected_date, total_amount, order_status, approval_comment, dept_id, create_by, create_time, update_by, update_time, remark) values
(3, 'PO-2026-0003', 4, 2, 2, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY), 46000.00, 'APPROVED', '紧急补货，同意执行', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 30 DAY), '', null, '已过期望到货日期仍未到货');
insert into erp_purchase_order(id, order_no, request_id, supplier_id, contract_id, order_date, expected_date, total_amount, order_status, approval_comment, dept_id, create_by, create_time, update_by, update_time, remark) values
(4, 'PO-2026-0004', 6, 2, 2, DATE_SUB(CURDATE(), INTERVAL 65 DAY), DATE_SUB(CURDATE(), INTERVAL 55 DAY), 42800.00, 'RECEIVED', '同意，按合同价执行', 200, 'caigou', DATE_SUB(sysdate(), INTERVAL 65 DAY), '', null, '已全部到货入库');

insert into erp_purchase_order_item(id, order_id, material_id, quantity, received_quantity, unit_price, amount) values
(1, 1, 1, 600.0000, 600.0000, 42.50, 25500.00),
(2, 1, 2, 300.0000, 200.0000, 68.00, 20400.00),
(3, 2, 3, 50.0000, 0.0000, 820.00, 41000.00),
(4, 3, 4, 40.0000, 0.0000, 1150.00, 46000.00),
(5, 4, 3, 40.0000, 40.0000, 820.00, 32800.00),
(6, 4, 4, 8.0000, 8.0000, 1250.00, 10000.00);

-- ----------------------------
-- 十、到货入库（已确认）
-- ----------------------------
insert into erp_receipt(id, receipt_no, order_id, warehouse_id, receipt_date, quality_note, receipt_status, confirmed_by, confirmed_time, idempotency_key, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, 'RC-2026-0001', 1, 1, DATE_SUB(CURDATE(), INTERVAL 18 DAY), '600 吨到货，抽检 20 吨表面锈蚀判定不合格，已退货', 'CONFIRMED', 203, DATE_SUB(sysdate(), INTERVAL 18 DAY), 'demo-receipt-0001', 201, 'cgy', DATE_SUB(sysdate(), INTERVAL 19 DAY), '', null, '首批到货，部分不合格');
insert into erp_receipt(id, receipt_no, order_id, warehouse_id, receipt_date, quality_note, receipt_status, confirmed_by, confirmed_time, idempotency_key, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, 'RC-2026-0002', 4, 2, DATE_SUB(CURDATE(), INTERVAL 52 DAY), '外观与功能检验全部合格', 'CONFIRMED', 203, DATE_SUB(sysdate(), INTERVAL 52 DAY), 'demo-receipt-0002', 201, 'cgy', DATE_SUB(sysdate(), INTERVAL 53 DAY), '', null, '长沙备件仓入库');

insert into erp_receipt_item(id, receipt_id, order_item_id, material_id, received_quantity, qualified_quantity, rejected_quantity, unit_cost) values
(1, 1, 1, 1, 600.0000, 580.0000, 20.0000, 42.50),
(2, 1, 2, 2, 200.0000, 200.0000, 0.0000, 68.00),
(3, 2, 5, 3, 40.0000, 40.0000, 0.0000, 820.00),
(4, 2, 6, 4, 8.0000, 8.0000, 0.0000, 1250.00);

-- ----------------------------
-- 十一、库存流水与余额
-- ----------------------------
insert into erp_inventory_ledger(warehouse_id, material_id, business_type, source_type, source_id, source_item_id, change_quantity, unit_cost, balance_quantity, occurred_time, create_by, create_time, remark) values
(1, 1, 'RECEIPT', 'RECEIPT', 1, 1, 580.0000, 42.50, 580.0000, DATE_SUB(sysdate(), INTERVAL 18 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 18 DAY), '入库单 RC-2026-0001 合格入库');
insert into erp_inventory_ledger(warehouse_id, material_id, business_type, source_type, source_id, source_item_id, change_quantity, unit_cost, balance_quantity, occurred_time, create_by, create_time, remark) values
(1, 2, 'RECEIPT', 'RECEIPT', 1, 2, 200.0000, 68.00, 200.0000, DATE_SUB(sysdate(), INTERVAL 18 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 18 DAY), '入库单 RC-2026-0001 合格入库');
insert into erp_inventory_ledger(warehouse_id, material_id, business_type, source_type, source_id, source_item_id, change_quantity, unit_cost, balance_quantity, occurred_time, create_by, create_time, remark) values
(2, 3, 'RECEIPT', 'RECEIPT', 2, 3, 40.0000, 820.00, 40.0000, DATE_SUB(sysdate(), INTERVAL 52 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 52 DAY), '入库单 RC-2026-0002 合格入库');
insert into erp_inventory_ledger(warehouse_id, material_id, business_type, source_type, source_id, source_item_id, change_quantity, unit_cost, balance_quantity, occurred_time, create_by, create_time, remark) values
(2, 4, 'RECEIPT', 'RECEIPT', 2, 4, 8.0000, 1250.00, 8.0000, DATE_SUB(sysdate(), INTERVAL 52 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 52 DAY), '入库单 RC-2026-0002 合格入库');

insert into erp_inventory_balance(warehouse_id, material_id, available_quantity, average_cost, inventory_amount, last_business_time, create_by, create_time, update_by, update_time) values
(1, 1, 580.0000, 42.5000, 24650.00, DATE_SUB(sysdate(), INTERVAL 18 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 18 DAY), '', null);
insert into erp_inventory_balance(warehouse_id, material_id, available_quantity, average_cost, inventory_amount, last_business_time, create_by, create_time, update_by, update_time) values
(1, 2, 200.0000, 68.0000, 13600.00, DATE_SUB(sysdate(), INTERVAL 18 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 18 DAY), '', null);
insert into erp_inventory_balance(warehouse_id, material_id, available_quantity, average_cost, inventory_amount, last_business_time, create_by, create_time, update_by, update_time) values
(2, 3, 40.0000, 820.0000, 32800.00, DATE_SUB(sysdate(), INTERVAL 52 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 52 DAY), '', null);
insert into erp_inventory_balance(warehouse_id, material_id, available_quantity, average_cost, inventory_amount, last_business_time, create_by, create_time, update_by, update_time) values
(2, 4, 8.0000, 1250.0000, 10000.00, DATE_SUB(sysdate(), INTERVAL 52 DAY), 'cgy', DATE_SUB(sysdate(), INTERVAL 52 DAY), '', null);

-- ----------------------------
-- 十二、审批记录
-- ----------------------------
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('CONTRACT', 1, 'SUBMIT', 'DRAFT', 'PENDING_APPROVAL', '提交年度框架合同审批', 201, '陈负责', DATE_SUB(sysdate(), INTERVAL 306 DAY), DATE_SUB(sysdate(), INTERVAL 306 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('CONTRACT', 1, 'APPROVE', 'PENDING_APPROVAL', 'ACTIVE', '条款审核通过，同意签署', 202, '周经理', DATE_SUB(sysdate(), INTERVAL 305 DAY), DATE_SUB(sysdate(), INTERVAL 305 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_REQUEST', 1, 'SUBMIT', 'DRAFT', 'PENDING_APPROVAL', '提交三季度备料申请', 200, '刘采购', DATE_SUB(sysdate(), INTERVAL 45 DAY), DATE_SUB(sysdate(), INTERVAL 45 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_REQUEST', 1, 'APPROVE', 'PENDING_APPROVAL', 'APPROVED', '需求合理，同意采购', 202, '周经理', DATE_SUB(sysdate(), INTERVAL 44 DAY), DATE_SUB(sysdate(), INTERVAL 44 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_REQUEST', 5, 'SUBMIT', 'DRAFT', 'PENDING_APPROVAL', '提交包装材料集中采购申请', 200, '刘采购', DATE_SUB(sysdate(), INTERVAL 5 DAY), DATE_SUB(sysdate(), INTERVAL 5 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_ORDER', 1, 'SUBMIT', 'DRAFT', 'PENDING_APPROVAL', '提交订单审批', 200, '刘采购', DATE_SUB(sysdate(), INTERVAL 40 DAY), DATE_SUB(sysdate(), INTERVAL 40 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_ORDER', 1, 'APPROVE', 'PENDING_APPROVAL', 'APPROVED', '价格与框架合同一致，同意下单', 201, '陈负责', DATE_SUB(sysdate(), INTERVAL 40 DAY), DATE_SUB(sysdate(), INTERVAL 40 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_ORDER', 2, 'SUBMIT', 'DRAFT', 'PENDING_APPROVAL', '提交伺服电机补货订单', 200, '刘采购', DATE_SUB(sysdate(), INTERVAL 15 DAY), DATE_SUB(sysdate(), INTERVAL 15 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('PURCHASE_ORDER', 3, 'APPROVE', 'PENDING_APPROVAL', 'APPROVED', '紧急补货，同意执行', 201, '陈负责', DATE_SUB(sysdate(), INTERVAL 30 DAY), DATE_SUB(sysdate(), INTERVAL 30 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('RECEIPT', 1, 'APPROVE', 'DRAFT', 'CONFIRMED', '确认入库，不合格品已退回', 203, '刘仓管', DATE_SUB(sysdate(), INTERVAL 18 DAY), DATE_SUB(sysdate(), INTERVAL 18 DAY));
insert into erp_approval_record(business_type, business_id, action, from_status, to_status, opinion, operator_id, operator_name, operate_time, create_time) values
('RECEIPT', 2, 'APPROVE', 'DRAFT', 'CONFIRMED', '检验合格，确认入库', 203, '刘仓管', DATE_SUB(sysdate(), INTERVAL 52 DAY), DATE_SUB(sysdate(), INTERVAL 52 DAY));

-- ----------------------------
-- 十三、知识空间演示数据
-- 说明：rag_dataset_id 为占位值，须通过"知识空间-同步"接口在 RAGFlow 中创建真实数据集后回填。
-- ----------------------------
insert into kb_space(id, space_name, space_type, rag_dataset_id, description, owner_id, space_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(1, '采购制度空间', 'POLICY', 'demo-dataset-policy-0001', '采购管理办法、供应商准入制度、合同管理规范等制度文件', 205, 'ACTIVE', 200, 'zhishi', sysdate(), '', null, '全员可读');
insert into kb_space(id, space_name, space_type, rag_dataset_id, description, owner_id, space_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(2, '供应商档案空间', 'SUPPLIER', 'demo-dataset-supplier-0001', '供应商资质文件、评审记录、框架合同附件', 205, 'ACTIVE', 200, 'zhishi', sysdate(), '', null, '采购与审计可读');

insert into kb_space(id, space_name, space_type, rag_dataset_id, description, owner_id, space_status, dept_id, create_by, create_time, update_by, update_time, remark) values
(3, '采购执行空间', 'PROCUREMENT', 'demo-dataset-procurement-0001', '采购申请、订单、到货入库相关的报价单、比价记录与验收单', 205, 'ACTIVE', 200, 'zhishi', sysdate(), '', null, '采购与仓储可读');

insert into kb_space_acl(space_id, subject_type, subject_id, permission_level, create_by, create_time) values
(1, 'ROLE', 2001, 'READ', 'zhishi', sysdate()),
(1, 'ROLE', 2002, 'READ', 'zhishi', sysdate()),
(1, 'ROLE', 2003, 'READ', 'zhishi', sysdate()),
(1, 'ROLE', 2004, 'READ', 'zhishi', sysdate()),
(1, 'ROLE', 2005, 'READ', 'zhishi', sysdate()),
(1, 'ROLE', 2006, 'MANAGE', 'zhishi', sysdate()),
(2, 'ROLE', 2001, 'WRITE', 'zhishi', sysdate()),
(2, 'ROLE', 2002, 'READ', 'zhishi', sysdate()),
(2, 'ROLE', 2005, 'READ', 'zhishi', sysdate()),
(2, 'ROLE', 2006, 'MANAGE', 'zhishi', sysdate()),
(1, 'ROLE', 1, 'MANAGE', 'zhishi', sysdate()),
(2, 'ROLE', 1, 'MANAGE', 'zhishi', sysdate()),
(3, 'ROLE', 1, 'MANAGE', 'zhishi', sysdate());

-- ----------------------------
-- 十五、知识文档示例（演示用；二进制文件需按部署文档上传后由 RAGFlow 解析）
-- 说明：示例文档的 parse_status 保持 UPLOADED / rag_document_id 为空，
--       因此不会参与检索，也不会产生不存在的引用；上传同 stable_key 的真实
--       文件后将形成新版本并自动切换当前有效版本。
-- ----------------------------
insert into kb_document(id, space_id, document_name, stable_key, document_version, is_current, file_path, file_size, mime_type, checksum_sha256, rag_document_id, parse_status, chunk_count, token_count, error_message, uploaded_by, ready_time, dept_id, create_by, create_time, update_by, update_time, version) values
(1, 2, '东莞市恒泰金属材料有限公司-营业执照.pdf', 'demo-hengta-license', 1, 1, 'knowledge/2/demo-hengta-license-v1.pdf', 245760, 'application/pdf', '0000000000000000000000000000000000000000000000000000000000000001', null, 'UPLOADED', 0, 0, null, 205, null, 200, 'zhishi', sysdate(), '', null, 0),
(2, 1, '采购管理制度（2026 版）.docx', 'demo-purchase-policy', 1, 1, 'knowledge/1/demo-purchase-policy-v1.docx', 184320, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '0000000000000000000000000000000000000000000000000000000000000002', null, 'UPLOADED', 0, 0, null, 205, null, 200, 'zhishi', sysdate(), '', null, 0),
(3, 1, '合同付款条款与质保约定.xlsx', 'demo-contract-payment', 1, 1, 'knowledge/1/demo-contract-payment-v1.xlsx', 97280, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '0000000000000000000000000000000000000000000000000000000000000003', null, 'UPLOADED', 0, 0, null, 205, null, 200, 'zhishi', sysdate(), '', null, 0);

insert into kb_object_link(document_id, business_type, business_id, relation_type, create_by, create_time) values
(1, 'SUPPLIER', 1, 'ATTACHMENT', 'zhishi', sysdate()),
(2, 'CONTRACT', 1, 'ATTACHMENT', 'zhishi', sysdate()),
(3, 'CONTRACT', 1, 'ATTACHMENT', 'zhishi', sysdate());

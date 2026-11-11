/**
 * 角色卡视觉资产：卡面纹理、角色语义、线稿徽记。
 * 角色卡与员工工卡同尺寸（85.6 × 53.98 mm），
 * 卡面取自金属卡 / 礼品卡的实拍纹理，叠加以角色为语义的金属色与徽记。
 */

import obsidianPhoto from '@/assets/role/obsidian.jpg'
import goldPhoto from '@/assets/role/gold.jpg'
import carbonPhoto from '@/assets/role/carbon.jpg'
import violetPhoto from '@/assets/role/violet.jpg'
import stealthPhoto from '@/assets/role/stealth.jpg'
import sapphirePhoto from '@/assets/role/sapphire.jpg'
import emeraldPhoto from '@/assets/role/emerald.jpg'
import rosePhoto from '@/assets/role/rose.jpg'
import holoPhoto from '@/assets/role/holo.jpg'
import pearlPhoto from '@/assets/role/pearl.jpg'

export const CARD_THEMES = {
  obsidian: {
    key: 'obsidian',
    photo: obsidianPhoto,
    accent: '#d9bd83',
    tone: 'dark',
    label: 'OBSIDIAN'
  },
  gold: {
    key: 'gold',
    photo: goldPhoto,
    accent: '#f3d18c',
    tone: 'dark',
    label: 'GOLD'
  },
  carbon: {
    key: 'carbon',
    photo: carbonPhoto,
    accent: '#b7c1cf',
    tone: 'dark',
    label: 'CARBON'
  },
  violet: {
    key: 'violet',
    photo: violetPhoto,
    accent: '#c9b4ff',
    tone: 'dark',
    label: 'VIOLET'
  },
  stealth: {
    key: 'stealth',
    photo: stealthPhoto,
    accent: '#d6dee8',
    tone: 'dark',
    label: 'STEALTH'
  },
  sapphire: {
    key: 'sapphire',
    photo: sapphirePhoto,
    accent: '#93c2ff',
    tone: 'dark',
    label: 'SAPPHIRE'
  },
  emerald: {
    key: 'emerald',
    photo: emeraldPhoto,
    accent: '#8fe3b8',
    tone: 'dark',
    label: 'EMERALD'
  },
  rose: {
    key: 'rose',
    photo: rosePhoto,
    accent: '#ffc3ab',
    tone: 'dark',
    label: 'ROSE'
  },
  holo: {
    key: 'holo',
    photo: holoPhoto,
    accent: '#c7e9ff',
    tone: 'dark',
    label: 'HOLO'
  },
  pearl: {
    key: 'pearl',
    photo: pearlPhoto,
    accent: '#8a6a33',
    tone: 'light',
    label: 'PEARL'
  }
}

export const CARD_STYLE_KEYS = Object.keys(CARD_THEMES)

export const ROLE_CATALOG = {
  superadmin: {
    key: 'superadmin',
    en: 'SUPER ADMIN',
    short: 'SAD',
    tagline: '系统最高权限 · 全模块治理',
    emblem: 'crown',
    theme: 'obsidian'
  },
  admin: {
    key: 'admin',
    en: 'ADMINISTRATOR',
    short: 'ADM',
    tagline: '系统配置 · 权限与安全',
    emblem: 'shield',
    theme: 'stealth'
  },
  purchase_manager: {
    key: 'purchase_manager',
    en: 'PROCUREMENT LEAD',
    short: 'PUR',
    tagline: '供应商策略 · 采购治理',
    emblem: 'cart',
    theme: 'rose'
  },
  purchase_staff: {
    key: 'purchase_staff',
    en: 'BUYER',
    short: 'BUY',
    tagline: '询价下单 · 到货跟进',
    emblem: 'cart',
    theme: 'gold'
  },
  dept_manager: {
    key: 'dept_manager',
    en: 'DEPARTMENT LEAD',
    short: 'DPM',
    tagline: '团队管理 · 业务协同',
    emblem: 'people',
    theme: 'sapphire'
  },
  warehouse_keeper: {
    key: 'warehouse_keeper',
    en: 'WAREHOUSE KEEPER',
    short: 'WHS',
    tagline: '出入库 · 盘点与库存',
    emblem: 'box',
    theme: 'carbon'
  },
  finance_auditor: {
    key: 'finance_auditor',
    en: 'FINANCE AUDITOR',
    short: 'FIN',
    tagline: '账务复核 · 风险审计',
    emblem: 'scale',
    theme: 'gold'
  },
  knowledge_admin: {
    key: 'knowledge_admin',
    en: 'KNOWLEDGE ADMIN',
    short: 'KNW',
    tagline: '知识运营 · 内容治理',
    emblem: 'book',
    theme: 'violet'
  },
  common: {
    key: 'common',
    en: 'MEMBER',
    short: 'MBR',
    tagline: '日常协作 · 基础权限',
    emblem: 'user',
    theme: 'pearl'
  },
  finance: {
    key: 'finance',
    en: 'FINANCE',
    short: 'FIN',
    tagline: '预算 · 核算 · 资金',
    emblem: 'scale',
    theme: 'gold'
  },
  hr: {
    key: 'hr',
    en: 'PEOPLE & CULTURE',
    short: 'HRM',
    tagline: '人才与组织发展',
    emblem: 'people',
    theme: 'rose'
  },
  sales: {
    key: 'sales',
    en: 'SALES',
    short: 'SLS',
    tagline: '客户经营 · 收入增长',
    emblem: 'chart',
    theme: 'emerald'
  },
  marketing: {
    key: 'marketing',
    en: 'MARKETING',
    short: 'MKT',
    tagline: '品牌与市场传播',
    emblem: 'megaphone',
    theme: 'holo'
  },
  rd: {
    key: 'rd',
    en: 'ENGINEERING',
    short: 'RND',
    tagline: '研发与工程实现',
    emblem: 'chip',
    theme: 'sapphire'
  },
  qa: {
    key: 'qa',
    en: 'QUALITY',
    short: 'QAS',
    tagline: '测试与质量保障',
    emblem: 'check',
    theme: 'emerald'
  },
  ops: {
    key: 'ops',
    en: 'OPERATIONS',
    short: 'OPS',
    tagline: '基础设施与稳定性',
    emblem: 'server',
    theme: 'carbon'
  },
  legal: {
    key: 'legal',
    en: 'LEGAL',
    short: 'LEG',
    tagline: '合同审查 · 合规风控',
    emblem: 'scale',
    theme: 'stealth'
  },
  service: {
    key: 'service',
    en: 'CUSTOMER CARE',
    short: 'CSR',
    tagline: '客户支持与成功',
    emblem: 'headset',
    theme: 'violet'
  },
  auditor: {
    key: 'auditor',
    en: 'AUDITOR',
    short: 'AUD',
    tagline: '审计核查 · 风险控制',
    emblem: 'search',
    theme: 'gold'
  },
  default: {
    key: 'default',
    en: 'ROLE',
    short: 'ROL',
    tagline: '组织角色 · 职责所在',
    emblem: 'star',
    theme: 'pearl'
  }
}

const KEY_ALIASES = {
  admin: 'superadmin',
  common: 'common',
  purchase_manager: 'purchase_manager',
  purchase_staff: 'purchase_staff',
  dept_manager: 'dept_manager',
  warehouse_keeper: 'warehouse_keeper',
  finance_auditor: 'finance_auditor',
  knowledge_admin: 'knowledge_admin'
}

const RULES = [
  [/超级管理员|超级管理|系统管理员/i, 'superadmin'],
  [/采购负责人|采购主管|采购经理/i, 'purchase_manager'],
  [/采购员|采购专员|采购/i, 'purchase_staff'],
  [/部门负责人|部门主管|部门经理|负责人/i, 'dept_manager'],
  [/仓管|仓库|仓储|库管/i, 'warehouse_keeper'],
  [/财务审计|审计员|审计/i, 'auditor'],
  [/财务|会计|资金|税务/i, 'finance'],
  [/知识管理|知识库|内容管理/i, 'knowledge_admin'],
  [/人事|人力|招聘|组织发展/i, 'hr'],
  [/销售|业务|渠道/i, 'sales'],
  [/市场|品牌|推广|营销/i, 'marketing'],
  [/研发|技术|工程|开发/i, 'rd'],
  [/测试|质量|品控|质控/i, 'qa'],
  [/运维|基础架构|网络/i, 'ops'],
  [/法务|合规|法律/i, 'legal'],
  [/客服|客户服务|售后|客户成功/i, 'service'],
  [/管理员/i, 'admin'],
  [/普通|成员|员工|用户/i, 'common']
]

function hashSeed(text) {
  let hash = 2166136261
  const value = String(text || '')
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

/** 角色 key 优先按 roleKey 精确匹配，其次按中文名语义匹配。 */
export function resolveRoleKey(role) {
  const code = String((role && role.roleKey) || '').toLowerCase()
  if (KEY_ALIASES[code]) return KEY_ALIASES[code]
  if (ROLE_CATALOG[code]) return code
  const text = String((role && role.roleName) || '')
  for (const [pattern, key] of RULES) {
    if (pattern.test(text)) return key
  }
  return ''
}

/** 角色卡编号：JL-ROL-001 */
export function roleCode(role) {
  const id = Number(role && role.roleId)
  const seed = Number.isFinite(id) && id > 0 ? id : hashSeed((role && role.roleKey) || (role && role.roleName)) % 900 + 100
  return 'JL-ROL-' + String(seed).padStart(3, '0')
}

export const DATA_SCOPES = {
  1: '全部数据权限',
  2: '自定义数据权限',
  3: '本部门数据权限',
  4: '本部门及以下',
  5: '仅本人数据'
}

export function dataScopeLabel(value) {
  return DATA_SCOPES[String(value || '')] || '按角色授权'
}

/** 解析一张角色卡的视觉：已知角色用固定语义卡面，未知角色稳定随机。 */
export function roleArtOf(role, index = 0) {
  const key = resolveRoleKey(role)
  const catalog = ROLE_CATALOG[key] || ROLE_CATALOG.default
  const seed = hashSeed(
    String((role && role.roleKey) || '') + '|' + String((role && role.roleName) || '') + '|' + String(index)
  )
  const fallbackKey = CARD_STYLE_KEYS[seed % CARD_STYLE_KEYS.length]
  const themeKey = key ? catalog.theme : fallbackKey
  const theme = CARD_THEMES[themeKey] || CARD_THEMES.obsidian
  return {
    ...catalog,
    ...theme,
    styleKey: theme.key,
    styleNo: CARD_STYLE_KEYS.indexOf(theme.key) + 1,
    styleCount: CARD_STYLE_KEYS.length,
    roleKey: (role && role.roleKey) || catalog.short.toLowerCase(),
    roleId: role && role.roleId
  }
}

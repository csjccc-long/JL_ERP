/**
 * 部门徽章视觉资产：类别、配色、线稿徽记。
 * 所有徽记统一绘制在 64 × 64 的视窗内，使用 currentColor 描边，
 * 便于在深色卡面上以不同金属色呈现。
 */

export const DEPT_CATALOG = {
  group: {
    key: 'group',
    en: 'GROUP',
    short: 'GEN',
    tagline: 'Vision & Governance',
    desc: '集团总部，负责战略、投资与整体治理，为各业务单元提供方向、资源与协同。',
    palette: { a: '#c8ced8', b: '#5f6876' }
  },
  headquarters: {
    key: 'headquarters',
    en: 'HEADQUARTERS',
    short: 'HQ',
    tagline: 'Head Office',
    desc: '区域总部，统筹本地区的经营管理、职能服务与资源调配，是集团战略的落点。',
    palette: { a: '#e3c88e', b: '#7d5f28' }
  },
  branch: {
    key: 'branch',
    en: 'BRANCH',
    short: 'BR',
    tagline: 'Regional Branch',
    desc: '区域分支机构，承接本地市场、客户与运营事务，把总部的服务带到业务一线。',
    palette: { a: '#93b8d6', b: '#395a78' }
  },
  purchase: {
    key: 'purchase',
    en: 'PROCUREMENT',
    short: 'PUR',
    tagline: 'Sourcing & Supply',
    desc: '负责供应商开发、采购计划与成本控制，为组织输送稳定可靠的物资与服务。',
    palette: { a: '#f0b46a', b: '#8f5520' }
  },
  warehouse: {
    key: 'warehouse',
    en: 'WAREHOUSE',
    short: 'WHS',
    tagline: 'Storage & Logistics',
    desc: '负责入库、保管、盘点与配送，让每一件物资都在正确的时间抵达正确的位置。',
    palette: { a: '#8fc3e4', b: '#35627f' }
  },
  finance: {
    key: 'finance',
    en: 'FINANCE',
    short: 'FIN',
    tagline: 'Treasury & Control',
    desc: '负责预算、核算、资金与税务管理，以严谨的数字守护组织的经营底线。',
    palette: { a: '#e8c86e', b: '#967428' }
  },
  production: {
    key: 'production',
    en: 'PRODUCTION',
    short: 'PRD',
    tagline: 'Manufacturing',
    desc: '负责生产计划、工艺与现场管理，把设计图纸转化为可交付的产品。',
    palette: { a: '#e58a6f', b: '#8f3b2a' }
  },
  sales: {
    key: 'sales',
    en: 'SALES',
    short: 'SLS',
    tagline: 'Revenue & Growth',
    desc: '负责客户开发、商机推进与回款，把市场机会转化为持续增长的收入。',
    palette: { a: '#f092a4', b: '#963a52' }
  },
  rd: {
    key: 'rd',
    en: 'RESEARCH & DEVELOPMENT',
    short: 'RND',
    tagline: 'Research & Engineering',
    desc: '负责技术研究、产品研发与工程实现，用代码与实验拓展组织的边界。',
    palette: { a: '#6fdbe8', b: '#186f84' }
  },
  marketing: {
    key: 'marketing',
    en: 'MARKETING',
    short: 'MKT',
    tagline: 'Brand & Market',
    desc: '负责品牌建设、市场洞察与传播，让组织的声音被更多客户听见。',
    palette: { a: '#b9a1f2', b: '#5e44a6' }
  },
  qa: {
    key: 'qa',
    en: 'QUALITY ASSURANCE',
    short: 'QAS',
    tagline: 'Testing & Quality',
    desc: '负责测试验证、质量体系与缺陷管理，在交付之前守住每一道质量关口。',
    palette: { a: '#86d9ad', b: '#2b7a56' }
  },
  ops: {
    key: 'ops',
    en: 'OPERATIONS',
    short: 'OPS',
    tagline: 'Infrastructure & Reliability',
    desc: '负责基础设施、系统运维与稳定性保障，让业务在看不见的地方持续运转。',
    palette: { a: '#9db4d2', b: '#405a79' }
  },
  hr: {
    key: 'hr',
    en: 'PEOPLE',
    short: 'HRM',
    tagline: 'Talent & Culture',
    desc: '负责人才招聘、组织发展与员工关系，让合适的人与合适的位置相遇。',
    palette: { a: '#e9aec7', b: '#91476d' }
  },
  admin: {
    key: 'admin',
    en: 'ADMINISTRATION',
    short: 'ADM',
    tagline: 'Affairs & Services',
    desc: '负责行政事务、后勤保障与办公环境，为组织提供稳定而细致的日常支撑。',
    palette: { a: '#d8c5a4', b: '#7d6847' }
  },
  legal: {
    key: 'legal',
    en: 'LEGAL',
    short: 'LEG',
    tagline: 'Compliance & Counsel',
    desc: '负责合同审查、合规管理与法律风险控制，为经营决策提供专业意见。',
    palette: { a: '#a8b0e8', b: '#4a529a' }
  },
  service: {
    key: 'service',
    en: 'CUSTOMER CARE',
    short: 'CSR',
    tagline: 'Support & Success',
    desc: '负责客户咨询、售后服务与满意度提升，让每一次求助都得到回应。',
    palette: { a: '#8ad6cd', b: '#2c746c' }
  },
  default: {
    key: 'default',
    en: 'DEPARTMENT',
    short: 'GEN',
    tagline: 'Organization',
    desc: '组织中的一枚坐标，承担明确的职责，也连接着更广阔的业务版图。',
    palette: { a: '#c8ced8', b: '#5f6876' }
  }
}

const RULES = [
  [/集团|控股|金流科技/i, 'group'],
  [/总公司|总部/i, 'headquarters'],
  [/分公司|办事处/i, 'branch'],
  [/采购|供应|招标/i, 'purchase'],
  [/仓储|仓库|物流|储运/i, 'warehouse'],
  [/财务|会计|资金|税务/i, 'finance'],
  [/生产|制造|车间|工厂/i, 'production'],
  [/销售|业务|渠道/i, 'sales'],
  [/研发|技术|产品|开发|研究/i, 'rd'],
  [/市场|品牌|推广|营销/i, 'marketing'],
  [/测试|质量|品控|质控/i, 'qa'],
  [/运维|信息技术|网络|基础架构|it/i, 'ops'],
  [/人事|人力|招聘|组织发展/i, 'hr'],
  [/行政|综合|办公|后勤/i, 'admin'],
  [/法务|合规|法律/i, 'legal'],
  [/客服|客户服务|售后/i, 'service']
]

/** 根据部门中文名解析类别 key，未知部门返回空字符串。 */
export function resolveKey(name) {
  const text = String(name || '')
  for (const [pattern, key] of RULES) {
    if (pattern.test(text)) return key
  }
  return ''
}

/** 根据部门中文名解析类别，未知部门落到 default。 */
export function resolveDept(name) {
  return DEPT_CATALOG[resolveKey(name) || 'default']
}

/** 给徽章编号：JL-PUR-200 */
export function badgeCode(dept) {
  const identity = resolveDept(dept && dept.deptName)
  const id = String((dept && dept.deptId) || 0).padStart(3, '0')
  return 'JL-' + identity.short + '-' + id
}

/* ------------------------------------------------------------------ */
/* 线稿徽记                                                             */
/* ------------------------------------------------------------------ */

const path = (d, extra = {}) => ({ t: 'path', d, ...extra })
const line = (x1, y1, x2, y2, extra = {}) => ({ t: 'line', x1, y1, x2, y2, ...extra })
const circle = (cx, cy, r, extra = {}) => ({ t: 'circle', cx, cy, r, ...extra })
const rect = (x, y, width, height, rx = 0, extra = {}) => ({ t: 'rect', x, y, width, height, rx, ...extra })
const ellipse = (cx, cy, rx, ry, extra = {}) => ({ t: 'ellipse', cx, cy, rx, ry, ...extra })

function gearPath(cx, cy, outer, inner, teeth) {
  const points = []
  for (let i = 0; i < teeth * 2; i += 1) {
    const radius = i % 2 === 0 ? outer : inner
    const angle = (i * Math.PI) / teeth - Math.PI / 2
    points.push(
      (cx + Math.cos(angle) * radius).toFixed(2) + ' ' + (cy + Math.sin(angle) * radius).toFixed(2)
    )
  }
  return 'M' + points.join('L') + 'Z'
}

export const EMBLEMS = {
  purchase: {
    paths: [
      path('M11 15.5h4.6l4.9 23.2a3.2 3.2 0 0 0 3.13 2.55h20.1a3.2 3.2 0 0 0 3.13-2.53L50.4 22H20.2'),
      path('M25 38.5h22'),
      path('M28.5 27.5l3 3.1 6-6.4')
    ],
    circles: [circle(26.5, 49, 3.3), circle(45.5, 49, 3.3), circle(26.5, 49, 1.1, { fill: true }), circle(45.5, 49, 1.1, { fill: true })]
  },
  warehouse: {
    paths: [
      path('M8 28.5 32 11l24 17.5'),
      path('M14 28.5V53 M50 28.5V53'),
      path('M8.5 53h47'),
      path('M21 40.5h9V53h-9z'),
      path('M34 40.5h9V53h-9z'),
      path('M26 29.5h12v11H26z')
    ]
  },
  finance: {
    paths: [
      path('M24.5 20.5 32 30l7.5-9.5'),
      path('M32 30v17'),
      path('M24 34.5h16'),
      path('M24 40.5h16')
    ],
    circles: [circle(32, 32, 21), circle(32, 32, 17, { dash: '1.6 3.2', opacity: 0.55 })]
  },
  production: {
    paths: [
      path('M8 52.5V31.5l12 6.4v-6.4l12 6.4v-6.4l12 6.4v15.2'),
      path('M18 31.5V18h7v8.8'),
      path('M7 52.5h50'),
      path('M25 44.5h8v8h-8z'),
      path(gearPath(47.5, 19, 9.6, 7.9, 10)),
      path('M6.5 44.5h6')
    ],
    circles: [circle(47.5, 19, 3.6), circle(47.5, 19, 1.2, { fill: true })]
  },
  sales: {
    paths: [
      path('M12 48V36.5'),
      path('M23 48V29.5'),
      path('M34 48V22'),
      path('M8 54h48'),
      path('M9.5 30.5 22 21l9.5 5 18-16'),
      path('M42.5 10H52v9.5')
    ],
    circles: [circle(22, 21, 1.6, { fill: true }), circle(31.5, 26, 1.6, { fill: true })]
  },
  rd: {
    paths: [
      path('M28.5 30.5 25 34l3.5 3.5'),
      path('M35.5 30.5 39 34l-3.5 3.5')
    ],
    rects: [rect(19, 19, 26, 26, 4), rect(27, 27, 10, 10, 2)],
    lines: [
      line(24, 11.5, 24, 19), line(32, 11.5, 32, 19), line(40, 11.5, 40, 19),
      line(24, 45, 24, 52.5), line(32, 45, 32, 52.5), line(40, 45, 40, 52.5),
      line(11.5, 24, 19, 24), line(11.5, 32, 19, 32), line(11.5, 40, 19, 40),
      line(45, 24, 52.5, 24), line(45, 32, 52.5, 32), line(45, 40, 52.5, 40)
    ]
  },
  marketing: {
    paths: [
      path('M13 32h38'),
      path('M16.5 22.5h31'),
      path('M16.5 41.5h31')
    ],
    circles: [circle(32, 32, 19)],
    ellipses: [
      ellipse(32, 32, 8, 19),
      ellipse(32, 32, 27, 10, { transform: 'rotate(-22 32 32)', dash: '2.4 3.2', opacity: 0.6 })
    ]
  },
  qa: {
    paths: [
      path('M32 9.5 51 16.5v14.2c0 11.3-7.9 18.1-19 23.8-11.1-5.7-19-12.5-19-23.8V16.5z'),
      path('M23 32l6 6 12-13')
    ]
  },
  ops: {
    paths: [
      path('M24 27v6 M40 27v6'),
      path('M8.5 54h47')
    ],
    rects: [rect(14, 12, 36, 15, 3), rect(14, 33, 36, 15, 3)],
    lines: [line(20, 19.5, 28, 19.5), line(20, 40.5, 28, 40.5)],
    circles: [circle(42, 19.5, 1.7, { fill: true }), circle(42, 40.5, 1.7, { fill: true })]
  },
  hr: {
    paths: [
      path('M20 48.5c0-7 5.4-12 12-12s12 5 12 12'),
      path('M9 44.5c0-5 3.6-9 8-9 1.5 0 2.9.4 4.1 1.2'),
      path('M55 44.5c0-5-3.6-9-8-9-1.5 0-2.9.4-4.1 1.2')
    ],
    circles: [circle(32, 21, 6.2), circle(17, 27.5, 4), circle(47, 27.5, 4)]
  },
  admin: {
    paths: [
      path('M8 25 32 10l24 15'),
      path('M8 25h48'),
      path('M8 54h48'),
      path('M18 31.5V48 M28 31.5V48 M38 31.5V48 M48 31.5V48'),
      path('M13 48h38')
    ]
  },
  legal: {
    paths: [
      path('M32 12v41'),
      path('M14 21.5h36'),
      path('M14 21.5 7 34h14z'),
      path('M50 21.5 43 34h14z'),
      path('M7 34a7 7 0 0 0 14 0'),
      path('M43 34a7 7 0 0 0 14 0'),
      path('M24 53h16')
    ]
  },
  service: {
    paths: [
      path('M13 36.5v-3.8a19 19 0 0 1 38 0v3.8'),
      path('M51 48.5v2.6a5.8 5.8 0 0 1-5.8 5.8H37'),
      path('M13 40.5v-3')
    ],
    rects: [rect(9, 33, 8, 15, 4), rect(47, 34, 8, 15, 4)],
    circles: [circle(34, 56.5, 2.1, { fill: true })]
  },
  default: {
    paths: [
      path('M32 13.5 36.5 27.5 50.5 32 36.5 36.5 32 50.5 27.5 36.5 13.5 32 27.5 27.5z'),
      path('M32 23.5v17 M23.5 32h17')
    ],
    circles: [circle(32, 32, 21)]
  }
}

/** 返回某个部门类别对应的徽记线稿。 */
export function emblemOf(key) {
  return EMBLEMS[key] || EMBLEMS.default
}

/**
 * 部门通行证的固定样式。
 *
 * 每款共用同一张卡形（85.6 × 53.98mm 横版，与员工工卡同规格），
 * 背景换成该部门真实场景的照片，中央保留一枚部门徽记。
 * 数据库里新增的、匹配不到类别的部门会用 deptId + deptName 做稳定哈希，
 * 从这组固定样式里取一枚——刷新不会变。
 */
import groupPhoto from '@/assets/dept/group.jpg'
import headquartersPhoto from '@/assets/dept/headquarters.jpg'
import branchPhoto from '@/assets/dept/branch.jpg'
import purchasePhoto from '@/assets/dept/purchase.jpg'
import warehousePhoto from '@/assets/dept/warehouse.jpg'
import financePhoto from '@/assets/dept/finance.jpg'
import productionPhoto from '@/assets/dept/production.jpg'
import salesPhoto from '@/assets/dept/sales.jpg'
import rdPhoto from '@/assets/dept/rd.jpg'
import marketingPhoto from '@/assets/dept/marketing.jpg'
import qaPhoto from '@/assets/dept/qa.jpg'
import opsPhoto from '@/assets/dept/ops.jpg'
import hrPhoto from '@/assets/dept/hr.jpg'
import adminPhoto from '@/assets/dept/admin.jpg'
import legalPhoto from '@/assets/dept/legal.jpg'
import servicePhoto from '@/assets/dept/service.jpg'

export const PASS_THEMES = {
  group: {
    key: 'group',
    photo: groupPhoto,
    no: '01',
    accent: '#d8c39a'
  },
  headquarters: {
    key: 'headquarters',
    photo: headquartersPhoto,
    no: '02',
    accent: '#d9b878'
  },
  branch: {
    key: 'branch',
    photo: branchPhoto,
    no: '03',
    accent: '#9dc0da'
  },
  purchase: {
    key: 'purchase',
    photo: purchasePhoto,
    no: '04',
    accent: '#e0a95b'
  },
  warehouse: {
    key: 'warehouse',
    photo: warehousePhoto,
    no: '05',
    accent: '#8fb6d2'
  },
  finance: {
    key: 'finance',
    photo: financePhoto,
    no: '06',
    accent: '#d9bc6a'
  },
  production: {
    key: 'production',
    photo: productionPhoto,
    no: '07',
    accent: '#d68d6d'
  },
  sales: {
    key: 'sales',
    photo: salesPhoto,
    no: '08',
    accent: '#d98fa3'
  },
  rd: {
    key: 'rd',
    photo: rdPhoto,
    no: '09',
    accent: '#6fc2c8'
  },
  marketing: {
    key: 'marketing',
    photo: marketingPhoto,
    no: '10',
    accent: '#a995d8'
  },
  qa: {
    key: 'qa',
    photo: qaPhoto,
    no: '11',
    accent: '#86bf98'
  },
  ops: {
    key: 'ops',
    photo: opsPhoto,
    no: '12',
    accent: '#8ea6c4'
  },
  hr: {
    key: 'hr',
    photo: hrPhoto,
    no: '13',
    accent: '#d19ab3'
  },
  admin: {
    key: 'admin',
    photo: adminPhoto,
    no: '14',
    accent: '#c8b28d'
  },
  legal: {
    key: 'legal',
    photo: legalPhoto,
    no: '15',
    accent: '#a3a9d6'
  },
  service: {
    key: 'service',
    photo: servicePhoto,
    no: '16',
    accent: '#8cc8be'
  }
}

export const PASS_STYLE_KEYS = Object.keys(PASS_THEMES)

/** 稳定哈希：同样的部门每次刷新都拿到同一款样式。 */
export function hashSeed(text) {
  let hash = 2166136261
  const source = String(text || '')
  for (let i = 0; i < source.length; i += 1) {
    hash ^= source.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

/**
 * 解析一个部门的卡片样式。
 * 已知部门按语义固定；未知部门从全部样式里稳定随机取一枚。
 */
export function passStyleOf(dept, artKey) {
  const fallbackKey = PASS_STYLE_KEYS[hashSeed(String(dept && dept.deptId) + '|' + String(dept && dept.deptName)) % PASS_STYLE_KEYS.length]
  const key = artKey && PASS_THEMES[artKey] ? artKey : fallbackKey
  const theme = PASS_THEMES[key]
  return {
    ...theme,
    styleKey: key,
    artKey: artKey || 'default',
    styleCount: PASS_STYLE_KEYS.length
  }
}

import request from '@/utils/request'

// 查询问答审计列表
export function listAudit(query) {
  return request({ url: '/knowledge/audit/queries', method: 'get', params: query })
}

// 查询审计详情
export function getAudit(traceId) {
  return request({ url: '/knowledge/audit/queries/' + traceId, method: 'get' })
}

// 审计概览
export function auditOverview() {
  return request({ url: '/knowledge/audit/overview', method: 'get' })
}

import request from '@/utils/request'

// 驾驶舱概览
export function getOverview() {
  return request({ url: '/erp/dashboard/overview', method: 'get' })
}

// 待办任务
export function getPending() {
  return request({ url: '/erp/dashboard/pending', method: 'get' })
}

// 低库存清单
export function getLowStock() {
  return request({ url: '/erp/dashboard/low-stock', method: 'get' })
}

// 入库趋势
export function getReceiptTrend(days) {
  return request({ url: '/erp/dashboard/receipt-trend', method: 'get', params: { days: days } })
}

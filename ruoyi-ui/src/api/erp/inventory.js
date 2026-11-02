import request from '@/utils/request'

// 查询库存余额列表
export function listBalance(query) {
  return request({
    url: '/erp/inventory/list',
    method: 'get',
    params: query
  })
}

// 查询库存流水列表
export function listLedger(query) {
  return request({
    url: '/erp/inventory/ledger/list',
    method: 'get',
    params: query
  })
}

import request from '@/utils/request'

// 查询入库单列表
export function listReceipt(query) {
  return request({
    url: '/erp/receipt/list',
    method: 'get',
    params: query
  })
}

// 查询入库单详细
export function getReceipt(id) {
  return request({
    url: '/erp/receipt/' + id,
    method: 'get'
  })
}

// 新增入库单
export function addReceipt(data) {
  return request({
    url: '/erp/receipt',
    method: 'post',
    data: data
  })
}

// 修改入库单
export function updateReceipt(data) {
  return request({
    url: '/erp/receipt',
    method: 'put',
    data: data
  })
}

// 删除入库单
export function delReceipt(ids) {
  return request({
    url: '/erp/receipt/' + ids,
    method: 'delete'
  })
}

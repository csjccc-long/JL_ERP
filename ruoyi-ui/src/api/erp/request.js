import request from '@/utils/request'

// 查询采购申请列表
export function listRequest(query) {
  return request({
    url: '/erp/request/list',
    method: 'get',
    params: query
  })
}

// 查询采购申请详细
export function getRequest(id) {
  return request({
    url: '/erp/request/' + id,
    method: 'get'
  })
}

// 新增采购申请
export function addRequest(data) {
  return request({
    url: '/erp/request',
    method: 'post',
    data: data
  })
}

// 修改采购申请
export function updateRequest(data) {
  return request({
    url: '/erp/request',
    method: 'put',
    data: data
  })
}

// 删除采购申请
export function delRequest(ids) {
  return request({
    url: '/erp/request/' + ids,
    method: 'delete'
  })
}

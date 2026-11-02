import request from '@/utils/request'

// 查询仓库列表
export function listWarehouse(query) {
  return request({
    url: '/erp/warehouse/list',
    method: 'get',
    params: query
  })
}

// 查询仓库详细
export function getWarehouse(id) {
  return request({
    url: '/erp/warehouse/' + id,
    method: 'get'
  })
}

// 新增仓库
export function addWarehouse(data) {
  return request({
    url: '/erp/warehouse',
    method: 'post',
    data: data
  })
}

// 修改仓库
export function updateWarehouse(data) {
  return request({
    url: '/erp/warehouse',
    method: 'put',
    data: data
  })
}

// 删除仓库
export function delWarehouse(ids) {
  return request({
    url: '/erp/warehouse/' + ids,
    method: 'delete'
  })
}

import request from '@/utils/request'

// 查询物料列表
export function listMaterial(query) {
  return request({
    url: '/erp/material/list',
    method: 'get',
    params: query
  })
}

// 查询物料详细
export function getMaterial(id) {
  return request({
    url: '/erp/material/' + id,
    method: 'get'
  })
}

// 新增物料
export function addMaterial(data) {
  return request({
    url: '/erp/material',
    method: 'post',
    data: data
  })
}

// 修改物料
export function updateMaterial(data) {
  return request({
    url: '/erp/material',
    method: 'put',
    data: data
  })
}

// 删除物料
export function delMaterial(ids) {
  return request({
    url: '/erp/material/' + ids,
    method: 'delete'
  })
}

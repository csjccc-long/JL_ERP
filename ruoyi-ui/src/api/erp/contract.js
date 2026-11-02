import request from '@/utils/request'

// 查询合同列表
export function listContract(query) {
  return request({
    url: '/erp/contract/list',
    method: 'get',
    params: query
  })
}

// 查询合同详细
export function getContract(id) {
  return request({
    url: '/erp/contract/' + id,
    method: 'get'
  })
}

// 新增合同
export function addContract(data) {
  return request({
    url: '/erp/contract',
    method: 'post',
    data: data
  })
}

// 修改合同
export function updateContract(data) {
  return request({
    url: '/erp/contract',
    method: 'put',
    data: data
  })
}

// 删除合同
export function delContract(ids) {
  return request({
    url: '/erp/contract/' + ids,
    method: 'delete'
  })
}

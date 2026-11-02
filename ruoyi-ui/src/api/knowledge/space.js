import request from '@/utils/request'

// 查询知识空间列表
export function listSpace(query) {
  return request({ url: '/knowledge/space/list', method: 'get', params: query })
}

// 可访问空间下拉
export function spaceOptions() {
  return request({ url: '/knowledge/space/options', method: 'get' })
}

// 查询知识空间详细
export function getSpace(id) {
  return request({ url: '/knowledge/space/' + id, method: 'get' })
}

// 新增知识空间
export function addSpace(data) {
  return request({ url: '/knowledge/space', method: 'post', data: data })
}

// 修改知识空间
export function updateSpace(data) {
  return request({ url: '/knowledge/space', method: 'put', data: data })
}

// 停用知识空间
export function delSpace(id) {
  return request({ url: '/knowledge/space/' + id, method: 'delete' })
}

// 空间授权列表
export function listAcl(spaceId) {
  return request({ url: '/knowledge/space/acl/' + spaceId, method: 'get' })
}

// 新增空间授权
export function addAcl(spaceId, data) {
  return request({ url: '/knowledge/space/acl/' + spaceId, method: 'post', data: data })
}

// 删除空间授权
export function delAcl(spaceId, aclId) {
  return request({ url: '/knowledge/space/acl/' + spaceId + '/' + aclId, method: 'delete' })
}

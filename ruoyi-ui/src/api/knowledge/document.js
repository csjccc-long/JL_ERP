import request from '@/utils/request'

// 查询文档列表
export function listDocument(query) {
  return request({ url: '/knowledge/document/list', method: 'get', params: query })
}

// 查询文档详细
export function getDocument(id) {
  return request({ url: '/knowledge/document/' + id, method: 'get' })
}

// 业务对象关联文档
export function listByBusiness(businessType, businessId) {
  return request({ url: '/knowledge/document/by-business', method: 'get', params: { businessType, businessId } })
}

// 上传文档：这个弹窗有自己的波浪进度层（选完文件即显形），
// 所以这里关掉右下角的全局浮层，避免同一时刻出现两层波浪。
export function uploadDocument(formData, onUploadProgress) {
  return request({
    url: '/knowledge/document/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData,
    timeout: 180000,
    uploadProgress: false,
    onUploadProgress
  })
}

// 重新索引
export function retryDocument(id) {
  return request({ url: '/knowledge/document/retry/' + id, method: 'post' })
}

// 停用文档
export function disableDocument(id) {
  return request({ url: '/knowledge/document/' + id, method: 'delete' })
}

// 启用（恢复）已停用的文档
export function enableDocument(id) {
  return request({
    url: '/knowledge/document/' + id + '/enable',
    method: 'post',
    timeout: 180000
  })
}

// 文档业务关联
export function listLinks(id) {
  return request({ url: '/knowledge/document/' + id + '/links', method: 'get' })
}

// 新增业务关联
export function addLink(id, data) {
  return request({ url: '/knowledge/document/' + id + '/link', method: 'post', data: data })
}

// 删除业务关联
export function delLink(id, linkId) {
  return request({ url: '/knowledge/document/' + id + '/link/' + linkId, method: 'delete' })
}

// 下载文档
export function downloadDocument(id) {
  return request({ url: '/knowledge/document/download/' + id, method: 'get', responseType: 'blob' })
}

// 预览文档
export function previewDocument(id) {
  return request({ url: '/knowledge/document/preview/' + id, method: 'get', responseType: 'blob' })
}

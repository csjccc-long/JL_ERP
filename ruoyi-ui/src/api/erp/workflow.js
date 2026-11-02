import request from '@/utils/request'

// 合同提交审批
export function submitContract(id) {
  return request({ url: '/erp/workflow/contract/submit/' + id, method: 'post' })
}

// 合同审批
export function approveContract(id, pass, opinion) {
  return request({ url: '/erp/workflow/contract/approve/' + id, method: 'post', params: { pass, opinion } })
}

// 合同终止
export function terminateContract(id, opinion) {
  return request({ url: '/erp/workflow/contract/terminate/' + id, method: 'post', params: { opinion } })
}

// 采购申请提交
export function submitRequest(id) {
  return request({ url: '/erp/workflow/request/submit/' + id, method: 'post' })
}

// 采购申请审批
export function approveRequest(id, pass, opinion) {
  return request({ url: '/erp/workflow/request/approve/' + id, method: 'post', params: { pass, opinion } })
}

// 采购订单提交
export function submitOrder(id) {
  return request({ url: '/erp/workflow/order/submit/' + id, method: 'post' })
}

// 采购订单审批
export function approveOrder(id, pass, opinion) {
  return request({ url: '/erp/workflow/order/approve/' + id, method: 'post', params: { pass, opinion } })
}

// 采购订单取消
export function cancelOrder(id, opinion) {
  return request({ url: '/erp/workflow/order/cancel/' + id, method: 'post', params: { opinion } })
}

// 入库确认
export function confirmReceipt(id) {
  return request({ url: '/erp/workflow/receipt/confirm/' + id, method: 'post' })
}

// 入库冲销
export function reverseReceipt(id, opinion) {
  return request({ url: '/erp/workflow/receipt/reverse/' + id, method: 'post', params: { opinion } })
}

// 审批记录
export function listApproval(businessType, businessId) {
  return request({
    url: '/erp/workflow/approval/list',
    method: 'get',
    params: { businessType: businessType, businessId: businessId }
  })
}

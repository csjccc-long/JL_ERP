import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 会话列表
export function listSessions() {
  return request({ url: '/assistant/sessions', method: 'get' })
}

// 新建会话
export function addSession(data) {
  return request({ url: '/assistant/sessions', method: 'post', data: data || {} })
}

// 删除会话
export function delSession(id) {
  return request({ url: '/assistant/sessions/' + id, method: 'delete' })
}

// 会话消息
export function listMessages(id) {
  return request({ url: '/assistant/sessions/' + id + '/messages', method: 'get' })
}

// 助手健康状态
export function assistantHealth() {
  return request({ url: '/assistant/health', method: 'get' })
}

/**
 * 流式问答（SSE）
 * @param {Object} payload { question, sessionId, contextType, contextId, spaceId }
 * @param {Object} handlers { onMeta, onDelta, onCitation, onDone, onError }
 * @returns {Function} abort 取消函数
 */
export function askStream(payload, handlers = {}) {
  const controller = new AbortController()
  const base = import.meta.env.VITE_APP_BASE_API || ''
  fetch(base + '/assistant/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=utf-8',
      Authorization: 'Bearer ' + getToken()
    },
    body: JSON.stringify(payload),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok || !response.body) {
      handlers.onError && handlers.onError({ code: 'HTTP_' + response.status, message: '问答服务调用失败，请稍后重试' })
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let eventName = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let index
      while ((index = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, index).replace(/\r$/, '')
        buffer = buffer.slice(index + 1)
        if (line === '') {
          eventName = ''
          continue
        }
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          let data = raw
          try {
            data = JSON.parse(raw)
          } catch (e) {
            data = raw
          }
          dispatch(eventName, data)
        }
      }
    }
  }).catch(error => {
    if (error && error.name === 'AbortError') return
    handlers.onError && handlers.onError({ code: 'NETWORK_ERROR', message: '网络异常，请检查连接后重试' })
  })

  function dispatch(event, data) {
    if (event === 'meta' && handlers.onMeta) handlers.onMeta(data)
    else if (event === 'delta' && handlers.onDelta) handlers.onDelta(data)
    else if (event === 'citation' && handlers.onCitation) handlers.onCitation(data)
    else if (event === 'done' && handlers.onDone) handlers.onDone(data)
    else if (event === 'error' && handlers.onError) handlers.onError(data)
  }

  return () => controller.abort()
}

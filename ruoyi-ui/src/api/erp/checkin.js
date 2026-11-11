import request from '@/utils/request'

// 打卡看板：最近一年每日热度、今日状态与连续天数
export function getCheckinBoard() {
  return request({
    url: '/erp/checkin/board',
    method: 'get'
  })
}

// 某一天的待办、自动化与审批记录（date 形如 2026-11-05，不传取今天）
export function getCheckinDay(date) {
  return request({
    url: '/erp/checkin/day',
    method: 'get',
    params: { date }
  })
}

// 今日打卡
export function doCheckin() {
  return request({
    url: '/erp/checkin',
    method: 'post'
  })
}

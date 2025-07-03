// 一小时的毫秒数
export const ONE_HOUR_STAMP = 60 * 60 * 1000
// 时间分辨率，即整个时间轴表示的时间范围
export const ZOOM = [0.5, 1, 2, 6, 12, 24, 72, 360, 720, 8760, 87600]// 半小时、1小时、2小时、6小时、12小时、1天、3天、15天、30天、365天、365*10天
// 时间分辨率对应的每格小时数，即最小格代表多少小时
export const ZOOM_HOUR_GRID = [1 / 60, 1 / 60, 2 / 60, 1 / 6, 0.25, 0.5, 1, 4, 4, 720, 7200]
export const MOBILE_ZOOM_HOUR_GRID = [
  1 / 20,
  1 / 30,
  1 / 20,
  1 / 3,
  0.5,
  2,
  4,
  4,
  4,
  720, 7200
]
// 时间分辨率对应的时间显示判断条件
export const ZOOM_DATE_SHOW_RULE = [
  () => { // 全部显示
    return true
  },
  date => { // 每五分钟显示
    return date.getMinutes() % 5 === 0
  },
  date => { // 每十分钟显示
    return date.getMinutes() % 10 === 0
  },
  date => { // 整点和半点显示
    return date.getMinutes() === 0 || date.getMinutes() === 30
  },
  date => { // 整点显示
    return date.getMinutes() === 0
  },
  date => { // 偶数整点的小时
    return date.getHours() % 2 === 0 && date.getMinutes() === 0
  },
  date => { // 每三小时小时
    return date.getHours() % 3 === 0 && date.getMinutes() === 0
  },
  date => { // 每12小时
    return date.getHours() % 12 === 0 && date.getMinutes() === 0
  },
  date => { // 全不显示
    return false
  },
  date => {
    return true
  },
  date => {
    return true
  }
]
export const MOBILE_ZOOM_DATE_SHOW_RULE = [
  () => { // 全部显示
    return true
  },
  date => { // 每五分钟显示
    return date.getMinutes() % 5 === 0
  },
  date => { // 每十分钟显示
    return date.getMinutes() % 10 === 0
  },
  date => { // 整点和半点显示
    return date.getMinutes() === 0 || date.getMinutes() === 30
  },
  date => { // 偶数整点的小时
    return date.getHours() % 2 === 0 && date.getMinutes() === 0
  },
  date => { // 偶数整点的小时
    return date.getHours() % 4 === 0 && date.getMinutes() === 0
  },
  date => { // 每三小时小时
    return date.getHours() % 3 === 0 && date.getMinutes() === 0
  },
  date => { // 每12小时
    return date.getHours() % 12 === 0 && date.getMinutes() === 0
  },
  date => { // 全不显示
    return false
  },
  date => {
    return true
  },
  date => {
    return true
  }
]

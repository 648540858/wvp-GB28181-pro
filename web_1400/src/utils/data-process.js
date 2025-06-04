// 回显数据字典
export function selectDictLabel(datas, value) {
  var actions = []
  Object.keys(datas).map((key) => {
    if (datas[key].itemText === ('' + value)) {
      actions.push(datas[key].itemValue)
      return false
    }
  })
  return actions.join('')
}

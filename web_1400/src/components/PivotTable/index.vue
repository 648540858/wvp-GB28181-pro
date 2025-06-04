<template>
  <div class="table-responsive">
    <table>
      <!-- Table header -->
      <thead>
        <tr v-for="(tr, index) in combineHeads" :key="index">
          <th v-for="cell in tr" :key="cell.__index" :rowspan="cell.rowspan" :colspan="cell.colspan">
            <div :class="{ 'col-corner-bg': cell.isCorner }" :style="{ 'min-height': _getMinHeightByRowCount(cell.rowspan) }">
              {{ cell.isCorner ? (rowPaths.length + ' x ' + colPaths.length) : cell.value }}
            </div>
          </th>
        </tr>
      </thead>
      <!-- Table body -->
      <tbody>
        <tr v-for="(tr, index) in combineValues" :key="index">
          <!-- Row headers -->
          <th v-for="cell in tr.head" v-if="!cell.isRowspan" :key="cell.__index" :rowspan="cell.rowspan" :colspan="cell.colspan">
            <div :style="{ 'min-height': _getMinHeightByRowCount(cell.rowspan) }">
              {{ cell.value }}
            </div>
          </th>
          <!-- Values -->
          <td v-for="cell in tr.data" :key="cell.__index" :rowspan="cell.rowspan" :colspan="cell.colspan">
            <div :style="{ 'min-height': _getMinHeightByRowCount(cell.rowspan) }">
              {{ cell.value }}
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { mergeBaseInfo, convertPathToMap, getHeightByCount, SEPARATOR } from '@/utils/visual-chart'

export default {
  name: 'PivotTable',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    rows: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    values: {
      type: Array,
      default: () => []
    }
  },
  data: () => ({
    localRows: [],
    localColumns: [],
    localValues: [],
    localData: [],
    // 计算列的数据
    calcData: [],
    // Separator
    Separator: SEPARATOR,
    // 合并后的表头
    combineHeads: [],
    // 合并后的单元格
    combineValues: []
  }),
  computed: {
    watchAllProps() {
      const { rows, columns, values, data } = this
      return { rows, columns, values, data }
    },
    rowPaths() {
      const _paths = this._combineRowPaths(
        this.localData,
        ...this.localRows.map(({ key, values }) => { return { key, values } })
      )
      return _paths
    },
    colPaths() {
      const keys = this.localColumns.map(({ values }) => values)
      if (this.localValues.length) {
        keys.push(this.localValues.map(({ key }) => key))
      }
      const _paths = this._combineColPaths(...keys)
      return _paths
    },
    // 列的表头
    colHeads() {
      // 共有多少行
      const _rows = this.localColumns.map(() => [])
      // 有几个值
      const valuesLen = this.localValues.length
      if (valuesLen) {
        _rows.push([])
      }
      // 计算合并单元格
      const colSpans = {}
      this.colPaths.forEach((path, pathIndex) => {
        // 条件值
        const pathValues = path.split(this.Separator)
        // 存储路径
        const currPath = []
        _rows.forEach((row, rowIndex) => {
          const cellData = {}
          const currVal = pathValues[rowIndex] || ''
          // 是否为 values 行
          const isLastRow = rowIndex === _rows.length - 1
          // 存储路径
          currPath.push(currVal)
          const baseX = rowIndex
          const baseY = this.localRows.length + pathIndex
          if (!isLastRow) {
            // 计算合并行数
            let compareVal = valuesLen
            for (let i = rowIndex; i < this.localColumns.length - 1; i++) {
              compareVal *= this.localColumns[rowIndex + 1].values.length
            }
            const currColSpan = colSpans[rowIndex] || {}
            const currColSpanVal = (currColSpan[currPath.join(this.Separator)] || 0) + 1
            currColSpan[currPath.join(this.Separator)] = currColSpanVal
            colSpans[rowIndex] = currColSpan
            // 合并单元格：起始单元格加入
            if (currColSpanVal === 1) {
              row.push(
                Object.assign(
                  cellData,
                  mergeBaseInfo({
                    __index: `${baseX}-${baseY}`,
                    x: baseX,
                    y: baseY,
                    colspan: compareVal,
                    path: currPath.filter((item) => !!item),
                    value: currVal
                  })
                )
              )
            }
          } else {
            row.push(
              Object.assign(
                cellData,
                mergeBaseInfo({
                  __index: `${baseX}-${baseY}`,
                  x: baseX,
                  y: baseY,
                  path: currPath.filter((item) => !!item),
                  // 最后一行是 values，替换显示文本
                  value: this.localValues.find(({ key }) => key === currVal).label
                })
              )
            )
          }
        })
      })
      return _rows
    },
    // 行的表头
    rowHeads() {
      // 共有多少列
      const _columns = []
      // 左上角特殊处理, 有行维、列维时才有
      const columnsLen = this.localColumns.length
      const rowsLen = this.localRows.length
      if (rowsLen && columnsLen) {
        _columns.push(mergeBaseInfo({
          __index: `0-0`,
          colspan: this.localRows.length,
          rowspan: this.localColumns.length,
          // 左上角标记
          isCorner: true
        }))
      }
      this.localRows.forEach(({ label }, index) => {
        _columns.push(mergeBaseInfo({
          __index: `${this.localColumns.length}-${index}`,
          value: label,
          x: this.localColumns.length,
          y: index
        }))
      })
      return _columns
    },
    // 行对应的值
    rowHeadValues() {
      let _values = []
      const rowsLen = this.localRows.length
      if (rowsLen) {
        // 计算合并单元格
        const rowSpans = {}
        _values = this.rowPaths.map((path, pathIndex) => {
          const values = path.split(this.Separator)
          const currPath = []
          return this.localRows.map((item, rowIndex) => {
            const currVal = values[rowIndex] || ''
            const baseX = this.localColumns.length + +Boolean(this.localValues.length) + pathIndex
            const baseY = rowIndex
            currPath.push(currVal)
            // 是否为最后列
            const isLastCol = rowIndex === rowsLen - 1
            if (!isLastCol) {
              // 计算合并列数
              // 过滤条件
              const conditions = {}
              for (let i = 0; i < rowIndex + 1; i++) {
                conditions[i] = values[i] || ''
              }
              const filterData = this.rowPaths.filter((data) => {
                let status = true
                const splitValues = data.split(this.Separator)
                for (const key in conditions) {
                  if (conditions[key] !== splitValues[key]) {
                    status = false
                    return
                  }
                }
                return status
              }) || []
              const mergeSpans = filterData.length
              const currRowSpan = rowSpans[rowIndex] || {}
              const currRowSpanVal = (currRowSpan[currPath.join(this.Separator)] || 0) + 1
              currRowSpan[currPath.join(this.Separator)] = currRowSpanVal
              rowSpans[rowIndex] = currRowSpan
              if (currRowSpanVal === 1) {
                return mergeBaseInfo({
                  __index: `${baseX}-${baseY}`,
                  value: currVal,
                  x: baseX,
                  y: baseY,
                  rowspan: mergeSpans,
                  path: currPath.filter((item) => !!item)
                })
              } else {
                return mergeBaseInfo({
                  __index: `${baseX}-${baseY}`,
                  value: currVal,
                  x: baseX,
                  y: baseY,
                  path: currPath.filter((item) => !!item),
                  // 是否合并单元格，遍历时判断不显示
                  isRowspan: true
                })
              }
            } else {
              return mergeBaseInfo({
                __index: `${baseX}-${baseY}`,
                value: currVal,
                x: baseX,
                y: baseY,
                path: currPath.filter((item) => !!item)
              })
            }
          })
        })
      }
      return _values
    },
    // 计算所有对应条件的值
    dataValues() {
      // 列对应的条件
      const colConditions = convertPathToMap(
        this.colPaths,
        this.localColumns.map(({ key }) => key).concat(this.localValues.length ? ['value'] : [])
      )
      // 行对应的条件
      const rowConditions = convertPathToMap(
        this.rowPaths,
        this.localRows.map(({ key }) => key)
      )
      // console.log('colConditions', colConditions)
      // console.log('rowConditions', rowConditions)
      // 针对没传入行或列的处理
      !colConditions.length && colConditions.push({})
      !rowConditions.length && rowConditions.push({})
      // 过滤数据, 遍历行以及遍历行对应的列
      return rowConditions.map((rowCondition, rowConditionIndex) => {
        const _data = colConditions.map((colCondition, colConditionIndex) => {
          // 存储当前单元对应的数据
          const cellData = {}
          // 当前单元对应的条件
          const conditions = Object.assign({}, rowCondition, colCondition)
          const _filterConditions = Object.fromEntries(
            Object.entries(conditions).filter(
              (item) => item[0] !== 'value'
            )
          )
          // 通过当前单元对应的条件，过滤数据
          const filterData = this._filterData(_filterConditions, this.localData)
          // 对应表格的坐标位置
          const baseX = this.localColumns.length + +Boolean(this.localValues.length) + rowConditionIndex
          const baseY = this.localRows.length + colConditionIndex
          Object.assign(
            cellData,
            mergeBaseInfo({
              conditions,
              x: baseX,
              y: baseY,
              __index: `${baseX}-${baseY}`
            })
          )
          // 针对为指定值 props.values 的空处理(绘制空表格)
          const isEmptyValues = this.localColumns.length && this.localRows.length && !this.localValues.length
          if (isEmptyValues) {
            Object.assign(cellData, { value: '' })
          } else {
            // 从 props.values 中找出对应的值
            const _value = this.values.find(({ key }) => key === conditions.value)
            Object.assign(cellData, { value: _value && _value.key ? this._reduceValue(filterData, _value.key) : '' })
          }
          return cellData
        })
        return {
          __index: _data[0].x,
          data: _data
        }
      })
    }
  },
  watch: {
    watchAllProps() {
      this.init()
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      if (this.rows.length || this.columns.length || this.values.length) {
        this.handleDataClone()
        this.setValuesToColAndRow()
        this.handleCalcData()
        this.handleCombineHeads()
        this.handleCombineValues()
      } else {
        console.warn('[Warn]: props.rows, props.columns, props.values at least one is not empty.')
      }
    },
    // clone data
    handleDataClone() {
      this.localRows = JSON.parse(JSON.stringify(this.rows))
      this.localColumns = JSON.parse(JSON.stringify(this.columns))
      this.localValues = JSON.parse(JSON.stringify(this.values))
      this.localData = Object.freeze(this.data)
    },
    // set the `values` attribute to rows and columns
    setValuesToColAndRow() {
      const rowKeys = this.localRows.map(({ key }) => key)
      const columnKeys = this.localColumns.map(({ key }) => key)
      const rowValues = this._findCategory(rowKeys, this.localData)
      const columnValues = this._findCategory(columnKeys, this.localData)
      this.localRows.forEach((row) => {
        const { key, values } = row
        this.$set(row, 'values', values || rowValues[key] || [])
      })
      this.localColumns.forEach((column) => {
        const { key, values } = column
        this.$set(column, 'values', values || columnValues[key] || [])
      })
    },
    // 合并表头
    handleCombineHeads() {
      let combineColHeads = JSON.parse(JSON.stringify(this.colHeads))
      combineColHeads[0] = combineColHeads[0] || []
      combineColHeads[0].unshift(...this.rowHeads.filter((item) => item.isCorner))
      combineColHeads[combineColHeads.length - 1].unshift(...this.rowHeads.filter((item) => !item.isCorner))
      combineColHeads = combineColHeads.filter((item) => item.length)
      this.combineHeads = combineColHeads
    },
    // 合并值
    handleCombineValues() {
      // values
      const combineValues = []
      const valueRowCount = this.dataValues.length || this.rowHeadValues.length
      for (let i = 0; i < valueRowCount; i++) {
        const _currRowHeadValue = this.rowHeadValues[i] || []
        const _currValue = this.dataValues[i] || {}
        const _row = [...(_currValue.data || [])]
        combineValues.push(
          Object.assign({}, { head: [..._currRowHeadValue] }, { data: _row })
        )
      }
      this.combineValues = combineValues
    },
    // 初始计算值
    handleCalcData() {
      if (!this.localValues.length) return
      const _rowPaths = this._combineRowPaths(
        this.localData,
        ...this.localRows.map(({ key, values }) => { return { key, values } })
      )
      const _rowKeys = this.localRows.map(({ key }) => key)
      const _colPaths = this._combineColPaths(
        ...this.localColumns.map(({ values }) => values)
      )
      const _colKeys = this.localColumns.map(({ key }) => key)
      // conditions of col-head
      const colConditions = convertPathToMap(_colPaths, _colKeys)
      // conditions of row-head
      const rowConditions = convertPathToMap(_rowPaths, _rowKeys)
      // Note: if there are no props.rows or props.columns, push an empty object
      !colConditions.length && colConditions.push({})
      !rowConditions.length && rowConditions.push({})
      // draw data
      this.calcData = Object.freeze(
        rowConditions
          .map((rowCondition, rowConditionIndex) =>
            colConditions
              .map((colCondition, colConditionIndex) => {
                // the condition of current cell
                const conditions = Object.assign({}, rowCondition, colCondition)
                // filter the data
                const filterData = this._filterData(conditions, this.localData)
                // empty cell
                const isEmptyCell = this.localRows.length && this.localColumns.length && !this.localValues.length
                const _values = {}
                // 多个值，多条数据
                this.values.forEach(({ key }) => {
                  _values[key] = isEmptyCell ? '' : this._reduceValue(filterData, key)
                })
                return Object.assign({}, conditions, _values)
              })
              .flat()
          )
          .filter((item) => item.length)
          .flat()
      )
    },
    _combineRowPaths(data, ...arrays) {
      const len = arrays.length
      let _result = []
      if (len) {
        const rowPaths = arrays.reduce((prev, curr) => {
          const arr = []
          prev.values.forEach(_prevEl => {
            const prevKey = prev.key.split(SEPARATOR)
            curr.values.forEach(_currEl => {
              const currKey = curr.key
              const conditions = {}
              prevKey.forEach((key, i) => {
                conditions[key] = _prevEl.split(SEPARATOR)[i]
              })
              conditions[currKey] = _currEl
              // 判断数据里是否有该项
              const filter = data.some((data) => {
                let status = true
                for (const key in conditions) {
                  if (conditions[key] !== data[key]) {
                    status = false
                    return
                  }
                }
                return status
              })
              if (filter) {
                arr.push(_prevEl + SEPARATOR + _currEl)
              }
            })
          })
          return { key: prev.key + SEPARATOR + curr.key, values: arr }
        }) || {}
        _result = rowPaths.values || []
      }
      return _result
    },
    _combineColPaths(...arrays) {
      return arrays.length ? arrays.reduce((prev, curr) => {
        const arr = []
        prev.forEach(_prevEl => {
          curr.forEach(_currEl => {
            arr.push(_prevEl + SEPARATOR + _currEl)
          })
        })
        return arr
      }) : arrays
    },
    _findCategory(keys = [], data = []) {
      const _result = {}
      data.forEach(item => {
        keys.forEach(key => {
          // Remove duplicates
          _result[key] = _result[key] || []
          _result[key].push(item[key])
          _result[key] = [...new Set(_result[key])]
        })
      })
      return _result
    },
    _reduceValue(data, key) {
      if (!data.length) return ''
      return data.reduce((sum, item) => { return sum + Number(item[key]) }, 0)
    },
    _filterData(conditions, data) {
      return data.filter((data) => {
        let status = true
        for (const key in conditions) {
          if (conditions[key] !== data[key]) {
            status = false
            return
          }
        }
        return status
      })
    },
    // get min height by rowspan
    _getMinHeightByRowCount(count) {
      return getHeightByCount(count)
    }
  }
}
</script>

<style lang="scss" scoped>
table {
  border-collapse: collapse;
  border-spacing: 0;
  border: none;
  td, th {
    border: 1px solid #ccc;
    padding: 0;
    vertical-align: middle;
    box-sizing: border-box;
    > div {
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      padding: 5px;
      text-align: center;
      white-space: nowrap;
      width: 100%;
      height: 100%;
      min-height: 36px;
      cursor: default;
      &.col-corner-bg {
      }
    }
  }
}
</style>

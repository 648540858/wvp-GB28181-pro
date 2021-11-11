Table 重封装组件说明
====


封装说明
----

>  基础的使用方式与 API 与 [官方版(Table)](https://vuecomponent.github.io/ant-design-vue/components/table-cn/) 本一致，在其基础上，封装了加载数据的方法。
>
> 你无需在你是用表格的页面进行分页逻辑处理，仅需向 Table 组件传递绑定 `:data="Promise"` 对象即可

该 `table` 由 [@Saraka](https://github.com/saraka-tsukai) 完成封装


例子1
----
（基础使用）

```vue

<template>
  <s-table
    ref="table"
    size="default"
    :rowKey="(record) => record.data.id"
    :columns="columns"
    :data="loadData"
    :rowSelection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
  >
  </s-table>
</template>

<script>
  import STable from '@/components'

  export default {
    components: {
      STable
    },
    data() {
      return {
        columns: [
          {
            title: '规则编号',
            dataIndex: 'no'
          },
          {
            title: '描述',
            dataIndex: 'description'
          },
          {
            title: '服务调用次数',
            dataIndex: 'callNo',
            sorter: true,
            needTotal: true,
            customRender: (text) => text + ' 次'
          },
          {
            title: '状态',
            dataIndex: 'status',
            needTotal: true
          },
          {
            title: '更新时间',
            dataIndex: 'updatedAt',
            sorter: true
          }
        ],
        // 查询条件参数
        queryParam: {},
        // 加载数据方法 必须为 Promise 对象
        loadData: parameter => {
          return this.$http.get('/service', {
            params: Object.assign(parameter, this.queryParam)
          }).then(res => {
            return res.result
          })
        },
        selectedRowKeys: [],
        selectedRows: []
      }
    },
    methods: {
      onSelectChange (selectedRowKeys, selectedRows) {
         this.selectedRowKeys = selectedRowKeys
         this.selectedRows = selectedRows
       }
    }
  }
</script>

```



例子2
----

（简单的表格，最后一列是各种操作）

```vue
<template>
  <s-table
    ref="table"
    size="default"
    :columns="columns"
    :data="loadData"
  >
    <span slot="action" slot-scope="text, record">
      <a>编辑</a>
      <a-divider type="vertical"/>
      <a-dropdown>
        <a class="ant-dropdown-link">
          更多 <a-icon type="down"/>
        </a>
        <a-menu slot="overlay">
          <a-menu-item>
            <a href="javascript:;">1st menu item</a>
          </a-menu-item>
          <a-menu-item>
            <a href="javascript:;">2nd menu item</a>
          </a-menu-item>
          <a-menu-item>
            <a href="javascript:;">3rd menu item</a>
          </a-menu-item>
        </a-menu>
      </a-dropdown>
    </span>
  </s-table>
</template>

<script>
  import STable from '@/components/table/'

  export default {
    components: {
      STable
    },
    data() {
      return {
        columns: [
          {
            title: '规则编号',
            dataIndex: 'no'
          },
          {
            title: '描述',
            dataIndex: 'description'
          },
          {
            title: '服务调用次数',
            dataIndex: 'callNo',
          },
          {
            title: '状态',
            dataIndex: 'status',
          },
          {
            title: '更新时间',
            dataIndex: 'updatedAt',
          },
          {
            table: '操作',
            dataIndex: 'action',
            scopedSlots: {customRender: 'action'},
          }
        ],
        // 查询条件参数
        queryParam: {},
        // 加载数据方法 必须为 Promise 对象
        loadData: parameter => {
          return this.$http.get('/service', {
            params: Object.assign(parameter, this.queryParam)
          }).then(res => {
            return res.result
          })
        },
      }
    },
    methods: {
      edit(row) {
        // axios 发送数据到后端 修改数据成功后
        // 调用 refresh() 重新加载列表数据
        // 这里 setTimeout 模拟发起请求的网络延迟..
        setTimeout(() => {
          this.$refs.table.refresh() // refresh() 不传参默认值 false 不刷新到分页第一页
        }, 1500)

      }
    }
  }
</script>
```



内置方法
----

通过 `this.$refs.table` 调用

`this.$refs.table.refresh(true)` 刷新列表 (用户新增/修改数据后，重载列表数据)

> 注意：要调用 `refresh(bool)` 需要给表格组件设定 `ref` 值
>
> `refresh()` 方法可以传一个 `bool` 值，当有传值 或值为 `true` 时，则刷新时会强制刷新到第一页（常用户页面 搜索 按钮进行搜索时，结果从第一页开始分页）


内置属性
----
> 除去 `a-table` 自带属性外，还而外提供了一些额外属性属性  


| 属性           | 说明                                            | 类型              | 默认值 |
| -------------- | ----------------------------------------------- | ----------------- | ------ |
| alert          | 设置是否显示表格信息栏                          | [object, boolean] | null   |
| showPagination | 显示分页选择器，可传 'auto' \| boolean          | [string, boolean] | 'auto' |
| data           | 加载数据方法 必须为 `Promise` 对象 **必须绑定** | Promise           | -      |


`alert` 属性对象：

```javascript
alert: {
  show: Boolean, 
  clear: [Function, Boolean]
}
```

注意事项
----

> 你可能需要为了与后端提供的接口返回结果一致而去修改以下代码：
> (需要注意的是，这里的修改是全局性的，意味着整个项目所有使用该 table 组件都需要遵守这个返回结果定义的字段。)
>
> 文档中的结构有可能由于组件 bug 进行修正而改动。实际修改请以当时最新版本为准

修改 `@/components/table/index.js`  第 156 行起



```javascript
result.then(r => {
          this.localPagination = this.showPagination && Object.assign({}, this.localPagination, {
            current: r.pageNo, // 返回结果中的当前分页数
            total: r.totalCount, // 返回结果中的总记录数
            showSizeChanger: this.showSizeChanger,
            pageSize: (pagination && pagination.pageSize) ||
              this.localPagination.pageSize
          }) || false
          // 为防止删除数据后导致页面当前页面数据长度为 0 ,自动翻页到上一页
          if (r.data.length === 0 && this.showPagination && this.localPagination.current > 1) {
            this.localPagination.current--
            this.loadData()
            return
          }

          // 这里用于判断接口是否有返回 r.totalCount 且 this.showPagination = true 且 pageNo 和 pageSize 存在 且 totalCount 小于等于 pageNo * pageSize 的大小
          // 当情况满足时，表示数据不满足分页大小，关闭 table 分页功能
          try {
            if ((['auto', true].includes(this.showPagination) && r.totalCount <= (r.pageNo * this.localPagination.pageSize))) {
              this.localPagination.hideOnSinglePage = true
            }
          } catch (e) {
            this.localPagination = false
          }
          console.log('loadData -> this.localPagination', this.localPagination)
          this.localDataSource = r.data // 返回结果中的数组数据
          this.localLoading = false
        })
```
返回 JSON 例子：
```json
{
  "message": "",
  "result": {
    "data": [{
        id: 1,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/WdGqmHpayyMjiEhcKoVE.png',
        title: 'Alipay',
        description: '那是一种内在的东西， 他们到达不了，也无法触及的',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      },
      {
        id: 2,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/zOsKZmFRdUtvpqCImOVY.png',
        title: 'Angular',
        description: '希望是一个好东西，也许是最好的，好东西是不会消亡的',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      },
      {
        id: 3,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/dURIMkkrRFpPgTuzkwnB.png',
        title: 'Ant Design',
        description: '城镇中有那么多的酒馆，她却偏偏走进了我的酒馆',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      },
      {
        id: 4,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/sfjbOqnsXXJgNCjCzDBL.png',
        title: 'Ant Design Pro',
        description: '那时候我只会想自己想要什么，从不想自己拥有什么',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      },
      {
        id: 5,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/siCrBXXhmvTQGWPNLBow.png',
        title: 'Bootstrap',
        description: '凛冬将至',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      },
      {
        id: 6,
        cover: 'https://gw.alipayobjects.com/zos/rmsportal/ComBAopevLwENQdKWiIn.png',
        title: 'Vue',
        description: '生命就像一盒巧克力，结果往往出人意料',
        status: 1,
        updatedAt: '2018-07-26 00:00:00'
      }
    ],
    "pageSize": 10,
    "pageNo": 0,
    "totalPage": 6,
    "totalCount": 57
  },
  "status": 200,
  "timestamp": 1534955098193
}
```



更新时间
----

该文档最后更新于： 2019-06-23 PM 17:19
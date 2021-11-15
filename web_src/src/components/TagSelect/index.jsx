import PropTypes from 'ant-design-vue/es/_util/vue-types'
import Option from './TagSelectOption.jsx'
import { filterEmpty } from '@/components/_util/util'

export default {
  Option,
  name: 'TagSelect',
  model: {
    prop: 'checked',
    event: 'change'
  },
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-tag-select'
    },
    defaultValue: {
      type: PropTypes.array,
      default: null
    },
    value: {
      type: PropTypes.array,
      default: null
    },
    expandable: {
      type: Boolean,
      default: false
    },
    hideCheckAll: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      expand: false,
      localCheckAll: false,
      items: this.getItemsKey(filterEmpty(this.$slots.default)),
      val: this.value || this.defaultValue || []
    }
  },
  methods: {
    onChange (checked) {
      const key = Object.keys(this.items).filter(key => key === checked.value)
      this.items[key] = checked.checked
      const bool = Object.values(this.items).lastIndexOf(false)
      if (bool === -1) {
        this.localCheckAll = true
      } else {
        this.localCheckAll = false
      }
    },
    onCheckAll (checked) {
      Object.keys(this.items).forEach(v => {
        this.items[v] = checked.checked
      })
      this.localCheckAll = checked.checked
    },
    getItemsKey (items) {
      const totalItem = {}
      items.forEach(item => {
        totalItem[item.componentOptions.propsData && item.componentOptions.propsData.value] = false
      })
      return totalItem
    },
    // CheckAll Button
    renderCheckAll () {
      const props = {
        on: {
          change: (checked) => {
            this.onCheckAll(checked)
            checked.value = 'total'
            this.$emit('change', checked)
          }
        }
      }
      const checkAllElement = <Option key={'total'} checked={this.localCheckAll} {...props}>All</Option>
      return !this.hideCheckAll && checkAllElement || null
    },
    // expandable
    renderExpandable () {

    },
    // render option
    renderTags (items) {
      const listeners = {
        change: (checked) => {
          this.onChange(checked)
          this.$emit('change', checked)
        }
      }

      return items.map(vnode => {
        const options = vnode.componentOptions
        options.listeners = listeners
        return vnode
      })
    }
  },
  render () {
    const { $props: { prefixCls } } = this
    const classString = {
      [`${prefixCls}`]: true
    }
    const tagItems = filterEmpty(this.$slots.default)
    return (
      <div class={classString}>
        {this.renderCheckAll()}
        {this.renderTags(tagItems)}
      </div>
    )
  }
}

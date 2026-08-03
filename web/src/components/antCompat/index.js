import {
  Comment,
  Fragment,
  cloneVNode,
  defineComponent,
  h,
  nextTick,
  ref,
  render,
  resolveComponent
} from 'vue'
import {
  Alert,
  AutoComplete,
  Breadcrumb,
  Button,
  Card,
  Checkbox,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Dropdown,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Menu,
  Modal,
  PageHeader,
  Pagination,
  Popover,
  Progress,
  Radio,
  Row,
  Select,
  Slider,
  Space,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Upload,
  message
} from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  ArrowUpOutlined,
  BellOutlined,
  CameraOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  CloseOutlined,
  CloudOutlined,
  CopyOutlined,
  DeleteFilled,
  DeleteOutlined,
  DownOutlined,
  DownloadOutlined,
  EditOutlined,
  EnvironmentOutlined,
  ExclamationCircleOutlined,
  FileAddOutlined,
  FileOutlined,
  FormOutlined,
  FullscreenOutlined,
  HomeOutlined,
  InfoCircleOutlined,
  LinkOutlined,
  LoadingOutlined,
  LockOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MinusOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  PoweroffOutlined,
  ReloadOutlined,
  SearchOutlined,
  ShareAltOutlined,
  SoundOutlined,
  UploadOutlined,
  VideoCameraOutlined,
  ZoomInOutlined,
  ZoomOutOutlined
} from '@ant-design/icons-vue'
import VueEasyTree from './tree'
import dragDialog from '@/directive/el-drag-dialog/drag'
import { dialogText } from '@/components/ui/config'
import './styles.scss'

const iconMap = {
  '-right': DownOutlined,
  'arrow-down': DownOutlined,
  bell: BellOutlined,
  camera: CameraOutlined,
  check: CheckOutlined,
  chicken: LinkOutlined,
  'circle-check': CheckCircleOutlined,
  'circle-close': CloseCircleOutlined,
  close: CloseOutlined,
  cloudy: CloudOutlined,
  coordinate: EnvironmentOutlined,
  delete: DeleteOutlined,
  'delete-solid': DeleteFilled,
  document: FileOutlined,
  'document-add': FileAddOutlined,
  'document-copy': CopyOutlined,
  download: DownloadOutlined,
  edit: EditOutlined,
  'edit-outline': FormOutlined,
  error: CloseCircleOutlined,
  'full-screen': FullscreenOutlined,
  info: InfoCircleOutlined,
  link: LinkOutlined,
  loading: LoadingOutlined,
  'location-information': EnvironmentOutlined,
  lock: LockOutlined,
  'map-location': EnvironmentOutlined,
  microphone: SoundOutlined,
  minus: MinusOutlined,
  'picture-outline': PictureOutlined,
  plus: PlusOutlined,
  position: EnvironmentOutlined,
  refresh: ReloadOutlined,
  'refresh-right': ReloadOutlined,
  search: SearchOutlined,
  's-fold': MenuFoldOutlined,
  's-home': HomeOutlined,
  's-open': FileOutlined,
  's-unfold': MenuUnfoldOutlined,
  share: ShareAltOutlined,
  success: CheckCircleOutlined,
  'switch-button': PoweroffOutlined,
  top: ArrowUpOutlined,
  upload: UploadOutlined,
  upload2: UploadOutlined,
  'video-camera': VideoCameraOutlined,
  'video-play': PlayCircleOutlined,
  'warning-outline': ExclamationCircleOutlined,
  'zoom-in': ZoomInOutlined,
  'zoom-out': ZoomOutOutlined
}

const iconLabelMap = {
  camera: '截图',
  close: '关闭',
  delete: '删除',
  'delete-solid': '删除',
  document: '查看',
  'document-add': '新增',
  'document-copy': '复制',
  download: '下载',
  edit: '编辑',
  'edit-outline': '编辑',
  'full-screen': '全屏',
  plus: '新增',
  refresh: '刷新',
  'refresh-right': '刷新',
  search: '搜索',
  upload: '上传',
  upload2: '上传',
  'video-play': '播放',
  'zoom-in': '放大',
  'zoom-out': '缩小'
}

function iconName(name = '') {
  return name.replace(/^el-icon-/, '')
}

export const AntIcon = defineComponent({
  name: 'AntIcon',
  inheritAttrs: false,
  props: {
    name: { type: String, default: '' }
  },
  setup(props, { attrs }) {
    return () => {
      const component = iconMap[iconName(props.name)] || InfoCircleOutlined
      return h(component, { ...attrs, class: ['ant-legacy-icon', attrs.class] })
    }
  }
})

function without(source, keys) {
  return Object.fromEntries(Object.entries(normalizeAttrs(source)).filter(([key]) => !keys.includes(key)))
}

function normalizeAttrs(source) {
  return Object.fromEntries(Object.entries(source).map(([key, value]) => [
    key.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase()),
    value
  ]))
}

function legacySize(size) {
  if (size === 'mini' || size === 'small') return 'small'
  if (size === 'medium') return 'middle'
  return size
}

function flatten(nodes = []) {
  return nodes.flatMap(node => {
    if (!node || typeof node === 'string') return node ? [node] : []
    if (node.type === Fragment && Array.isArray(node.children)) return flatten(node.children)
    return node.type === Comment ? [] : [node]
  })
}

function legacySlots(slots) {
  const result = {}
  Object.entries(slots).forEach(([name, slot]) => {
    if (name !== 'default') result[name] = slot
  })
  const defaults = flatten(slots.default ? slots.default() : [])
  result.defaultNodes = []
  defaults.forEach(node => {
    const name = node && node.props && node.props.slot
    if (name) {
      const cloned = cloneVNode(node, { slot: null })
      if (!result[name]) result[name] = () => [cloned]
    } else {
      result.defaultNodes.push(node)
    }
  })
  result.default = () => result.defaultNodes
  return result
}

const ElButton = defineComponent({
  name: 'ElButton',
  inheritAttrs: false,
  props: {
    icon: String,
    size: String,
    type: String,
    circle: Boolean,
    plain: Boolean,
    nativeType: String
  },
  setup(props, { attrs, slots }) {
    return () => {
      const defaultNodes = slots.default ? slots.default() : []
      const hasLabel = Boolean(flatten(defaultNodes).length)
      const semanticType = props.plain && props.type === 'primary'
        ? 'default'
        : ['primary', 'default', 'dashed', 'link', 'text'].includes(props.type)
        ? (props.type === 'text' ? 'link' : props.type)
        : (props.type === 'danger' ? 'primary' : 'default')
      return h(Button, {
        ...attrs,
        type: semanticType,
        danger: props.type === 'danger',
        size: legacySize(props.size),
        shape: props.circle ? 'circle' : undefined,
        htmlType: props.nativeType,
        'aria-label': attrs['aria-label'] || (props.icon && !hasLabel ? (iconLabelMap[iconName(props.icon)] || '操作') : undefined),
        icon: props.icon ? h(AntIcon, { name: props.icon }) : undefined,
        class: [
          'el-button',
          props.type && `el-button--${props.type}`,
          props.circle && 'is-circle',
          attrs.class
        ],
        style: [props.plain && props.type === 'primary' ? { color: '#1677ff', borderColor: '#1677ff' } : null, attrs.style]
      }, { ...slots, default: () => defaultNodes })
    }
  }
})

function inputValue(props) {
  return props.modelValue !== undefined ? props.modelValue : props.value
}

const ElInput = defineComponent({
  name: 'ElInput',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    type: String,
    size: String,
    clearable: Boolean,
    showPassword: Boolean,
    showWordLimit: Boolean,
    prefixIcon: String,
    suffixIcon: String
  },
  emits: ['update:modelValue', 'input', 'change', 'clear', 'focus', 'blur'],
  setup(props, { attrs, slots, emit, expose }) {
    const inner = ref()
    expose({
      focus: () => inner.value?.focus(),
      blur: () => inner.value?.blur(),
      select: () => inner.value?.select?.()
    })
    return () => {
      const grouped = legacySlots(slots)
      const component = props.showPassword
        ? Input.Password
        : (props.type === 'textarea' ? Input.TextArea : Input)
      const componentSlots = { ...grouped }
      delete componentSlots.defaultNodes
      if (props.prefixIcon) componentSlots.prefix = () => h(AntIcon, { name: props.prefixIcon })
      if (props.suffixIcon) componentSlots.suffix = () => h(AntIcon, { name: props.suffixIcon })
      if (grouped.prepend) componentSlots.addonBefore = grouped.prepend
      if (grouped.append) componentSlots.addonAfter = grouped.append
      return h(component, {
        ...attrs,
        ref: inner,
        value: inputValue(props),
        type: component === Input ? props.type : undefined,
        size: legacySize(props.size),
        allowClear: props.clearable,
        showCount: props.showWordLimit,
        class: ['el-input', props.type === 'textarea' && 'el-textarea', attrs.class],
        'onUpdate:value': value => {
          emit('update:modelValue', value)
          emit('input', value)
        },
        onChange: event => emit('change', event?.target?.value ?? event),
        onClear: () => emit('clear'),
        onFocus: event => emit('focus', event),
        onBlur: event => emit('blur', event)
      }, componentSlots)
    }
  }
})

function createValueComponent(name, component, modelProp = 'value', className = '') {
  return defineComponent({
    name,
    inheritAttrs: false,
    props: {
      modelValue: { default: undefined },
      value: { default: undefined },
      size: String
    },
    emits: ['update:modelValue', 'input', 'change', 'focus', 'blur'],
    setup(props, { attrs, slots, emit, expose }) {
      const inner = ref()
      expose({ focus: () => inner.value?.focus(), blur: () => inner.value?.blur() })
      return () => h(component, {
        ...attrs,
        ref: inner,
        [modelProp]: inputValue(props),
        size: legacySize(props.size),
        class: [className, attrs.class],
        [`onUpdate:${modelProp}`]: value => {
          emit('update:modelValue', value)
          emit('input', value)
        },
        onChange: value => emit('change', value?.target?.[modelProp] ?? value),
        onFocus: event => emit('focus', event),
        onBlur: event => emit('blur', event)
      }, slots)
    }
  })
}

const ElInputNumber = createValueComponent('ElInputNumber', InputNumber, 'value', 'el-input-number')

function selectOptions(nodes) {
  return flatten(nodes).flatMap(node => {
    const optionAttrs = normalizeAttrs(node?.props || {})
    if (node?.type?.name === 'ElOptionGroup') {
      return [{
        label: optionAttrs.label,
        options: selectOptions(node.children?.default ? node.children.default() : [])
      }]
    }
    if (node?.type?.name !== 'ElOption') return []
    return [{
      value: optionAttrs.value,
      label: node.children?.default ? node.children.default() : optionAttrs.label,
      disabled: optionAttrs.disabled
    }]
  })
}

const ElSelect = defineComponent({
  name: 'ElSelect',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    size: String,
    clearable: Boolean,
    filterable: Boolean,
    multiple: Boolean,
    collapseTags: Boolean
  },
  emits: ['update:modelValue', 'input', 'change', 'visible-change', 'clear', 'remove-tag'],
  setup(props, { attrs, slots, emit }) {
    return () => h(Select, {
      ...attrs,
      value: inputValue(props),
      options: selectOptions(slots.default ? slots.default() : []),
      size: legacySize(props.size),
      allowClear: props.clearable,
      showSearch: props.filterable,
      mode: props.multiple ? 'multiple' : undefined,
      maxTagCount: props.collapseTags ? 'responsive' : attrs.maxTagCount,
      class: ['el-select', attrs.class],
      'onUpdate:value': value => {
        emit('update:modelValue', value)
        emit('input', value)
      },
      onChange: (value, option) => emit('change', value, option),
      onClear: () => emit('clear'),
      onDeselect: value => emit('remove-tag', value),
      onDropdownVisibleChange: value => emit('visible-change', value)
    })
  }
})

const ElOption = defineComponent({
  name: 'ElOption',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(Select.Option, { ...attrs, class: ['el-select-dropdown__item', attrs.class] }, {
      default: () => slots.default ? slots.default() : attrs.label
    })
  }
})

const ElOptionGroup = defineComponent({
  name: 'ElOptionGroup',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(Select.OptGroup, attrs, slots)
  }
})

const ElCheckbox = defineComponent({
  name: 'ElCheckbox',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    trueLabel: { default: true },
    falseLabel: { default: false }
  },
  emits: ['update:modelValue', 'input', 'change'],
  setup(props, { attrs, slots, emit }) {
    return () => h(Checkbox, {
      ...attrs,
      checked: inputValue(props) === props.trueLabel,
      class: ['el-checkbox', attrs.class],
      'onUpdate:checked': checked => {
        const value = checked ? props.trueLabel : props.falseLabel
        emit('update:modelValue', value)
        emit('input', value)
      },
      onChange: event => emit('change', event.target.checked ? props.trueLabel : props.falseLabel)
    }, { default: () => slots.default ? slots.default() : attrs.label })
  }
})

const ElRadioGroup = createValueComponent('ElRadioGroup', Radio.Group, 'value', 'el-radio-group')
const ElSlider = createValueComponent('ElSlider', Slider, 'value', 'el-slider')
const ElAutocomplete = defineComponent({
  name: 'ElAutocomplete',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    fetchSuggestions: Function,
    valueKey: { type: String, default: 'value' }
  },
  emits: ['update:modelValue', 'input', 'change', 'select'],
  setup(props, { attrs, slots, emit }) {
    const options = ref([])
    const search = query => {
      props.fetchSuggestions?.(query, items => {
        options.value = (items || []).map(item => ({
          value: item[props.valueKey] ?? item.value,
          item
        }))
      })
    }
    return () => h(AutoComplete, {
      ...attrs,
      value: inputValue(props),
      options: options.value,
      class: ['el-autocomplete', attrs.class],
      onSearch: search,
      'onUpdate:value': value => {
        emit('update:modelValue', value)
        emit('input', value)
      },
      onChange: value => emit('change', value),
      onSelect: (value, option) => emit('select', option.item || option)
    }, {
      option: option => slots.default ? slots.default({ item: option.item || option }) : option.value
    })
  }
})

const ElRadio = defineComponent({
  name: 'ElRadio',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    label: { default: undefined }
  },
  emits: ['update:modelValue', 'input', 'change'],
  setup(props, { attrs, slots, emit }) {
    return () => {
      const value = props.label !== undefined ? props.label : props.value
      return h(Radio, {
        ...attrs,
        value,
        checked: props.modelValue === undefined ? undefined : props.modelValue === value,
        class: ['el-radio', attrs.class],
        onChange: event => {
          if (props.modelValue !== undefined) {
            emit('update:modelValue', value)
            emit('input', value)
          }
          emit('change', value, event)
        }
      }, { default: () => slots.default ? slots.default() : props.label })
    }
  }
})

const ElRadioButton = defineComponent({
  name: 'ElRadioButton',
  inheritAttrs: false,
  props: {
    label: { default: undefined },
    value: { default: undefined }
  },
  setup(props, { attrs, slots }) {
    return () => h(Radio.Button, { ...attrs, value: props.label ?? props.value }, {
      default: () => slots.default ? slots.default() : props.label
    })
  }
})

const ElSwitch = defineComponent({
  name: 'ElSwitch',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    activeValue: { default: true },
    inactiveValue: { default: false },
    activeText: String,
    inactiveText: String
  },
  emits: ['update:modelValue', 'input', 'change'],
  setup(props, { attrs, emit }) {
    return () => h(Switch, {
      ...attrs,
      checked: inputValue(props),
      checkedValue: props.activeValue,
      unCheckedValue: props.inactiveValue,
      checkedChildren: props.activeText,
      unCheckedChildren: props.inactiveText,
      class: ['el-switch', attrs.class],
      'onUpdate:checked': value => {
        emit('update:modelValue', value)
        emit('input', value)
      },
      onChange: value => emit('change', value)
    })
  }
})

function dateFormat(format) {
  return format && format
    .replace(/yyyy/g, 'YYYY')
    .replace(/dd/g, 'DD')
}

function legacyPickerPresets(options, isRange) {
  return options?.shortcuts?.map(shortcut => ({
    label: shortcut.text,
    value: () => {
      let picked
      shortcut.onClick?.({
        $emit: (event, value) => {
          if (event === 'pick') picked = value
        }
      })
      if (picked === undefined) return isRange ? [] : null
      return isRange ? picked.map(value => dayjs(value)) : dayjs(picked)
    }
  }))
}

function normalizeFormRules(rules) {
  if (!rules) return rules
  return Object.fromEntries(Object.entries(rules).map(([field, value]) => [
    field,
    (Array.isArray(value) ? value : [value]).map(rule => {
      if (typeof rule?.validator !== 'function' || rule.validator.length < 3) return rule
      const validator = rule.validator
      return {
        ...rule,
        validator: (currentRule, currentValue) => new Promise((resolve, reject) => {
          const callback = error => error
            ? reject(error instanceof Error ? error : new Error(String(error)))
            : resolve()
          try {
            const result = validator(currentRule, currentValue, callback)
            if (result && typeof result.then === 'function') result.then(resolve, reject)
          } catch (error) {
            reject(error)
          }
        })
      }
    })
  ]))
}

const ElDatePicker = defineComponent({
  name: 'ElDatePicker',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined },
    type: String,
    size: String,
    valueFormat: String,
    format: String,
    startPlaceholder: String,
    endPlaceholder: String,
    pickerOptions: Object
  },
  emits: ['update:modelValue', 'input', 'change', 'blur', 'focus'],
  setup(props, { attrs, emit }) {
    return () => {
      const isRange = props.type === 'daterange' || props.type === 'datetimerange'
      const normalizedAttrs = normalizeAttrs(attrs)
      const component = isRange ? DatePicker.RangePicker : DatePicker
      const picker = ['month', 'year', 'week', 'quarter'].includes(props.type) ? props.type : undefined
      const showTime = props.type === 'datetime' || props.type === 'datetimerange'
      const displayFormat = dateFormat(props.format) || (
        props.type === 'month'
          ? 'YYYY-MM'
          : (props.type === 'year' ? 'YYYY' : (showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'))
      )
      const rangePlaceholder = props.startPlaceholder || props.endPlaceholder
        ? [props.startPlaceholder || '开始日期', props.endPlaceholder || '结束日期']
        : undefined
      const cellClassName = props.pickerOptions?.cellClassName
      return h(component, {
        ...normalizedAttrs,
        value: inputValue(props),
        valueFormat: dateFormat(props.valueFormat),
        format: displayFormat,
        picker,
        showTime,
        placeholder: isRange ? rangePlaceholder : normalizedAttrs.placeholder,
        disabledDate: props.pickerOptions?.disabledDate
          ? current => props.pickerOptions.disabledDate(current?.toDate?.() ?? current)
          : normalizedAttrs.disabledDate,
        presets: legacyPickerPresets(props.pickerOptions, isRange),
        separator: normalizedAttrs.rangeSeparator,
        size: legacySize(props.size),
        class: ['el-date-editor', normalizedAttrs.class],
        'onUpdate:value': value => {
          emit('update:modelValue', value)
          emit('input', value)
        },
        onChange: (value, text) => emit('change', value, text),
        onBlur: event => emit('blur', event),
        onFocus: event => emit('focus', event)
      }, cellClassName ? {
        dateRender: current => h('div', {
          class: ['ant-picker-cell-inner', cellClassName(current?.toDate?.() ?? current)]
        }, current.date())
      } : undefined)
    }
  }
})

const ElForm = defineComponent({
  name: 'ElForm',
  inheritAttrs: false,
  props: {
    model: Object,
    rules: Object,
    size: String,
    inline: Boolean,
    labelWidth: String,
    labelPosition: String
  },
  setup(props, { attrs, slots, expose }) {
    const inner = ref()
    const validate = callback => {
      const promise = inner.value?.validate()
      if (callback && promise) {
        promise.then(() => callback(true)).catch(error => callback(false, error?.errorFields))
      }
      return promise
    }
    expose({
      validate,
      validateField: field => inner.value?.validateFields([field]).catch(() => undefined),
      clearValidate: fields => inner.value?.clearValidate(fields),
      resetFields: fields => inner.value?.resetFields(fields),
      scrollToField: field => inner.value?.scrollToField(field)
    })
    return () => h(Form, {
      ...attrs,
      ref: inner,
      model: props.model,
      rules: normalizeFormRules(props.rules),
      size: legacySize(props.size),
      layout: props.inline ? 'inline' : (props.labelPosition === 'top' ? 'vertical' : 'horizontal'),
      labelCol: props.labelWidth ? { style: { width: props.labelWidth } } : attrs.labelCol,
      class: ['el-form', props.inline && 'el-form--inline', attrs.class]
    }, slots)
  }
})

const ElFormItem = defineComponent({
  name: 'ElFormItem',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(Form.Item, {
      ...without(attrs, ['prop']),
      name: attrs.name || attrs.prop,
      class: ['el-form-item', attrs.class]
    }, slots)
  }
})

function dialogMetrics(width) {
  const source = width === undefined || width === null || width === ''
    ? '720px'
    : (typeof width === 'number' ? `${width}px` : String(width).trim())
  const match = source.match(/^(\d+(?:\.\d+)?)(px|rem|%|vw)?$/)
  if (!match) return { size: 'medium', width: source }

  const value = Number(match[1])
  const unit = match[2] || 'px'
  const pixels = unit === 'rem' ? value * 16 : (unit === 'px' ? value : null)
  let size
  if (unit === '%' || unit === 'vw') {
    size = value <= 45 ? 'small' : (value <= 65 ? 'medium' : 'large')
  } else {
    size = pixels <= 480 ? 'small' : (pixels < 900 ? 'medium' : 'large')
  }
  const limits = {
    small: [400, 480],
    medium: [600, 800],
    large: [900, 1200]
  }[size]
  return {
    size,
    width: `min(clamp(${limits[0]}px, ${source}, ${limits[1]}px), calc(100vw - 32px))`
  }
}

const ElDialog = defineComponent({
  name: 'ElDialog',
  inheritAttrs: false,
  props: {
    visible: { type: Boolean, default: undefined },
    modelValue: { type: Boolean, default: undefined },
    beforeClose: Function
  },
  emits: ['update:visible', 'update:modelValue', 'open', 'opened', 'close', 'closed'],
  setup(props, { attrs, slots, emit }) {
    const close = event => {
      const done = () => {
        emit('update:visible', false)
        emit('update:modelValue', false)
        emit('close', event)
      }
      props.beforeClose ? props.beforeClose(done) : done()
    }
    return () => {
      const normalizedAttrs = normalizeAttrs(attrs)
      const grouped = legacySlots(slots)
      const open = props.visible !== undefined ? props.visible : Boolean(props.modelValue)
      const dialog = dialogMetrics(normalizedAttrs.width)
      const modalSlots = {
        default: () => h('div', { class: 'el-dialog__body' }, grouped.defaultNodes),
        title: () => h('div', { class: 'el-dialog__header' }, grouped.title ? grouped.title() : normalizedAttrs.title)
      }
      modalSlots.footer = grouped.footer
        ? () => h('div', { class: 'el-dialog__footer' }, grouped.footer())
        : () => null
      return h(Modal, {
        ...without(normalizedAttrs, ['title', 'closeOnClickModal', 'closeOnPressEscape', 'appendToBody', 'destroyOnClose', 'showClose', 'top', 'center']),
        open,
        title: undefined,
        footer: null,
        closable: normalizedAttrs.showClose !== false,
        closeIcon: h('span', {
          class: 'ant-compat-dialog-close-icon',
          title: dialogText.closeText,
          'aria-label': dialogText.closeText
        }, [h(CloseOutlined)]),
        width: dialog.width,
        maskClosable: normalizedAttrs.closeOnClickModal !== false,
        keyboard: normalizedAttrs.closeOnPressEscape !== false,
        destroyOnClose: normalizedAttrs.destroyOnClose,
        centered: false,
        style: [normalizedAttrs.top ? { top: normalizedAttrs.top } : null, normalizedAttrs.style],
        wrapClassName: [
          'el-dialog-wrapper',
          `pro-dialog--${dialog.size}`,
          normalizedAttrs.center && 'el-dialog--center',
          normalizedAttrs.wrapClassName
        ].filter(Boolean).join(' '),
        class: ['el-dialog', normalizedAttrs.class],
        onCancel: close,
        onAfterClose: () => emit('closed'),
        'onUpdate:open': value => {
          if (!value) close()
        }
      }, modalSlots)
    }
  }
})

const ElTableColumn = defineComponent({ name: 'ElTableColumn', render: () => null })

function tableActionContent(content) {
  const nodes = flatten(Array.isArray(content) ? content : [content])
    .filter(node => node && node.type !== Divider && node.type?.name !== 'ElDivider')
  return h(Space, {
    class: ['ant-compat-table-action-list', nodes.length > 3 && 'ant-compat-table-action-list--dense'],
    size: 'small',
    wrap: false
  }, {
    default: () => nodes
  })
}

function tableColumns(nodes, selection) {
  return flatten(nodes).flatMap((node, index) => {
    if (!node || !node.props) return []
    const props = normalizeAttrs(node.props)
    if (props.type === 'selection') {
      selection.value = props
      return []
    }
    const dataIndex = props.prop ? props.prop.split('.') : undefined
    const actionColumn = props.label === '操作'
    const column = {
      key: props.columnKey || props.prop || `${props.label || 'column'}-${index}`,
      title: node.children?.header ? node.children.header() : props.label,
      dataIndex,
      align: props.align,
      fixed: props.fixed === true ? 'left' : (props.fixed || (actionColumn ? 'right' : undefined)),
      width: props.width || props.minWidth || (actionColumn ? 220 : undefined),
      className: [props.className, actionColumn && 'ant-compat-table-actions'].filter(Boolean).join(' '),
      ellipsis: props.showOverflowTooltip,
      sorter: props.sortable ? true : undefined
    }
    const scoped = node.children?.default
    if (scoped) {
      column.customRender = ({ record, index: rowIndex, text }) => {
        const content = scoped({
          row: record,
          $index: rowIndex,
          column,
          value: text
        })
        return actionColumn ? tableActionContent(content) : content
      }
    } else if (typeof props.formatter === 'function') {
      column.customRender = ({ record, index: rowIndex, text }) => props.formatter(record, column, text, rowIndex)
    } else if (props.type === 'index') {
      column.customRender = ({ index: rowIndex }) => rowIndex + 1
    }
    return [column]
  })
}

const ElTable = defineComponent({
  name: 'ElTable',
  inheritAttrs: false,
  emits: ['selection-change', 'current-change', 'row-click', 'row-dblclick', 'sort-change', 'expand-change'],
  setup(_, { attrs, slots, emit, expose }) {
    const selectedKeys = ref([])
    const selectedRows = ref([])
    const selectionColumn = ref(null)
    const keyFor = record => {
      const tableAttrs = normalizeAttrs(attrs)
      const rowKey = tableAttrs.rowKey
      if (typeof rowKey === 'function') return rowKey(record)
      if (typeof rowKey === 'string') return record?.[rowKey]
      return record?.id ?? record?.deviceId ?? record?.gbDeviceId ?? (tableAttrs.data || []).indexOf(record)
    }
    const clearSelection = () => {
      selectedKeys.value = []
      selectedRows.value = []
      emit('selection-change', [])
    }
    expose({
      clearSelection,
      doLayout: () => undefined,
      setCurrentRow: row => emit('current-change', row),
      toggleRowSelection: (row, selected = true) => {
        const data = normalizeAttrs(attrs).data || []
        const key = keyFor(row, data.indexOf(row))
        const keys = new Set(selectedKeys.value)
        selected ? keys.add(key) : keys.delete(key)
        selectedKeys.value = Array.from(keys)
        selectedRows.value = data.filter((item, index) => keys.has(keyFor(item, index)))
        emit('selection-change', selectedRows.value)
      }
    })
    return () => {
      const tableAttrs = normalizeAttrs(attrs)
      selectionColumn.value = null
      const columns = tableColumns(slots.default ? slots.default() : [], selectionColumn)
      const dataSource = tableAttrs.data || []
      const rowSelection = selectionColumn.value ? {
        selectedRowKeys: selectedKeys.value,
        preserveSelectedRowKeys: Boolean(selectionColumn.value.reserveSelection),
        columnWidth: selectionColumn.value.width || selectionColumn.value.minWidth,
        getCheckboxProps: selectionColumn.value.selectable
          ? record => ({ disabled: !selectionColumn.value.selectable(record, dataSource.indexOf(record)) })
          : undefined,
        onChange: (keys, rows) => {
          selectedKeys.value = keys
          selectedRows.value = rows
          emit('selection-change', rows)
        }
      } : undefined
      const height = tableAttrs.height || tableAttrs.maxHeight
      const emptyState = tableAttrs.emptyText || h(Empty, {
        image: Empty.PRESENTED_IMAGE_SIMPLE,
        description: '暂无数据'
      })
      return h(Table, {
        ...without(tableAttrs, [
          'data', 'stripe', 'highlightCurrentRow', 'currentRowKey', 'rowKey', 'height', 'maxHeight',
          'defaultSort', 'defaultExpandAll', 'showHeader', 'fit'
        ]),
        dataSource,
        columns,
        rowKey: keyFor,
        rowSelection,
        pagination: false,
        bordered: tableAttrs.border,
        locale: { emptyText: emptyState },
        size: legacySize(tableAttrs.size),
        showHeader: tableAttrs.showHeader !== false,
        scroll: height ? { y: height, x: 'max-content' } : { x: 'max-content' },
        class: ['el-table', tableAttrs.stripe && 'el-table--striped', tableAttrs.class],
        customHeaderRow: tableAttrs.headerRowClassName
          ? (_columns, index) => ({
              class: typeof tableAttrs.headerRowClassName === 'function'
                ? tableAttrs.headerRowClassName({ row: _columns, rowIndex: index })
                : tableAttrs.headerRowClassName
            })
          : undefined,
        customRow: (record, index) => ({
          onClick: event => {
            emit('row-click', record, undefined, event)
            emit('current-change', record)
          },
          onDblclick: event => emit('row-dblclick', record, undefined, event),
            class: tableAttrs.highlightCurrentRow && keyFor(record, index) === tableAttrs.currentRowKey ? 'current-row' : undefined
        }),
        onChange: (_pagination, _filters, sorter) => emit('sort-change', {
          column: sorter.column,
          prop: Array.isArray(sorter.field) ? sorter.field.join('.') : sorter.field,
          order: sorter.order === 'ascend' ? 'ascending' : (sorter.order === 'descend' ? 'descending' : null)
        }),
        onExpand: (expanded, record) => emit('expand-change', record, expanded)
      })
    }
  }
})

const ElPagination = defineComponent({
  name: 'ElPagination',
  inheritAttrs: false,
  props: {
    currentPage: { type: Number, default: 1 },
    pageSize: { type: Number, default: 10 },
    pageSizes: Array,
    total: { type: Number, default: 0 },
    layout: { type: String, default: 'prev, pager, next' },
    small: Boolean
  },
  emits: ['update:currentPage', 'update:pageSize', 'current-change', 'size-change'],
  setup(props, { attrs, emit }) {
    return () => h(Pagination, {
      ...attrs,
      current: props.currentPage,
      pageSize: props.pageSize,
      pageSizeOptions: props.pageSizes?.map(String),
      total: props.total,
      size: props.small ? 'small' : undefined,
      showSizeChanger: props.layout.includes('sizes'),
      showQuickJumper: props.layout.includes('jumper'),
      showTotal: props.layout.includes('total') ? total => `共 ${total} 条` : undefined,
      responsive: true,
      showTitle: true,
      class: ['el-pagination', attrs.class],
      onChange: (current, size) => {
        emit('update:currentPage', current)
        emit('current-change', current)
        if (size !== props.pageSize) {
          emit('update:pageSize', size)
          emit('size-change', size)
        }
      },
      onShowSizeChange: (_current, size) => {
        emit('update:pageSize', size)
        emit('size-change', size)
      }
    }, {
      itemRender: ({ type, originalElement }) => {
        if (type === 'prev') return h('span', { class: 'ant-compat-pagination-direction' }, '上一页')
        if (type === 'next') return h('span', { class: 'ant-compat-pagination-direction' }, '下一页')
        return originalElement
      }
    })
  }
})

const ElTabs = defineComponent({
  name: 'ElTabs',
  inheritAttrs: false,
  props: {
    modelValue: { default: undefined },
    value: { default: undefined }
  },
  emits: ['update:modelValue', 'input', 'tab-click', 'tab-remove', 'tab-add', 'edit'],
  setup(props, { attrs, slots, emit }) {
    return () => h(Tabs, {
      ...without(attrs, ['editable', 'stretch']),
      type: attrs.editable ? 'editable-card' : (attrs.type === 'border-card' ? 'card' : attrs.type),
      activeKey: inputValue(props),
      class: ['el-tabs', attrs.class],
      'onUpdate:activeKey': value => {
        emit('update:modelValue', value)
        emit('input', value)
      },
      onTabClick: (key, event) => emit('tab-click', { name: key }, event),
      onEdit: (key, action) => {
        emit(action === 'add' ? 'tab-add' : 'tab-remove', key)
        emit('edit', key, action)
      }
    }, {
      default: () => flatten(slots.default ? slots.default() : []).map((node, index) => {
        const paneProps = normalizeAttrs(node.props || {})
        const paneSlots = legacySlots(node.children || {})
        return h(Tabs.TabPane, {
          ...without(paneProps, ['name', 'label', 'lazy']),
          key: paneProps.name ?? String(index),
          tab: paneSlots.label ? paneSlots.label() : paneProps.label,
          forceRender: paneProps.lazy === false
        }, { default: paneSlots.default })
      })
    })
  }
})

const ElTabPane = defineComponent({
  name: 'ElTabPane',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => {
      const grouped = legacySlots(slots)
      return h(Tabs.TabPane, {
        ...attrs,
        key: attrs.name,
        tab: grouped.label ? grouped.label() : attrs.label,
        class: ['el-tab-pane', attrs.class]
      }, { default: grouped.default })
    }
  }
})

function dropdownItems(nodes) {
  return flatten(nodes).map((node, index) => {
    const props = node.props || {}
    const command = props.command ?? props.key ?? index
    const key = typeof command === 'string' || typeof command === 'number'
      ? command
      : `legacy-dropdown-item-${index}`
    return {
      command,
      onClick: props.onClick,
      menuItem: {
        key,
        disabled: props.disabled,
        danger: props.danger,
        icon: props.icon ? () => h(AntIcon, { name: props.icon }) : undefined,
        label: h('span', {
          class: ['el-dropdown-menu__item', props.divided && 'el-dropdown-menu__item--divided']
        }, node.children?.default ? node.children.default() : [])
      }
    }
  })
}

const ElDropdown = defineComponent({
  name: 'ElDropdown',
  inheritAttrs: false,
  emits: ['command', 'visible-change'],
  setup(_, { attrs, slots, emit }) {
    return () => {
      const grouped = legacySlots(slots)
      const menuNode = grouped.dropdown
        ? flatten(grouped.dropdown())[0]
        : grouped.defaultNodes.find(node => node?.type?.name === 'ElDropdownMenu')
      const itemNodes = menuNode?.children?.default ? menuNode.children.default() : []
      const dropdownOptions = dropdownItems(itemNodes)
      const onMenuClick = ({ key, domEvent }) => {
        const option = dropdownOptions.find(item => String(item.menuItem.key) === String(key))
        option?.onClick?.(domEvent)
        emit('command', option?.command ?? key)
      }
      const trigger = attrs.trigger
        ? (Array.isArray(attrs.trigger) ? attrs.trigger : [attrs.trigger])
        : ['hover', 'click']
      return h(Dropdown, {
        ...attrs,
        trigger,
        class: ['el-dropdown', attrs.class],
        onOpenChange: open => emit('visible-change', open)
      }, {
        default: () => grouped.defaultNodes.filter(node => node?.type?.name !== 'ElDropdownMenu'),
        overlay: () => h(Menu, {
          items: dropdownOptions.map(item => item.menuItem),
          class: ['el-dropdown-menu', menuNode?.props?.class],
          onClick: onMenuClick
        })
      })
    }
  }
})

const ElDropdownMenu = defineComponent({ name: 'ElDropdownMenu', render: () => null })
const ElDropdownItem = defineComponent({ name: 'ElDropdownItem', render: () => null })

function legacyMenuPresentation(nodes) {
  const item = flatten(nodes).find(node => node?.type?.name === 'MenuItem' && node.props)
  if (!item) return null
  return {
    icon: item.props.icon,
    title: item.props.title
  }
}

function legacyMenuIcon(icon) {
  if (!icon) return null
  if (icon.includes('el-icon')) {
    return h(AntIcon, { name: icon, class: [icon, 'sub-el-icon'] })
  }
  return h(resolveComponent('svg-icon'), { iconClass: icon })
}

const ElMenu = defineComponent({
  name: 'ElMenu',
  inheritAttrs: false,
  props: {
    defaultActive: { type: String, default: '' },
    collapse: Boolean
  },
  emits: ['select'],
  setup(props, { attrs, slots, emit }) {
    return () => {
      const menuAttrs = normalizeAttrs(attrs)
      return h(Menu, {
        ...without(menuAttrs, ['backgroundColor', 'textColor', 'activeTextColor', 'uniqueOpened', 'collapseTransition']),
        selectedKeys: props.defaultActive ? [props.defaultActive] : [],
        inlineCollapsed: props.collapse,
        mode: menuAttrs.mode === 'vertical' ? 'inline' : menuAttrs.mode,
        class: ['el-menu', menuAttrs.class],
        style: [{ backgroundColor: menuAttrs.backgroundColor, color: menuAttrs.textColor }, menuAttrs.style],
        onClick: ({ key, keyPath, domEvent }) => emit('select', key, keyPath, domEvent)
      }, slots)
    }
  }
})

const ElMenuItem = defineComponent({
  name: 'ElMenuItem',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => {
      const presentation = legacyMenuPresentation(slots.default ? slots.default() : [])
      const icon = legacyMenuIcon(presentation?.icon)
      return h(Menu.Item, {
        ...attrs,
        key: attrs.index,
        title: attrs.title || presentation?.title,
        class: ['el-menu-item', attrs.class]
      }, {
        default: presentation ? () => presentation.title : slots.default,
        icon: icon ? () => icon : slots.icon
      })
    }
  }
})

const ElSubmenu = defineComponent({
  name: 'ElSubmenu',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => {
      const presentation = legacyMenuPresentation(slots.title ? slots.title() : [])
      const icon = legacyMenuIcon(presentation?.icon)
      return h(Menu.SubMenu, {
        ...attrs,
        key: attrs.index,
        class: ['el-submenu', attrs.class]
      }, {
        ...slots,
        title: presentation ? () => presentation.title : slots.title,
        icon: icon ? () => icon : slots.icon
      })
    }
  }
})

const ElPopover = defineComponent({
  name: 'ElPopover',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => {
      const grouped = legacySlots(slots)
      return h(Popover, {
        ...attrs,
        title: attrs.title,
        trigger: attrs.trigger,
        placement: attrs.placement,
        class: ['el-popover', attrs.class]
      }, {
        default: grouped.reference || grouped.default,
        content: grouped.reference ? grouped.default : grouped.content
      })
    }
  }
})

const ElUpload = defineComponent({
  name: 'ElUpload',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => {
      const uploadAttrs = normalizeAttrs(attrs)
      const grouped = legacySlots(slots)
      return h('div', { class: ['el-upload', uploadAttrs.class], style: uploadAttrs.style }, [
        h(Upload.Dragger, {
          ...without(uploadAttrs, ['autoUpload', 'onSuccess', 'onError', 'onChange', 'class', 'style']),
          beforeUpload: uploadAttrs.autoUpload === false ? () => false : uploadAttrs.beforeUpload,
          onChange: info => {
            uploadAttrs.onChange?.(info.file, info.fileList)
            if (info.file.status === 'done') uploadAttrs.onSuccess?.(info.file.response, info.file, info.fileList)
            if (info.file.status === 'error') uploadAttrs.onError?.(info.file.error, info.file, info.fileList)
          }
        }, { default: grouped.default }),
        grouped.tip ? grouped.tip() : null
      ])
    }
  }
})

const ElImage = defineComponent({
  name: 'ElImage',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const failed = ref(false)
    return () => {
      const imageAttrs = normalizeAttrs(attrs)
      const grouped = legacySlots(slots)
      return failed.value && grouped.error
        ? h('div', { class: 'el-image__error' }, grouped.error())
        : h(Image, { ...imageAttrs, preview: imageAttrs.previewSrcList ? { src: imageAttrs.previewSrcList[0] } : imageAttrs.preview, class: ['el-image', imageAttrs.class], onError: () => { failed.value = true } }, slots)
    }
  }
})

const ElScrollbar = defineComponent({
  name: 'ElScrollbar',
  inheritAttrs: false,
  render() {
    const attrs = normalizeAttrs(this.$attrs)
    return h('div', { class: ['el-scrollbar', attrs.class], style: attrs.style }, [
      h('div', {
        ref: 'wrap',
        class: ['el-scrollbar__wrap', attrs.wrapClass],
        style: { height: '100%', overflow: 'auto' }
      }, this.$slots.default?.())
    ])
  }
})

function simpleComponent(name, component, className, transform = attrs => attrs) {
  return defineComponent({
    name,
    inheritAttrs: false,
    setup(_, { attrs, slots }) {
      return () => {
        const normalizedAttrs = normalizeAttrs(attrs)
        return h(component, { ...transform(normalizedAttrs), class: [className, normalizedAttrs.class] }, slots)
      }
    }
  })
}

const ElTag = defineComponent({
  name: 'ElTag',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(Tag, {
      ...attrs,
      color: { success: 'green', warning: 'orange', danger: 'red', info: 'default' }[attrs.type],
      bordered: attrs.effect !== 'dark',
      class: ['el-tag', attrs.type && `el-tag--${attrs.type}`, attrs.class]
    }, slots)
  }
})
const ElDivider = simpleComponent('ElDivider', Divider, 'el-divider', attrs => ({ ...attrs, orientation: attrs.contentPosition }))
const ElAlert = simpleComponent('ElAlert', Alert, 'el-alert', attrs => ({ ...attrs, message: attrs.title, description: attrs.description }))
const ElProgress = simpleComponent('ElProgress', Progress, 'el-progress', attrs => ({ ...attrs, showInfo: attrs.showText !== false, strokeWidth: attrs.strokeWidth }))
const ElTooltip = simpleComponent('ElTooltip', Tooltip, 'el-tooltip', attrs => ({ ...attrs, title: attrs.content }))
const ElEmpty = simpleComponent('ElEmpty', Empty, 'el-empty')
const ElCard = simpleComponent('ElCard', Card, 'el-card')
const ElRow = simpleComponent('ElRow', Row, 'el-row')
const ElCol = simpleComponent('ElCol', Col, 'el-col')
const ElBreadcrumb = simpleComponent('ElBreadcrumb', Breadcrumb, 'el-breadcrumb')
const ElBreadcrumbItem = simpleComponent('ElBreadcrumbItem', Breadcrumb.Item, 'el-breadcrumb__item')
ElBreadcrumbItem.__ANT_BREADCRUMB_ITEM = true
const ElDescriptions = simpleComponent('ElDescriptions', Descriptions, 'el-descriptions', attrs => ({
  ...without(attrs, ['border']),
  bordered: attrs.border !== undefined && attrs.border !== false
}))
const ElDescriptionsItem = simpleComponent('ElDescriptionsItem', Descriptions.Item, 'el-descriptions-item')
const ElPageHeader = simpleComponent('ElPageHeader', PageHeader, 'el-page-header', attrs => ({ ...attrs, subTitle: attrs.content }))
const ElButtonGroup = simpleComponent('ElButtonGroup', Button.Group, 'el-button-group')
const ElContainer = simpleComponent('ElContainer', 'div', 'el-container')
const ElMain = simpleComponent('ElMain', 'main', 'el-main')

function messageOptions(value, type) {
  const source = typeof value === 'object' ? value : { message: value }
  const duration = source.duration ? (source.duration > 20 ? source.duration / 1000 : source.duration) : undefined
  return { type: type || source.type || 'info', content: source.message ?? source.content ?? '', duration, onClose: source.onClose }
}

function showMessage(value) {
  return message.open(messageOptions(value))
}
['success', 'error', 'warning', 'info', 'loading'].forEach(type => {
  showMessage[type] = value => message.open(messageOptions(value, type))
})
showMessage.closeAll = () => message.destroy()

function confirm(messageText, title = dialogText.defaultTitle, options = {}) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title,
      content: messageText,
      okText: options.confirmButtonText || dialogText.okText,
      cancelText: options.cancelButtonText || dialogText.cancelText,
      okType: options.type === 'warning' ? 'primary' : options.confirmButtonType,
      centered: true,
      onOk: () => resolve(true),
      onCancel: () => reject(new Error(dialogText.cancelText))
    })
  })
}

async function copyText(value) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(String(value))
  }
  const input = document.createElement('textarea')
  input.value = String(value)
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  input.remove()
}

const loadingKey = Symbol('loading')
function updateLoading(el, active, options = {}) {
  if (!el) return
  const fullscreen = el === document.body || el === document.documentElement
  if (!el[loadingKey]) {
    const overlay = document.createElement('div')
    overlay.className = 'ant-compat-loading'
    overlay.setAttribute('role', 'status')
    overlay.setAttribute('aria-live', 'polite')
    el.appendChild(overlay)
    el[loadingKey] = overlay
    if (!fullscreen && getComputedStyle(el).position === 'static') el.style.position = 'relative'
  }

  const overlay = el[loadingKey]
  const text = options.text || el.getAttribute('element-loading-text') || '加载中'
  const background = options.background ?? el.getAttribute('element-loading-background')
  overlay.classList.toggle('ant-compat-loading--fullscreen', fullscreen)
  overlay.setAttribute('aria-hidden', active ? 'false' : 'true')
  if (overlay.dataset.loadingText !== text) {
    render(h(Spin, { size: 'large', tip: text }), overlay)
    overlay.dataset.loadingText = text
  }
  if (background) {
    overlay.style.background = background
  } else {
    overlay.style.removeProperty('background')
  }
  overlay.style.display = active ? 'flex' : 'none'
  if (fullscreen) document.body.classList.toggle('ant-compat-loading-locked', Boolean(active && options.lock))
}

const loadingDirective = {
  mounted: (el, binding) => updateLoading(el, binding.value),
  updated: (el, binding) => updateLoading(el, binding.value),
  unmounted(el) {
    if (el[loadingKey]) {
      render(null, el[loadingKey])
      el[loadingKey].remove()
      delete el[loadingKey]
    }
  }
}

const clipboardKey = Symbol('clipboard')
const clipboardBindingKey = Symbol('clipboard-binding')
const clipboardDirective = {
  mounted(el, binding, vnode) {
    el[clipboardBindingKey] = {
      value: binding.value,
      onSuccess: vnode.props?.onSuccess,
      onError: vnode.props?.onError
    }
    el[clipboardKey] = async() => {
      const current = el[clipboardBindingKey]
      try {
        await copyText(current.value)
        current.onSuccess?.(current.value)
      } catch (error) {
        current.onError?.(error)
      }
    }
    el.addEventListener('click', el[clipboardKey])
  },
  updated(el, binding, vnode) {
    el[clipboardBindingKey] = {
      value: binding.value,
      onSuccess: vnode.props?.onSuccess,
      onError: vnode.props?.onError
    }
  },
  unmounted(el) {
    el.removeEventListener('click', el[clipboardKey])
  }
}

const infiniteKey = Symbol('infinite-scroll')
const infiniteScrollDirective = {
  mounted(el, binding) {
    const handler = () => {
      if (el.scrollHeight - el.scrollTop - el.clientHeight < 40) binding.value?.()
    }
    el[infiniteKey] = handler
    el.addEventListener('scroll', handler, { passive: true })
  },
  unmounted(el) {
    el.removeEventListener('scroll', el[infiniteKey])
  }
}

let contextMenuContainer
let closeContextMenu
function showContextMenu({ items = [], event, zIndex = 3000 }) {
  closeContextMenu?.()
  const container = document.createElement('div')
  contextMenuContainer = container
  container.className = 'ant-compat-context-menu'
  Object.assign(container.style, {
    left: `${event.clientX}px`,
    top: `${event.clientY}px`,
    zIndex: String(zIndex)
  })
  const close = () => {
    render(null, container)
    container.remove()
    if (contextMenuContainer === container) contextMenuContainer = null
    if (closeContextMenu === close) closeContextMenu = null
    document.removeEventListener('pointerdown', onPointerDown)
  }
  const onPointerDown = pointerEvent => {
    if (!container.contains(pointerEvent.target)) close()
  }
  closeContextMenu = close
  const vnode = h(Menu, { selectable: false }, {
    default: () => items.map((item, index) => h(Menu.Item, {
      key: index,
      disabled: item.disabled,
      danger: item.icon === 'el-icon-delete',
      class: item.divided && 'ant-compat-menu-divider',
      onClick: () => {
        item.onClick?.()
        close()
      }
    }, {
      icon: item.icon ? () => h(AntIcon, { name: item.icon }) : undefined,
      default: () => item.label
    }))
  })
  render(vnode, container)
  document.body.appendChild(container)
  nextTick(() => document.addEventListener('pointerdown', onPointerDown))
}

const components = {
  AntIcon,
  VueEasyTree,
  ElAlert,
  ElAutocomplete,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElButtonGroup,
  ElCard,
  ElCheckbox,
  ElCol,
  ElContainer,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDivider,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElOptionGroup,
  ElPageHeader,
  ElPagination,
  ElPopover,
  ElProgress,
  ElRadio,
  ElRadioButton,
  ElRadioGroup,
  ElRow,
  ElScrollbar,
  ElSelect,
  ElSlider,
  ElSubmenu,
  ElSwitch,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
  ElTooltip,
  ElUpload
}

export { confirm as MessageBoxConfirm, showMessage as Message }

export default {
  install(app) {
    Object.entries(components).forEach(([name, component]) => app.component(name, component))
    app.directive('loading', loadingDirective)
    app.directive('clipboard', clipboardDirective)
    app.directive('infinite-scroll', infiniteScrollDirective)
    app.directive('el-drag-dialog', dragDialog)
    app.config.globalProperties.$message = showMessage
    app.config.globalProperties.$confirm = confirm
    app.config.globalProperties.$copyText = copyText
    app.config.globalProperties.$contextmenu = showContextMenu
    app.config.globalProperties.$loading = options => {
      const targetOption = options?.target
      const target = (typeof targetOption === 'string' ? document.querySelector(targetOption) : targetOption) || document.body
      updateLoading(target, true, options)
      return { close: () => updateLoading(target, false, options) }
    }
  }
}

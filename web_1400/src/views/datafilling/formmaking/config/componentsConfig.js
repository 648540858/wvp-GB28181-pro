// 表单组件
export const formComponents = [
  {
    type: 'input', // 表单类型
    label: '输入框', // 标题文字
    icon: 'icon-input', // 图标
    options: {
      type: 'text', // 文本或密码，text或者password
      width: '100%', // 宽度
      defaultValue: '', // 默认值
      placeholder: '', // 没有输入时，提示文字
      maxLength: null, // 最大长度
      clearable: false, // 是否可清除，false否，true是
      hidden: false, // 是否隐藏，false显示，true隐藏
      disabled: false // 是否禁用，false不禁用，true禁用
    },
    model: '', // 字段标识
    key: '',
    rules: [
      // 验证规则
      {
        required: false, // 必须填写
        message: '必填项'
      }
    ]
  },
  {
    type: 'textarea',
    label: '文本框',
    icon: 'icon-edit',
    options: {
      width: '100%',
      defaultValue: '',
      placeholder: '',
      minRows: 4, // 最小行数
      maxRows: 6, // 最大行数
      maxLength: null,
      hidden: false,
      disabled: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'number',
    label: '数字输入框',
    icon: 'icon-number',
    options: {
      width: '100%',
      defaultValue: 0,
      placeholder: '',
      min: null,
      max: null,
      precision: null,
      step: 1,
      hidden: false,
      disabled: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'select',
    label: '下拉选择器',
    icon: 'icon-xiala',
    options: {
      width: '100%',
      defaultValue: '',
      multiple: false,
      disabled: false,
      clearable: false,
      hidden: false,
      placeholder: '',
      dynamic: false,
      dynamicKey: '',
      dynamicFunc: '',
      options: [
        {
          value: '1',
          label: '下拉框1'
        },
        {
          value: '2',
          label: '下拉框2'
        }
      ]
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'checkbox',
    label: '多选框',
    icon: 'icon-duoxuan1',
    options: {
      disabled: false,
      hidden: false,
      defaultValue: [],
      inline: false,
      dynamic: false,
      dynamicKey: '',
      dynamicFunc: '',
      options: [
        {
          value: '1',
          label: '选项1'
        },
        {
          value: '2',
          label: '选项2'
        },
        {
          value: '3',
          label: '选项3'
        }
      ]
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'radio',
    label: '单选框',
    icon: 'icon-danxuan-cuxiantiao',
    options: {
      disabled: false,
      hidden: false,
      defaultValue: '',
      inline: false,
      dynamic: false,
      dynamicKey: '',
      dynamicFunc: '',
      options: [
        {
          value: '1',
          label: '选项1'
        },
        {
          value: '2',
          label: '选项2'
        },
        {
          value: '3',
          label: '选项3'
        }
      ]
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'date',
    label: '日期选择框',
    icon: 'icon-calendar',
    options: {
      width: '100%',
      defaultValue: '',
      placeholder: '',
      hidden: false,
      readonly: false,
      disabled: false,
      editable: true,
      clearable: true,
      startPlaceholder: '',
      endPlaceholder: '',
      type: 'date',
      format: 'yyyy-MM-dd',
      timestamp: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'time',
    label: '时间选择框',
    icon: 'icon-time',
    options: {
      width: '100%',
      defaultValue: '',
      placeholder: '',
      hidden: false,
      readonly: false,
      disabled: false,
      editable: true,
      clearable: true,
      isRange: false,
      startPlaceholder: '',
      endPlaceholder: '',
      arrowControl: true,
      format: 'HH:mm:ss'
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'rate',
    label: '评分',
    icon: 'icon-pingfen_moren',
    options: {
      defaultValue: 0,
      max: 5,
      disabled: false,
      hidden: false,
      allowHalf: false,
      showScore: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'slider',
    label: '滑动输入条',
    icon: 'icon-menu',
    options: {
      width: '100%',
      defaultValue: 0,
      disabled: false,
      hidden: false,
      min: 0,
      max: 100,
      step: 1,
      showInput: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'color',
    label: '颜色选择器',
    icon: 'icon-pingfen_moren',
    options: {
      defaultValue: '',
      disabled: false,
      hidden: false,
      showAlpha: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  // {
  //   type: 'uploadFile',
  //   label: '上传文件',
  //   icon: 'icon-upload',
  //   options: {
  //     defaultValue: [],
  //     disabled: false,
  //     hidden: false,
  //     multiple: false,
  //     width: '100%',
  //     limit: 3,
  //     data: '{}',
  //     fileName: 'file',
  //     headers: {},
  //     action: 'http://cdn.kcz66.com/uploadFile.txt',
  //     placeholder: '上传'
  //   },
  //   model: '',
  //   key: '',
  //   rules: [
  //     {
  //       required: false,
  //       message: '必填项'
  //     }
  //   ]
  // },
  // {
  //   type: 'uploadImg',
  //   label: '上传图片',
  //   icon: 'icon-image',
  //   options: {
  //     defaultValue: [],
  //     hidden: false,
  //     disabled: false,
  //     multiple: false,
  //     width: '100%',
  //     data: '{}',
  //     limit: 3,
  //     placeholder: '上传',
  //     fileName: 'image',
  //     headers: {},
  //     action: 'http://cdn.kcz66.com/upload-img.txt',
  //     listType: 'picture-card'
  //   },
  //   model: '',
  //   key: '',
  //   rules: [
  //     {
  //       required: false,
  //       message: '必填项'
  //     }
  //   ]
  // },
  {
    type: 'cascader',
    label: '级联选择器',
    icon: 'icon-guanlian',
    options: {
      disabled: false,
      hidden: false,
      defaultValue: [],
      placeholder: '',
      width: '100%',
      clearable: false,
      dynamic: false,
      dynamicKey: '',
      dynamicFunc: '',
      options: [
        {
          value: '1',
          label: '选项1',
          children: [
            {
              value: '11',
              label: '选项11'
            }
          ]
        },
        {
          value: '2',
          label: '选项2',
          children: [
            {
              value: '22',
              label: '选项22'
            }
          ]
        }
      ],
      props: {
        value: 'value',
        label: 'label',
        children: 'children'
      }
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'subform',
    label: '子表单',
    icon: 'icon-biaoge',
    list: [],
    options: {
      scrollY: 0,
      disabled: false,
      hidden: false,
      showLabel: false,
      hideSequence: false,
      width: '100%'
    },
    model: '',
    key: ''
  },
  {
    type: 'editor',
    label: '富文本',
    icon: 'icon-LC_icon_edit_line_1',
    options: {
      height: 300,
      placeholder: '',
      defaultValue: '',
      hidden: false,
      disabled: false,
      width: '100%'
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'switch',
    label: '开关',
    icon: 'icon-kaiguan3',
    options: {
      defaultValue: false,
      hidden: false,
      disabled: false
    },
    model: '',
    key: '',
    rules: [
      {
        required: false,
        message: '必填项'
      }
    ]
  },
  {
    type: 'alert',
    label: '警告提示',
    icon: 'icon-zu',
    options: {
      type: 'success',
      title: '提示的文案',
      description: '',
      effect: '',
      hidden: false,
      showIcon: false,
      closable: false,
      center: false
    },
    model: '',
    key: ''
  },
  {
    type: 'text',
    label: '文字',
    icon: 'icon-zihao',
    options: {
      hidden: false,
      textAlign: 'left',
      defaultValue: 'This is a text'
    },
    model: '',
    key: ''
  },
  {
    type: 'html',
    label: 'HTML',
    icon: 'icon-ai-code',
    options: {
      hidden: false,
      defaultValue: '<strong>This is a HTML</strong>'
    },
    model: '',
    key: ''
  }
]
// 布局组件
export const layoutComponents = [
  {
    type: 'divider',
    label: '分割线',
    icon: 'icon-fengexian',
    options: {
      hidden: false,
      orientation: 'left'
    },
    key: '',
    model: ''
  },
  {
    type: 'grid',
    label: '栅格布局',
    icon: 'icon-zhage',
    columns: [
      {
        span: 12,
        list: []
      },
      {
        span: 12,
        list: []
      }
    ],
    options: {
      gutter: 0
    },
    key: '',
    model: ''
  },
  {
    type: 'table',
    label: '表格布局',
    icon: 'icon-biaoge',
    trs: [
      {
        tds: [
          {
            colspan: 1,
            rowspan: 1,
            list: []
          },
          {
            colspan: 1,
            rowspan: 1,
            list: []
          }
        ]
      },
      {
        tds: [
          {
            colspan: 1,
            rowspan: 1,
            list: []
          },
          {
            colspan: 1,
            rowspan: 1,
            list: []
          }
        ]
      }
    ],
    options: {
      width: '100%',
      bordered: true,
      bright: false,
      small: true,
      customStyle: ''
    },
    key: '',
    model: ''
  },
  {
    type: 'tabs',
    label: '标签页布局',
    icon: 'icon-tabs',
    options: {
      type: 'border-card',
      tabPosition: 'top'
    },
    tabs: [
      {
        name: '1',
        label: '选项1',
        list: []
      },
      {
        name: '2',
        label: '选项2',
        list: []
      }
    ],
    key: '',
    model: ''
  },
  {
    type: 'card',
    label: '卡片布局',
    icon: 'icon-qiapian',
    list: [],
    key: '',
    model: ''
  }
]

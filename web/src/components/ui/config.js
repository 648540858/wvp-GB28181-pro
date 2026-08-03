import zhCN from 'ant-design-vue/es/locale/zh_CN'

export const dialogText = Object.freeze({
  defaultTitle: '提示',
  okText: '确定',
  cancelText: '取消',
  justOkText: '知道了',
  closeText: '关闭'
})

export const locale = Object.freeze({
  ...zhCN,
  locale: 'zh-cn',
  Modal: {
    ...zhCN.Modal,
    okText: dialogText.okText,
    cancelText: dialogText.cancelText,
    justOkText: dialogText.justOkText
  },
  Popconfirm: {
    ...zhCN.Popconfirm,
    okText: dialogText.okText,
    cancelText: dialogText.cancelText
  }
})

export const themeConfig = {
  token: {
    colorPrimary: '#2563eb',
    colorPrimaryHover: '#3b82f6',
    colorPrimaryActive: '#1d4ed8',
    colorPrimaryBg: '#eff6ff',
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorInfo: '#2563eb',
    colorLink: '#2563eb',
    colorBgLayout: '#f5f7fa',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBgSpotlight: '#1f2937',
    colorFillAlter: '#f8fafc',
    colorFillSecondary: '#f3f4f6',
    colorText: '#1f2937',
    colorTextSecondary: '#6b7280',
    colorTextTertiary: '#9ca3af',
    colorTextDisabled: '#9ca3af',
    colorBorder: '#dfe3e8',
    colorBorderSecondary: '#edf0f3',
    colorSplit: '#e5e7eb',
    controlItemBgHover: '#eff6ff',
    controlItemBgActive: '#dbeafe',
    borderRadius: 6,
    borderRadiusLG: 8,
    controlHeight: 36,
    controlHeightSM: 30,
    controlHeightLG: 44,
    fontSize: 14,
    lineHeight: 1.5715,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", Arial, sans-serif',
    boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)',
    boxShadowSecondary: '0 12px 32px rgba(15, 23, 42, 0.14)',
    controlOutline: 'rgba(37, 99, 235, 0.14)',
    motionDurationMid: '0.25s'
  },
  components: {
    Alert: {
      borderRadiusLG: 6
    },
    Button: {
      fontWeight: 500,
      primaryShadow: 'none',
      defaultShadow: 'none'
    },
    Card: {
      bodyPadding: 20,
      headerHeight: 48,
      headerFontSize: 15
    },
    Descriptions: {
      labelBg: '#f8fafc',
      titleMarginBottom: 16
    },
    Dropdown: {
      controlItemBgHover: '#eff6ff'
    },
    Form: {
      itemMarginBottom: 18,
      labelColor: '#1f2937',
      labelFontSize: 14,
      verticalLabelPadding: '0 0 6px'
    },
    Input: {
      activeShadow: '0 0 0 2px rgba(37, 99, 235, 0.12)'
    },
    InputNumber: {
      activeShadow: '0 0 0 2px rgba(37, 99, 235, 0.12)'
    },
    Menu: {
      itemHeight: 48,
      itemBorderRadius: 6,
      itemMarginInline: 8,
      iconSize: 18
    },
    Modal: {
      titleFontSize: 16,
      titleLineHeight: 1.5
    },
    Pagination: {
      itemActiveBg: '#eff6ff',
      itemBg: '#ffffff'
    },
    Select: {
      activeOutlineColor: 'rgba(37, 99, 235, 0.12)',
      optionActiveBg: '#eff6ff',
      optionSelectedBg: '#dbeafe'
    },
    Table: {
      headerBg: '#f8fafc',
      headerColor: '#374151',
      headerSplitColor: '#e5e7eb',
      headerSortActiveBg: '#f1f5f9',
      headerSortHoverBg: '#f1f5f9',
      rowHoverBg: '#f6f9ff',
      borderColor: '#e5e7eb',
      cellPaddingBlock: 14,
      cellPaddingInline: 16
    },
    Tabs: {
      itemSelectedColor: '#2563eb',
      itemHoverColor: '#3b82f6',
      inkBarColor: '#2563eb',
      titleFontSize: 14
    },
    Tooltip: {
      borderRadius: 6
    },
    Tree: {
      nodeHoverBg: '#eff6ff',
      nodeSelectedBg: '#dbeafe'
    }
  }
}

export const formConfig = {
  validateMessages: {
    default: '${label}校验失败',
    required: '请输入或选择${label}',
    enum: '${label}必须是其中一个有效选项',
    whitespace: '${label}不能为空',
    date: {
      format: '${label}日期格式错误',
      parse: '${label}无法转换为日期',
      invalid: '${label}不是有效日期'
    },
    types: {
      email: '${label}邮箱格式错误',
      number: '${label}必须是数字',
      integer: '${label}必须是整数',
      url: '${label}地址格式错误'
    }
  }
}

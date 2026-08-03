import SvgIcon from '@/components/SvgIcon'// svg component
import { registerSvgSprite } from './sprite'

const svgModules = import.meta.glob('./svg/*.svg', {
  eager: true,
  query: '?raw',
  import: 'default'
})

export function registerIcons(app) {
  app.component('svg-icon', SvgIcon)
  registerSvgSprite(svgModules)
}

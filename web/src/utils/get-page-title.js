import defaultSettings from '@/settings'

const title = defaultSettings.title || 'WVP视频平台'

export default function getPageTitle(pageTitle) {
  if (pageTitle) {
    return `${pageTitle} - ${title}`
  }
  return `${title}`
}

const cleanupKey = Symbol('drag-dialog-cleanup')

function setupDrag(el, binding) {
  window.setTimeout(() => {
    const dialogs = Array.from(document.querySelectorAll('.el-dialog-wrapper .ant-modal'))
    const dialog = el.querySelector?.('.ant-modal') || dialogs.reverse().find(item => item.offsetParent)
    const header = dialog?.querySelector('.ant-modal-header')
    if (!dialog || !header || header.dataset.dragReady) return

    header.dataset.dragReady = 'true'
    header.style.cursor = 'move'

    const onMouseDown = event => {
      if (event.button !== 0) return
      const rect = dialog.getBoundingClientRect()
      const startX = event.clientX
      const startY = event.clientY

      dialog.style.position = 'fixed'
      dialog.style.left = `${rect.left}px`
      dialog.style.top = `${rect.top}px`
      dialog.style.margin = '0'

      const onMouseMove = moveEvent => {
        const maxLeft = Math.max(0, window.innerWidth - rect.width)
        const maxTop = Math.max(0, window.innerHeight - rect.height)
        const left = Math.min(maxLeft, Math.max(0, rect.left + moveEvent.clientX - startX))
        const top = Math.min(maxTop, Math.max(0, rect.top + moveEvent.clientY - startY))
        dialog.style.left = `${left}px`
        dialog.style.top = `${top}px`
        binding.instance?.$emit?.('dragDialog')
      }

      const onMouseUp = () => {
        document.removeEventListener('mousemove', onMouseMove)
        document.removeEventListener('mouseup', onMouseUp)
      }

      document.addEventListener('mousemove', onMouseMove)
      document.addEventListener('mouseup', onMouseUp)
    }

    header.addEventListener('mousedown', onMouseDown)
    el[cleanupKey] = () => header.removeEventListener('mousedown', onMouseDown)
  })
}

export default {
  mounted: setupDrag,
  updated: setupDrag,
  beforeUnmount(el) {
    el[cleanupKey]?.()
  }
}

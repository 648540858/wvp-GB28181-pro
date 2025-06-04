// 压缩图片
export function compressImg(file, config) {
  // let fileSize = parseFloat(parseInt(file['size']) / 1024 / 1024).toFixed(2)
  const read = new FileReader()
  read.readAsDataURL(file)
  return new Promise(function(resolve, reject) {
    read.onload = function(e) {
      const img = new Image()
      img.src = e.target.result
      img.onload = function() {
        // 默认按比例压缩
        const w = config.width
        const h = config.height
        // 生成canvas
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        let base64
        // 创建属性节点
        canvas.setAttribute('width', w)
        canvas.setAttribute('height', h)
        ctx.drawImage(this, 0, 0, w, h)
        // eslint-disable-next-line prefer-const
        base64 = canvas.toDataURL(file['type'], config.quality)
        // 回调函数返回file的值（将base64编码转成file）
        // const files = dataURLtoFile(base64) // 如果后台接收类型为base64的话这一步可以省略
        // 回调函数返回file的值（将base64转为二进制）
        // const fileBinary = dataURLtoBlob(base64)
        resolve(base64)
      }
    }
  })
}

// 将base64转为二进制
export function dataURLtoBlob(dataurl) {
  const arr = dataurl.split(',')
  const mime = arr[0].match(/:(.*?);/)[1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new Blob([u8arr], { type: mime })
}

// base64转码（将base64编码转回file文件）
export function dataURLtoFile(dataurl) {
  const arr = dataurl.split(',')
  const mime = arr[0].match(/:(.*?);/)[1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new File([u8arr], { type: mime })
}

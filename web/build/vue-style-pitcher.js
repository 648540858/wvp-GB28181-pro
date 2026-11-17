/**
 * vue-loader 15 + webpack 5 的兼容补丁。
 *
 * vue-loader 的 pitcher 对 style 块只输出：
 *   export * from "<style request>"
 * 但按 ES 规范 `export *` 不会转发 default，而 vue-loader 生成的父模块使用的是：
 *   import style0 from "<style request>"
 * webpack 5 因此会为每个带样式的 .vue 文件打印：
 *   export 'default' (imported as 'style0') was not found ... (possible exports unknown)
 *
 * 这里在模块加载时包装原 pitcher 的 pitch：普通 style 块显式补一个
 * `export default undefined`，让 webpack 可以静态满足父模块的默认导入。
 * 运行时行为不变：普通样式依靠 loader 的副作用注入，default 不会被使用
 * （CSS Modules 场景由原逻辑提供真实的 default，不做改动）。
 *
 * 用法：在 vue.config.js 顶部 require 本文件即可（只做一次副作用补丁，
 * 不修改 node_modules）。
 */
const qs = require("querystring")
const pitcher = require("@vue/vue-loader-v15/lib/loaders/pitcher")

const PATCH_FLAG = "__wvpDefaultReexportPatched"

if (!pitcher[PATCH_FLAG]) {
  const originalPitch = pitcher.pitch

  pitcher.pitch = function (remainingRequest) {
    const code = originalPitch.call(this, remainingRequest)
    const query = qs.parse((this.resourceQuery || "").slice(1))

    // 只处理普通 style 块。CSS Modules（query.module）原逻辑已经带了
    // `export { default }`，它的 default 需要保留真实值，不能覆盖。
    if (query.type === "style" && !query.module && typeof code === "string") {
      const matched = code.trim().match(/^export \* from (.+)$/)
      if (matched) {
        // 普通 style 块的 default 在运行时不会被使用（样式由 loader 的副作用注入），
        // 显式导出 undefined 只是为了让 webpack 能静态满足父模块的默认导入。
        // dev（vue-style-loader）和 prod（mini-css-extract-plugin）都适用。
        return `export * from ${matched[1]}\nexport default undefined`
      }
    }
    return code
  }

  pitcher[PATCH_FLAG] = true
}

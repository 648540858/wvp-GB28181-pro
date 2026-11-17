const path = require("path")
const defaultSettings = require("./src/settings.js")
// 副作用补丁：修复 vue-loader 15 + webpack 5 的 style default 导出告警
require("./build/vue-style-pitcher")

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = defaultSettings.title || "WVP视频平台" // page title

// If your port is set to 80,
// use administrator privileges to execute the command line.
// For example, Mac: sudo npm run
// You can change the port by the following methods:
// port = 9528 npm run dev OR npm run dev --port = 9528
const port = process.env.port || process.env.npm_config_port || 9528 // dev port

// All configuration item explanations can be find in https://cli.vuejs.org/config/
module.exports = {
  /**
   * You will need to set publicPath if you plan to deploy your site under a sub path,
   * for example GitHub Pages. If you plan to deploy your site to https://foo.github.io/bar/,
   * then publicPath should be set to "/bar/".
   * In most cases please use '/' !!!
   * Detail: https://cli.vuejs.org/config/#publicpath
   */
  publicPath: "/",
  outputDir: "../src/main/resources/static",
  assetsDir: "static",
  lintOnSave: false, // Disable ESLint to avoid warnings
  productionSourceMap: false,
  css: {
    loaderOptions: {
      css: {
        url: {
          // 以 /static/ 开头的资源由 public 目录原样提供，交给浏览器解析即可。
          // 否则 css-loader 6 会把它们当成模块，从文件系统根目录解析导致构建失败。
          filter: (url) => !url.startsWith("/static/"),
        },
      },
    },
  },
  // webpack 5 可以直接解析 ES2020+ 语法，ol / maplibre-gl 自带的产物无需再经过 Babel。
  // 尤其是 maplibre-gl：它的 Worker 通过 Function.toString() 内联，若被 Babel 转译，
  // Worker 中会缺少模块顶层的 helper（如 _wrapNativeSuper）而导致地图无法加载。
  transpileDependencies: [],
  devServer: {
    host: "localhost",
    port: port,
    open: true,
    // webpack-dev-server 4 起 overlay 移到了 client 下
    client: {
      overlay: {
        warnings: false,
        errors: false,
        // 不弹出运行时错误遮罩，避免遮住“登录已过期”等业务提示
        runtimeErrors: false,
      },
    },
    // before: require('./mock/mock-server.js'),
    proxy: {
      "/dev-api": {
        target: "http://127.0.0.1:18080",
        changeOrigin: true,
        pathRewrite: {
          "^/dev-api": "/",
        },
      },
      "/static/snap": {
        target: "http://127.0.0.1:18080",
        changeOrigin: true,
        // pathRewrite: {
        //   '^/static/snap': '/static/snap'
        // }
      },
    },
  },
  configureWebpack: {
    // provide the app's title in webpack's name field, so that
    // it can be accessed in index.html to inject the correct title.
    name: name,
    performance: {
      // 项目内置 mermaid(3.2MB) / bg19.png(3MB) / libDecoder.wasm(2.3MB) 等资源，
      // webpack 默认 244KiB 阈值必然告警。这里放大到实际可控范围，
      // 仍保留对资源异常增长的提示。
      maxAssetSize: 4 * 1024 * 1024,
      maxEntrypointSize: 8 * 1024 * 1024,
    },
    resolve: {
      alias: {
        "@": resolve("src"),
      },
    },
  },
  chainWebpack(config) {
    // set svg-sprite-loader
    config.module.rule("svg").exclude.add(resolve("src/icons")).end()
    config.module
      .rule("icons")
      .test(/\.svg$/)
      .include.add(resolve("src/icons"))
      .end()
      .use("svg-sprite-loader")
      .loader("svg-sprite-loader")
      .options({
        symbolId: "icon-[name]",
      })
      .end()

    config.when(process.env.NODE_ENV !== "development", (config) => {
      config.optimization.splitChunks({
        chunks: "all",
        cacheGroups: {
          libs: {
            name: "chunk-libs",
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: "initial", // only package third parties that are initially dependent
          },
          elementUI: {
            name: "chunk-elementUI", // split elementUI into a single package
            priority: 20, // the weight needs to be larger than libs and app or it will be packaged into libs or app
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/, // in order to adapt to cnpm
          },
          commons: {
            name: "chunk-commons",
            test: resolve("src/components"), // can customize your rules
            minChunks: 3, //  minimum common number
            priority: 5,
            reuseExistingChunk: true,
          },
        },
      })
      // https:// webpack.js.org/configuration/optimization/#optimizationruntimechunk
      config.optimization.runtimeChunk("single")
    })
  },
}

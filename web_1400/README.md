## Build Setup

# 进入项目目录
cd web_1400

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装以来，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npmmirror.com

# 启动服务
npm run dev
```

浏览器访问 [http://localhost:8080](http://localhost:8080)

## 发布

```bash

# 构建生产环境
npm run build
```
// https://github.com/michael-ciniawsky/postcss-load-config

module.exports = {
  "plugins": {
    "postcss-import": {},
    "postcss-url": {},
    // to edit target browsers: use "browserslist" field in package.json
    "autoprefixer": {},
    'postcss-pxtorem': {
      rootValue: 24, 
      propList: ['font-size'] // 只转化font-size
        // propList: ['*'], // 转化全部
        // propList: ['*','!border'], //转化全部，除了border属性
        // selectorBlackList: ['body'] // 过滤掉.am-开头的class，不进行rem转换
    }
  }
}

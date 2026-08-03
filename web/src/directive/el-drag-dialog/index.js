import drag from './drag'

const install = function(app) {
  app.directive('el-drag-dialog', drag)
}

drag.install = install
export default drag

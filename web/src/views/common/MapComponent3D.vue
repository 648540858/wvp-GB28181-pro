<template>
  <div ref="mapContainer" style="width: 100%;height: 100%;" />
</template>

<script>
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import gcoord from 'gcoord'

/**
 * 3D地图组件（MapLibre GL 实现）
 * 对外接口与 MapComponent.vue 保持一致，可替换使用：
 * - 数据：mapTileList、mapTileIndex、dragInteraction
 * - 事件：loaded、coordinateSystemChange、zoomChange
 * - 方法：addPointLayer、addVectorTileLayer、openInfoBox 等（见 methods）
 *
 * 与 2D 版的区别：
 * - mapconfig 返回的 tilesUrl 按 MapLibre style JSON 地址处理（三个地址即三套 style）
 * - 支持 3D 俯仰（NavigationControl 的 visualizePitch），style 内含 fill-extrusion 时自动立体渲染
 */

// ---------------------------------------------------------------------------
// 坐标转换：输入输出永远是 WGS84，GCJ02 场景下转换到地图坐标系（与 TransformLonLat 等价，不依赖 ol）
// ---------------------------------------------------------------------------
function fromLonLat(coordinate) {
  if (window.coordinateSystem === 'GCJ02') {
    return gcoord.transform(coordinate, gcoord.WGS84, gcoord.GCJ02)
  }
  return coordinate
}

function toLonLat(coordinate) {
  if (window.coordinateSystem === 'GCJ02') {
    return gcoord.transform(coordinate, gcoord.GCJ02, gcoord.WGS84)
  }
  return coordinate
}

let idCounter = 0
function genId(prefix) {
  idCounter += 1
  return prefix + '-' + idCounter
}

let infoBoxCounter = 0
function genInfoBoxId() {
  infoBoxCounter += 1
  return 'infobox-' + infoBoxCounter
}

// ---------------------------------------------------------------------------
// 3D 版拖拽交互，对外 API 与 map/DragInteraction.js 一致：
// addFeatureId(id, {startEvent, moveEvent, endEvent}) / removeFeatureId(id) / hasFeatureId(id)
// endEvent 回调参数中带 lonLat（WGS84 经纬度数组）
// ---------------------------------------------------------------------------
class DragInteraction3D {
  constructor() {
    this.featureIdMap_ = new Map()
    this.map = null
    this.mapComponent = null
    // 按下时命中的可拖拽要素：{ featureId, descriptor }
    this.dragging_ = null
    // 按下位置相对图标锚点的屏幕偏移：{ x, y }
    // 拖动时把点位放在“光标 - 偏移”处，图标才不会跳到光标位置
    this.dragOffset_ = null
    // 按下位置，用于判断是否超过拖拽阈值
    this.dragStartPoint_ = null
    // 是否已真正开始拖动（超过阈值后才触发 startEvent）
    this.dragActive_ = false
    this.previousCursor_ = undefined
  }
  bind(map, mapComponent) {
    this.map = map
    this.mapComponent = mapComponent

    map.on('mousedown', (e) => {
      if (e.originalEvent && e.originalEvent.button !== 0) {
        return
      }
      if (this.dragging_) {
        return
      }
      const hit = this.findDraggable_(e.point)
      if (!hit) {
        return
      }
      const feature = hit.descriptor.features.get(hit.featureId)
      if (!feature) {
        return
      }
      // 图标锚点（点位）在屏幕上的位置，与按下位置相减得到抓取偏移。
      // 摄像头图标的 icon-anchor 是 bottom，用户点在图标中心时偏移约为 20px；
      // 不记录偏移就会在第一次移动时把图标整体吸到光标处（看起来往上飘）。
      const anchor = map.project(fromLonLat(feature.position))
      this.dragging_ = hit
      this.dragOffset_ = { x: e.point.x - anchor.x, y: e.point.y - anchor.y }
      this.dragStartPoint_ = { x: e.point.x, y: e.point.y }
      this.dragActive_ = false
      // 先禁用地图平移，避免拖动图标时地图跟着平移；
      // startEvent 等到真正移动超过阈值再触发，纯点击不改变编辑框
      map.dragPan.disable()
      map.getCanvas().style.cursor = 'grabbing'
    })

    map.on('mousemove', (e) => {
      const drag = this.dragging_
      if (drag) {
        if (!this.dragActive_) {
          const dx = Math.abs(e.point.x - this.dragStartPoint_.x)
          const dy = Math.abs(e.point.y - this.dragStartPoint_.y)
          if (dx < 3 && dy < 3) {
            return
          }
          this.dragActive_ = true
          const startCallback = this.featureIdMap_.get(drag.featureId)
          if (startCallback && typeof startCallback.startEvent === 'function') {
            startCallback.startEvent(e)
          }
        }
        const descriptor = drag.descriptor
        const feature = descriptor.features.get(drag.featureId)
        if (!feature) {
          return
        }
        // 用光标位置减去抓取偏移反投影，保证按下时图标上的那个点始终贴着光标
        const offset = this.dragOffset_ || { x: 0, y: 0 }
        const target = map.unproject([e.point.x - offset.x, e.point.y - offset.y])
        feature.position = toLonLat([target.lng, target.lat])
        this.mapComponent.updatePointSource(descriptor)
        const eventCallback = this.featureIdMap_.get(drag.featureId)
        if (eventCallback && typeof eventCallback.moveEvent === 'function') {
          eventCallback.moveEvent(e)
        }
      } else {
        // hover 手型光标（与 2D 版一致：悬停在要素上显示 pointer）
        const canvas = map.getCanvas()
        const hit = this.findAnyFeature_(e.point)
        if (hit) {
          if (canvas.style.cursor !== 'pointer') {
            this.previousCursor_ = canvas.style.cursor
            canvas.style.cursor = 'pointer'
          }
        } else if (this.previousCursor_ !== undefined) {
          canvas.style.cursor = this.previousCursor_
          this.previousCursor_ = undefined
        }
      }
    })

    const finishDrag = () => {
      const drag = this.dragging_
      if (!drag) {
        return
      }
      // 用要素自身坐标作为最终位置：鼠标位置带着抓取偏移，
      // 直接拿光标经纬度会让图标在松手时再跳一下
      const feature = drag.descriptor.features.get(drag.featureId)
      const lonLat = feature ? feature.position.slice() : null
      const wasActive = this.dragActive_

      // 先复位拖拽状态，再回调：即使 endEvent 里抛异常，也不会让图标继续“粘”在鼠标上
      this.dragging_ = null
      this.dragOffset_ = null
      this.dragStartPoint_ = null
      this.dragActive_ = false
      map.dragPan.enable()
      map.getCanvas().style.cursor = ''
      this.previousCursor_ = undefined

      // 纯点击（没有移动超过阈值）不触发 endEvent，编辑框保持原状
      if (!wasActive || !lonLat) {
        return
      }
      const eventCallback = this.featureIdMap_.get(drag.featureId)
      // 与 2D 版 DragInteraction 一致：回调对象上的 endEvent 才是函数，
      // 之前误写成 eventCallback(...)，会抛 "eventCallback is not a function"
      if (eventCallback && typeof eventCallback.endEvent === 'function') {
        eventCallback.endEvent({ lonLat })
      }
    }
    map.on('mouseup', () => {
      finishDrag()
    })
    // 鼠标在画布外释放时的兜底，避免拖拽状态卡死
    this._windowUpHandler = () => {
      if (this.dragging_) {
        finishDrag()
      }
    }
    window.addEventListener('mouseup', this._windowUpHandler)
  }
  destroy() {
    if (this._windowUpHandler) {
      window.removeEventListener('mouseup', this._windowUpHandler)
      this._windowUpHandler = null
    }
    this.dragging_ = null
    this.dragOffset_ = null
    this.dragStartPoint_ = null
    this.dragActive_ = false
    this.map = null
    this.mapComponent = null
  }
  // 查找当前可拖拽的要素（featureId 在 featureIdMap_ 中的点位要素）
  findDraggable_(point) {
    const map = this.map
    if (!map) {
      return null
    }
    const layers = []
    this.mapComponent.userLayers.forEach((d) => {
      if (d.type === 'point' && map.getLayer(d.layerIds[0])) {
        layers.push({ descriptor: d, layerId: d.layerIds[0] })
      }
    })
    for (const item of layers) {
      let features = []
      try {
        features = map.queryRenderedFeatures(point, { layers: [item.layerId] })
      } catch (err) {
        features = []
      }
      for (let i = 0; i < features.length; i++) {
        const fid = String(features[i].id)
        if (this.featureIdMap_.has(fid)) {
          return { featureId: fid, descriptor: item.descriptor }
        }
      }
    }
    return null
  }
  // 查找任意已注册图层的要素（用于 hover 光标）
  findAnyFeature_(point) {
    const map = this.map
    if (!map) {
      return null
    }
    const layers = []
    this.mapComponent.userLayers.forEach((d) => {
      if ((d.type === 'point' || d.type === 'vectorTile') && map.getLayer(d.layerIds[0])) {
        layers.push(d.layerIds[0])
      }
    })
    if (layers.length === 0) {
      return null
    }
    try {
      const features = map.queryRenderedFeatures(point, { layers: layers })
      return features.length > 0 ? features[0] : null
    } catch (err) {
      return null
    }
  }
  addFeatureId(id, moveEndEvent) {
    if (this.featureIdMap_.has(String(id))) {
      return
    }
    this.featureIdMap_.set(String(id), moveEndEvent)
  }
  removeFeatureId(id) {
    const key = String(id)
    this.featureIdMap_.delete(key)
    // 正处于拖拽中的要素被取消/保存时，立即结束拖拽状态，避免图标继续跟随鼠标
    if (this.dragging_ && this.dragging_.featureId === key) {
      this.dragging_ = null
      this.dragOffset_ = null
      this.dragStartPoint_ = null
      this.dragActive_ = false
      if (this.map) {
        this.map.dragPan.enable()
        this.map.getCanvas().style.cursor = ''
      }
    }
  }
  hasFeatureId(id) {
    return this.featureIdMap_.has(String(id))
  }
}

// ---------------------------------------------------------------------------
// 图层 wrapper：对外表现为与 2D 版（ol Layer）一致的调用接口
// ---------------------------------------------------------------------------
function makeLayerWrapper(descriptor) {
  return {
    descriptor: descriptor,
    get: (key) => {
      if (key === 'hideFeatures') {
        return descriptor.hideFeatures || []
      }
      return descriptor[key]
    },
    getSource: () => ({
      refresh: () => descriptor.__component.refreshDescriptor(descriptor),
      clear: () => descriptor.__component.clearDescriptor(descriptor),
      getFeatureById: (id) => descriptor.__component.getFeatureInDescriptor(descriptor, id),
      getExtent: () => descriptor.__component.getExtentOfDescriptor(descriptor)
    })
  }
}

export default {
  name: 'MapComponent3D',
  props: [],
  data() {
    return {
      overlayId: null,
      dragInteraction: new DragInteraction3D(),
      mapTileList: [],
      mapTileIndex: 0
    }
  },
  created() {
    this.$nextTick(() => {
      this.init()
    })
  },
  destroyed() {
    if (this.dragInteraction) {
      this.dragInteraction.destroy()
    }
    if (this._markers) {
      this._markers.forEach((marker) => {
        try {
          marker.remove()
        } catch (err) {
          // ignore
        }
      })
      this._markers.clear()
    }
    if (this.map) {
      this.map.remove()
      this.map = null
    }
  },
  methods: {
    init() {
      this.userLayers = []
      this.mapTileList = []
      this.mapTileIndex = 0
      this.overlayId = null
      this._styleGen = 0
      this._iconGen = -1
      this._customImageCache = new Map()
      this._customImageSeq = 0
      this._markers = new Map()
      this._loadedEmitted = false
      this._registerFlushScheduled = false

      this.$store.dispatch('server/getMapConfig')
        .then(mapConfigList => {
          if (mapConfigList.length === 0) {
            if (window.mapParam && window.mapParam.tilesUrl) {
              this.mapTileList.push({
                tilesUrl: window.mapParam.tilesUrl,
                coordinateSystem: window.mapParam.coordinateSystem
              })
            }
          } else {
            this.mapTileList = mapConfigList
          }
          this.initMap()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    initMap() {
      const mapParam = window.mapParam || {}
      window.coordinateSystem = this.mapTileList.length > 0
        ? this.mapTileList[this.mapTileIndex].coordinateSystem
        : (mapParam.coordinateSystem || 'GCJ02')

      const center = fromLonLat(mapParam.center || [116.41020, 39.915119])

      this.map = new maplibregl.Map({
        container: this.$refs.mapContainer,
        // mapconfig 返回的三个地址为三个 style 地址，直接作为 style 渲染
        style: this.styleForIndex(this.mapTileIndex, mapParam),
        center: center,
        zoom: mapParam.zoom || 10,
        pitch: mapParam.pitch || 0,
        minZoom: mapParam.minZoom || 1,
        maxZoom: mapParam.maxZoom || 19,
        attributionControl: false,
        dragRotate: true,
        fadeDuration: 0,
        pitchWithRotate: true
      })

      // 3D 俯仰/指北控件
      this.map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'top-right')

      this.dragInteraction.bind(this.map, this)

      // style 加载完成（首次加载 & 每次 setStyle 之后）：重建用户图层
      this.map.on('style.load', () => {
        this._styleGen += 1
        this.ensureIconImages().then(() => {
          this.registerAllUserLayers()
          this.emitLoadedOnce()
        })
      })
      this.map.on('zoom', () => {
        this.$emit('zoomChange', this.map.getZoom())
      })
      this.map.on('error', (e) => {
        const err = e && e.error
        // 只记录 style 级别错误，瓦片/图层加载错误不打扰控制台
        if (err && !e.sourceId && !e.layerId) {
          console.error('map style error', err)
          this.$message({
            showClose: true,
            message: '地图样式加载失败：' + (err.message || err),
            type: 'error'
          })
        }
        // style 地址不可达时同样触发一次 loaded，保证页面图层逻辑可继续
        this.emitLoadedOnce()
      })
    },
    emitLoadedOnce() {
      if (this._loadedEmitted) {
        return
      }
      this._loadedEmitted = true
      this.$emit('loaded')
    },
    /**
     * 取指定下标对应的 style：
     * - 正常情况 tilesUrl 即 style JSON 地址
     * - 若配置的是 raster 瓦片模板（含 {z}/{x}/{y}），则包装为内联 raster style 兜底
     */
    styleForIndex(index, mapParam) {
      mapParam = mapParam || (window.mapParam || {})
      if (this.mapTileList.length > 0 && this.mapTileList[index]) {
        const url = this.mapTileList[index].tilesUrl
        if (url && url.indexOf('{z}') >= 0) {
          return this.buildRasterStyle(url, mapParam)
        }
        return url
      }
      if (mapParam.tilesUrl) {
        return this.buildRasterStyle(mapParam.tilesUrl, mapParam)
      }
      // 无任何配置时使用 OpenStreetMap 兜底 style
      return 'https://demotiles.maplibre.org/style.json'
    },
    buildRasterStyle(tilesUrl, mapParam) {
      return {
        version: 8,
        sources: {
          'raster-fallback': {
            type: 'raster',
            tiles: [tilesUrl],
            tileSize: (mapParam && mapParam.tileSize) || 256
          }
        },
        layers: [
          { id: 'raster-bg', type: 'raster', source: 'raster-fallback' }
        ]
      }
    },
    // -------------------------------------------------------------------------
    // 图标资源：sprite.png (120x40) 横向排列 ON/OFF/checked 三个 40x40 图标
    // setStyle 会清空已注册图片，因此每个 style 代次需重新注册
    // -------------------------------------------------------------------------
    ensureIconImages() {
      const gen = this._styleGen
      if (this._iconGen === gen) {
        return Promise.resolve(true)
      }
      const map = this.map
      // MapLibre 5 的 loadImage 是 Promise API（resolve 值形如 { data }），
      // 旧版的 loadImage(url, callback) 已不再支持：传进去的 callback 永远不会被调用，
      // 会导致这里一直挂起、'loaded' 事件发不出去，页面图层因此不会初始化。
      return map.loadImage('static/images/gis/sprite.png')
        .then((response) => {
          const img = response && response.data
          if (!img) {
            console.error('加载地图图标 sprite 失败')
            return false
          }
          const regions = {
            'camera-on': [0, 0],
            'camera-off': [40, 0],
            'camera-checked': [80, 0]
          }
          Object.keys(regions).forEach((name) => {
            if (map.hasImage(name)) {
              return
            }
            const sx = regions[name][0]
            const sy = regions[name][1]
            const canvas = document.createElement('canvas')
            canvas.width = 80
            canvas.height = 80
            const ctx = canvas.getContext('2d')
            ctx.drawImage(img, sx, sy, 40, 40, 0, 0, 80, 80)
            // MapLibre 5 的 addImage 不接受 HTMLCanvasElement，只接受
            // HTMLImageElement / ImageBitmap / ImageData，这里转成 ImageData。
            // 2x 注册，保证高分屏清晰，icon-size 1 时显示 40x40 css 像素
            map.addImage(name, ctx.getImageData(0, 0, 80, 80), { pixelRatio: 2 })
          })
          // 重新注册自定义图片（setStyle 后被清空）
          this._customImageCache.forEach((item) => {
            if (!map.hasImage(item.name)) {
              map.addImage(item.name, item.img, { pixelRatio: 1 })
            }
          })
          if (this._styleGen === gen) {
            this._iconGen = gen
          }
          return true
        })
        .catch((err) => {
          // 图标加载失败不能阻塞图层初始化和 'loaded' 事件
          console.error('加载地图图标 sprite 失败', err)
          return false
        })
    },
    ensureCustomImage(src) {
      const map = this.map
      const cached = this._customImageCache.get(src)
      if (cached) {
        return Promise.resolve(cached.name)
      }
      const name = 'custom-image-' + (++this._customImageSeq)
      return map.loadImage(src)
        .then((response) => {
          const img = response && response.data
          if (!img) {
            return null
          }
          this._customImageCache.set(src, { name: name, img: img })
          if (!map.hasImage(name)) {
            map.addImage(name, img, { pixelRatio: 1 })
          }
          return name
        })
        .catch(() => null)
    },
    // -------------------------------------------------------------------------
    // 图层注册：setStyle 之后按 userLayers 顺序重建
    // -------------------------------------------------------------------------
    registerDescriptor(descriptor) {
      this.userLayers.push(descriptor)
      if (!this.registerLayer(descriptor)) {
        // style 还没完成加载时 addSource/addLayer 会抛 "Style is not done loading"，
        // 等地图空闲后再统一补注册（registerLayer 是幂等的）
        this.scheduleRegisterFlush()
      }
    },
    registerAllUserLayers() {
      this.userLayers.forEach((d) => {
        this.registerLayer(d)
      })
    },
    scheduleRegisterFlush() {
      if (this._registerFlushScheduled || !this.map) {
        return
      }
      this._registerFlushScheduled = true
      this.map.once('idle', () => {
        this._registerFlushScheduled = false
        if (this.map) {
          this.registerAllUserLayers()
        }
      })
    },
    registerLayer(d) {
      if (!this.map) {
        return false
      }
      if (d.type === 'group') {
        let ok = true
        d.children.forEach((child) => {
          if (!this.registerLayer(child)) {
            ok = false
          }
        })
        return ok
      }
      if (this.map.getLayer(d.layerIds[0])) {
        return true
      }
      // 注意不要用 map.isStyleLoaded() 作为判断条件：它在瓦片/图片未加载完时
      // 也会返回 false，但此时 addSource/addLayer 其实是可以成功的。
      // 真正未加载时 addLayer 会抛异常，用 try/catch 兜住并稍后重试。
      try {
        if (d.type === 'point') {
          this.registerPointLayer(d)
        } else if (d.type === 'vectorTile') {
          this.registerVectorTileLayer(d)
        } else if (d.type === 'line') {
          this.registerLineLayer(d)
        }
      } catch (err) {
        return false
      }
      return true
    },
    registerPointLayer(d) {
      const map = this.map
      if (map.getLayer(d.layerIds[0])) {
        return
      }
      map.addSource(d.sourceId, {
        type: 'geojson',
        data: this.pointSourceData(d)
      })
      const option = d.option || {}
      const layout = {
        'icon-size': 1
      }
      if (d.customImages) {
        layout['icon-anchor'] = ['match', ['get', 'anchor'], 'center', 'center', 'top', 'top', 'bottom']
      } else {
        layout['icon-anchor'] = 'bottom'
      }
      layout['icon-image'] = ['coalesce',
        ['get', 'customIcon'],
        ['match', ['get', 'status'], 'ON', 'camera-on', 'OFF', 'camera-off', 'checked', 'camera-checked', 'camera-on']
      ]
      const layer = {
        id: d.layerIds[0],
        type: 'symbol',
        source: d.sourceId,
        layout: layout
      }
      // MapLibre 的 maxzoom 是“不包含”的：如果写成地图的 maxZoom，
      // 在地图放大到最大层级时该图层会直接消失（定位通道正好会飞到 maxZoom）。
      // 因此默认不限制，仅在调用方显式传入时设置。
      if (option.minZoom != null) {
        layer.minzoom = option.minZoom
      }
      if (option.maxZoom != null) {
        layer.maxzoom = option.maxZoom
      }
      map.addLayer(layer)
    },
    registerVectorTileLayer(d) {
      const map = this.map
      if (map.getLayer(d.layerIds[0])) {
        return
      }
      map.addSource(d.sourceId, {
        type: 'vector',
        tiles: [d.tileUrl],
        'tile-size': 256
      })
      map.addLayer({
        id: d.layerIds[0],
        type: 'symbol',
        source: d.sourceId,
        'source-layer': 'points',
        minzoom: 0,
        filter: this.buildHideFilter(d),
        layout: {
          'icon-image': ['match', ['get', 'gbStatus'], 'ON', 'camera-on', 'OFF', 'camera-off', 'camera-off'],
          'icon-anchor': 'bottom',
          'icon-size': 1
        }
      })
    },
    registerLineLayer(d) {
      const map = this.map
      if (map.getLayer(d.layerIds[0])) {
        return
      }
      const coordinates = d.positions.map((p) => fromLonLat(p))
      map.addSource(d.sourceId, {
        type: 'geojson',
        data: {
          type: 'FeatureCollection',
          features: [
            {
              type: 'Feature',
              geometry: { type: 'LineString', coordinates: coordinates },
              properties: {}
            }
          ]
        }
      })
      map.addLayer({
        id: d.layerIds[0],
        type: 'line',
        source: d.sourceId,
        paint: {
          'line-color': '#0c6d6a',
          'line-width': 4
        }
      })
    },
    buildHideFilter(d) {
      // MapLibre 没有 `includes` 表达式，数组包含判断要用 `in`：
      // ["in", needle, haystack]，haystack 必须是 literal 数组。
      const hidden = (d.hideFeatures || []).map((id) => Number(id))
      return ['!', ['in', ['to-number', ['get', 'gbId']], ['literal', hidden]]]
    },
    updateHideFilter(d) {
      if (this.map && this.map.getLayer(d.layerIds[0])) {
        this.map.setFilter(d.layerIds[0], this.buildHideFilter(d))
      }
    },
    pointSourceData(d) {
      const features = []
      d.features.forEach((f) => {
        const properties = {
          id: f.id,
          status: f.status,
          customIcon: f.imageName || null
        }
        if (f.anchor) {
          properties.anchor = f.anchor
        }
        features.push({
          type: 'Feature',
          id: String(f.id),
          geometry: { type: 'Point', coordinates: fromLonLat(f.position) },
          properties: properties
        })
      })
      return { type: 'FeatureCollection', features: features }
    },
    updatePointSource(d) {
      if (this.map && this.map.getSource(d.sourceId)) {
        this.map.getSource(d.sourceId).setData(this.pointSourceData(d))
      }
    },
    // -------------------------------------------------------------------------
    // 对外接口：与 MapComponent.vue 一致
    // -------------------------------------------------------------------------
    /**
     * 添加 MVT 矢量瓦片图层（摄像头点位瓦片）
     */
    addVectorTileLayer(tileUrl, clickEvent, errorEvent) {
      const sep = tileUrl.indexOf('?') >= 0 ? '&' : '?'
      const coordSys = this.mapTileList.length > 0
        ? this.mapTileList[this.mapTileIndex].coordinateSystem
        : ((window.mapParam && window.mapParam.coordinateSystem) || 'GCJ02')
      tileUrl += `${sep}geoCoordSys=${coordSys}&accessToken=${this.$store.getters.token}`
      const sourceId = genId('mvt')
      const iconLayerId = sourceId + '-icon'
      const d = {
        type: 'vectorTile',
        sourceId: sourceId,
        layerIds: [iconLayerId],
        tileUrl: tileUrl,
        clickEvent: clickEvent,
        errorEvent: errorEvent,
        hideFeatures: []
      }
      d.__component = this
      // 图层事件按 layerId 注册，setStyle 重建图层后依然有效
      this.map.on('click', iconLayerId, (e) => {
        if (clickEvent && e.features && e.features.length > 0) {
          const items = []
          for (let i = 0; i < e.features.length; i++) {
            items.push(e.features[i].properties)
          }
          clickEvent(items)
        }
      })
      this.map.on('error', (e) => {
        if (errorEvent && e && e.sourceId === sourceId) {
          errorEvent(e)
        }
      })
      this.registerDescriptor(d)
      return makeLayerWrapper(d)
    },
    /**
     * 添加点位图层， 数据坐标系由控件内完成，输入和输出永远是wgs84
     * data: [{ id, position: [lng, lat], data, status }]
     */
    addPointLayer(data, clickEvent, option) {
      const d = this.createPointDescriptor(data, clickEvent, option)
      this.registerDescriptor(d)
      return makeLayerWrapper(d)
    },
    createPointDescriptor(data, clickEvent, option) {
      const sourceId = genId('point')
      const layerId = sourceId + '-icon'
      const features = new Map()
      if (data && data.length > 0) {
        for (let i = 0; i < data.length; i++) {
          features.set(String(data[i].id), {
            id: data[i].id,
            position: data[i].position,
            data: data[i].data,
            status: data[i].status
          })
        }
      }
      const d = {
        type: 'point',
        sourceId: sourceId,
        layerIds: [layerId],
        features: features,
        clickEvent: clickEvent,
        option: option || {}
      }
      d.__component = this
      this.map.on('click', layerId, (e) => {
        if (clickEvent && e.features && e.features.length > 0) {
          const items = []
          for (let i = 0; i < e.features.length; i++) {
            const feature = features.get(String(e.features[i].id))
            if (feature && feature.data) {
              items.push(feature.data)
            }
          }
          if (items.length > 0) {
            clickEvent(items)
          }
        }
      })
      return d
    },
    /**
     * 添加点位图层（每个点位自定义图标）
     * data: [{ id, position, data, image: {src, anchor} }]
     */
    createPointLayer2(data, clickEvent, option) {
      if (!data || data.length === 0) {
        return
      }
      const sourceId = genId('point')
      const layerId = sourceId + '-icon'
      const features = new Map()
      for (let i = 0; i < data.length; i++) {
        const anchor = (data[i].image && data[i].image.anchor) ? data[i].image.anchor : [0.5, 1]
        features.set(String(data[i].id), {
          id: data[i].id,
          position: data[i].position,
          data: data[i].data,
          status: data[i].status,
          image: data[i].image || null,
          anchor: anchor[1] === 1 ? 'bottom' : (anchor[1] === 0 ? 'top' : 'center')
        })
      }
      const d = {
        type: 'point',
        sourceId: sourceId,
        layerIds: [layerId],
        features: features,
        clickEvent: clickEvent,
        option: option || {},
        customImages: true
      }
      d.__component = this
      this.map.on('click', layerId, (e) => {
        if (clickEvent && e.features && e.features.length > 0) {
          const items = []
          for (let i = 0; i < e.features.length; i++) {
            const feature = features.get(String(e.features[i].id))
            if (feature && feature.data) {
              items.push(feature.data)
            }
          }
          if (items.length > 0) {
            clickEvent(items)
          }
        }
      })
      this.registerDescriptor(d)
      // 异步加载自定义图标
      d.features.forEach((f) => {
        if (f.image && f.image.src) {
          this.ensureCustomImage(f.image.src).then((name) => {
            if (name) {
              f.imageName = name
              this.updatePointSource(d)
            }
          })
        }
      })
      return makeLayerWrapper(d)
    },
    addPointLayerGroup(data, clickEvent) {
      const keys = Array.from(data.keys())
      const children = []
      for (let i = 0; i < keys.length; i++) {
        const zoom = keys[i]
        const child = this.createPointDescriptor(data.get(zoom), clickEvent, {
          minZoom: zoom
        })
        children.push(child)
      }
      const d = {
        type: 'group',
        sourceId: null,
        layerIds: children.reduce((acc, c) => acc.concat(c.layerIds), []),
        children: children
      }
      d.__component = this
      this.registerDescriptor(d)
      return makeLayerWrapper(d)
    },
    updatePointLayerGroup(layer, data, postponement) {
      // 与 2D 版一致：空实现
    },
    updatePointLayer(layer, data, postponement) {
      const d = layer.descriptor
      d.features.clear()
      if (data && data.length > 0) {
        for (let i = 0; i < data.length; i++) {
          d.features.set(String(data[i].id), {
            id: data[i].id,
            position: data[i].position,
            data: data[i].data,
            status: data[i].status
          })
        }
      }
      this.updatePointSource(d)
      return layer
    },
    hasFeature(layer, id) {
      const d = layer.descriptor
      if (d.type === 'group') {
        return true
      }
      if (d.features && d.features.has(String(id))) {
        return true
      }
      return false
    },
    addFeature(layer, data) {
      const d = layer.descriptor
      d.features.set(String(data.id), {
        id: data.id,
        position: data.position,
        data: data.data,
        status: data.status
      })
      this.updatePointSource(d)
    },
    setFeaturePositionById(layer, featureId, data) {
      const d = layer.descriptor
      d.features.set(String(featureId), {
        id: data.id,
        position: data.position,
        data: data.data,
        status: data.status
      })
      this.updatePointSource(d)
    },
    setFeatureImageById(layer, featureId, image) {
      const d = layer.descriptor
      const feature = d.features.get(String(featureId))
      if (!feature) {
        console.error('更改feature的图标时未找到图标')
        return
      }
      feature.image = image
      this.ensureCustomImage(image.src).then((name) => {
        if (!name) {
          return
        }
        feature.imageName = name
        this.updatePointSource(d)
      })
    },
    hideFeature(layer, featureId) {
      const d = layer.descriptor
      const list = d.hideFeatures || (d.hideFeatures = [])
      if (list.indexOf(featureId) === -1) {
        list.push(featureId)
        // 仅 MVT 瓦片图层有 gbId 属性可过滤（与 2D 版语义一致）
        if (d.type === 'vectorTile') {
          this.updateHideFilter(d)
        }
      }
    },
    cancelHideFeature(layer, featureId) {
      const d = layer.descriptor
      const list = d.hideFeatures || []
      const index = list.indexOf(featureId)
      if (index >= 0) {
        list.splice(index, 1)
        if (d.type === 'vectorTile') {
          this.updateHideFilter(d)
        }
      }
    },
    removeLayer(layer) {
      if (!layer || !layer.descriptor) {
        return
      }
      const d = layer.descriptor
      const subs = d.type === 'group' ? d.children : [d]
      // 从用户图层列表中移除（group 只移除自身，其子图层随 group 一并销毁）
      const topIndex = this.userLayers.indexOf(d)
      if (topIndex >= 0) {
        this.userLayers.splice(topIndex, 1)
      }
      subs.forEach((sub) => {
        try {
          if (this.map) {
            sub.layerIds.forEach((layerId) => {
              if (this.map.getLayer(layerId)) {
                this.map.removeLayer(layerId)
              }
            })
            if (sub.sourceId && this.map.getSource(sub.sourceId)) {
              this.map.removeSource(sub.sourceId)
            }
          }
        } catch (err) {
          console.error('removeLayer error', err)
        }
      })
    },
    clearLayer(layer) {
      this.clearDescriptor(layer.descriptor)
    },
    clearDescriptor(d) {
      if (d.type === 'point') {
        d.features.clear()
        this.updatePointSource(d)
      } else if (d.type === 'group') {
        d.children.forEach((child) => this.clearDescriptor(child))
      }
    },
    refreshLayer(layer) {
      this.refreshDescriptor(layer.descriptor)
    },
    refreshDescriptor(d) {
      if (d.type === 'point') {
        this.updatePointSource(d)
        if (this.map) {
          this.map.triggerRepaint()
        }
      } else if (d.type === 'vectorTile') {
        // 移除并重建 source，强制重新拉取瓦片
        try {
          if (this.map) {
            if (this.map.getLayer(d.layerIds[0])) {
              this.map.removeLayer(d.layerIds[0])
            }
            if (this.map.getSource(d.sourceId)) {
              this.map.removeSource(d.sourceId)
            }
          }
        } catch (err) {
          // ignore
        }
        this.registerLayer(d)
      } else if (d.type === 'group') {
        d.children.forEach((child) => this.refreshDescriptor(child))
      }
    },
    getFeatureInDescriptor(d, id) {
      if (d.type === 'point' && d.features.has(String(id))) {
        return d.features.get(String(id))
      }
      if (d.type === 'group') {
        for (let i = 0; i < d.children.length; i++) {
          const f = this.getFeatureInDescriptor(d.children[i], id)
          if (f) {
            return f
          }
        }
      }
      return null
    },
    getExtentOfDescriptor(d) {
      if (d.type !== 'point' && d.type !== 'group') {
        return null
      }
      const list = d.type === 'group' ? d.children : [d]
      let minLng = Infinity
      let minLat = Infinity
      let maxLng = -Infinity
      let maxLat = -Infinity
      let found = false
      list.forEach((sub) => {
        sub.features.forEach((f) => {
          const c = fromLonLat(f.position)
          found = true
          if (c[0] < minLng) minLng = c[0]
          if (c[1] < minLat) minLat = c[1]
          if (c[0] > maxLng) maxLng = c[0]
          if (c[1] > maxLat) maxLat = c[1]
        })
      })
      if (!found) {
        return null
      }
      return [minLng, minLat, maxLng, maxLat]
    },
    fit(layer) {
      const extent = this.getExtentOfDescriptor(layer.descriptor)
      if (extent) {
        this.map.fitBounds([[extent[0], extent[1]], [extent[2], extent[3]]], {
          duration: 600,
          padding: [100, 100, 100, 100]
        })
      }
    },
    addLineLayer(positions) {
      if (!positions || positions.length === 0) {
        return
      }
      const sourceId = genId('line')
      const layerId = sourceId + '-line'
      const d = {
        type: 'line',
        sourceId: sourceId,
        layerIds: [layerId],
        positions: positions
      }
      d.__component = this
      this.registerDescriptor(d)
      return makeLayerWrapper(d)
    },
    openInfoBox(position, content, offset) {
      if (!this.map || !content) {
        return null
      }
      if (this.overlayId !== null) {
        this.closeInfoBox(this.overlayId)
        this.overlayId = null
      }
      const id = genInfoBoxId()
      const lngLat = fromLonLat(position)
      const marker = new maplibregl.Marker({
        element: content,
        anchor: 'bottom',
        offset: offset || [0, 0]
      }).setLngLat(lngLat)
      // Marker 不是 IControl（没有 onAdd/onRemove），不能用 map.addControl()，
      // 否则 MapLibre 只会抛 "Invalid argument to map.addControl()" 而不挂载元素，
      // 表现为定位通道后信息弹窗不显示。
      marker.addTo(this.map)
      // 简化版 autoPan：超出视野时平移
      if (!this.map.getBounds().contains(lngLat)) {
        this.map.panTo(lngLat)
      }
      this._markers.set(id, marker)
      this.overlayId = id
      return id
    },
    closeInfoBox(id) {
      const marker = id !== null && id !== undefined ? this._markers.get(id) : null
      if (marker) {
        // marker.remove() 会把 marker 元素（即调用方传入的 Vue 节点）从 DOM 移除，
        // 下次 openInfoBox 时 marker.addTo() 会重新挂载
        marker.remove()
        this._markers.delete(id)
      }
      if (this.overlayId === id) {
        this.overlayId = null
      }
    },
    setCenter(point) {
      if (this.map) {
        this.map.setCenter(fromLonLat(point))
      }
    },
    getCenter() {
      const c = this.map.getCenter()
      return toLonLat([c.lng, c.lat])
    },
    getZoom() {
      return this.map.getZoom()
    },
    zoomIn() {
      if (this.map.getZoom() >= this.map.getMaxZoom()) {
        return
      }
      this.map.zoomIn()
    },
    zoomOut() {
      if (this.map.getZoom() <= this.map.getMinZoom()) {
        return
      }
      this.map.zoomOut()
    },
    centerAndZoom(point, zoom, callback) {
      const targetZoom = zoom || this.map.getZoom()
      this.map.easeTo({
        center: fromLonLat(point),
        zoom: targetZoom,
        duration: 600
      })
      if (callback) {
        setTimeout(callback, 700)
      }
    },
    coordinateInView(point) {
      return this.map.getBounds().contains(fromLonLat(point))
    },
    panTo(point, zoom, endCallback) {
      const coordinate = fromLonLat(point)
      const finish = () => {
        if (endCallback) {
          endCallback()
        }
      }
      if (this.map.getBounds().contains(coordinate)) {
        this.map.setCenter(coordinate)
        if (zoom !== undefined && zoom !== null && zoom !== this.map.getZoom()) {
          this.map.setZoom(zoom)
        }
        finish()
        return
      }
      this.map.flyTo({
        center: coordinate,
        zoom: zoom || this.map.getZoom(),
        duration: 1500
      })
      setTimeout(finish, 1600)
    },
    getZoomExtent() {
      return [this.map.getMinZoom(), this.map.getMaxZoom()]
    },
    /**
     * 根据距离计算经纬度差值，方便前端抽稀计算
     * @param distance 距离， 单位：像素值
     * @param zoom 地图层级，默认取值当前层级
     */
    computeDiff(distance, zoom) {
      if (!distance) {
        return []
      }
      const z = zoom || this.map.getZoom()
      // webmercator 下 zoom z 时整个世界 360 度对应 256 * 2^z 像素
      const pixelsPerWorld = 256 * Math.pow(2, z)
      return distance * 360 / pixelsPerWorld
    },
    startDrawBox(callback) {
      const map = this.map
      const sourceId = genId('drawbox')
      const layerId = sourceId + '-box'
      const lineLayerId = sourceId + '-box-line'
      let startLngLat = null
      let moved = false

      map.addSource(sourceId, {
        type: 'geojson',
        data: { type: 'FeatureCollection', features: [] }
      })
      map.addLayer({
        id: layerId,
        type: 'fill',
        source: sourceId,
        paint: {
          'fill-color': 'rgba(255, 97, 97, 0.24)',
          'fill-opacity': 1
        }
      })
      map.addLayer({
        id: lineLayerId,
        type: 'line',
        source: sourceId,
        paint: {
          'line-color': 'rgba(255, 97, 97, 0.84)',
          'line-width': 0
        }
      })

      const updateBox = (a, b) => {
        if (!map.getSource(sourceId)) {
          return
        }
        const min = [Math.min(a.lng, b.lng), Math.min(a.lat, b.lat)]
        const max = [Math.max(a.lng, b.lng), Math.max(a.lat, b.lat)]
        map.getSource(sourceId).setData({
          type: 'FeatureCollection',
          features: [
            {
              type: 'Feature',
              geometry: {
                type: 'Polygon',
                coordinates: [[min, max, max, min, min]]
              },
              properties: {}
            }
          ]
        })
      }
      const cleanup = () => {
        map.off('mousedown', onDown)
        map.off('mousemove', onMove)
        map.off('mouseup', onUp)
        window.removeEventListener('mouseup', onWindowUp)
        try {
          if (map.getLayer(lineLayerId)) {
            map.removeLayer(lineLayerId)
          }
          if (map.getLayer(layerId)) {
            map.removeLayer(layerId)
          }
          if (map.getSource(sourceId)) {
            map.removeSource(sourceId)
          }
        } catch (err) {
          // ignore
        }
        map.dragPan.enable()
        map.getCanvas().style.cursor = ''
      }
      const onDown = (e) => {
        if (e.originalEvent && e.originalEvent.button !== 0) {
          return
        }
        startLngLat = e.lngLat
        moved = false
        map.dragPan.disable()
      }
      const onMove = (e) => {
        if (!startLngLat) {
          return
        }
        moved = true
        updateBox(startLngLat, e.lngLat)
      }
      const onUp = (e) => {
        if (!startLngLat) {
          return
        }
        if (moved) {
          updateBox(startLngLat, e.lngLat)
          const a = toLonLat([startLngLat.lng, startLngLat.lat])
          const b = toLonLat([e.lngLat.lng, e.lngLat.lat])
          const extent = [
            Math.min(a[0], b[0]),
            Math.min(a[1], b[1]),
            Math.max(a[0], b[0]),
            Math.max(a[1], b[1])
          ]
          cleanup()
          callback(extent)
        } else {
          cleanup()
        }
      }
      // 鼠标在画布外释放时的兜底，避免拖拽状态卡死
      const onWindowUp = () => {
        if (startLngLat) {
          cleanup()
        }
      }
      map.on('mousedown', onDown)
      map.on('mousemove', onMove)
      map.on('mouseup', onUp)
      window.addEventListener('mouseup', onWindowUp)
    },
    getCurrentCoordinateSystem() {
      return this.mapTileList[this.mapTileIndex].coordinateSystem
    },
    changeMapTile(index) {
      if (!this.map || this.mapTileList.length === 0 || index < 0 || index >= this.mapTileList.length) {
        return
      }
      const mapParam = window.mapParam || {}
      const oldConfig = this.mapTileList[this.mapTileIndex]
      // 记录当前视角，坐标系切换后修正回同一点
      const centerWgs = this.getCenter()
      const zoom = this.map.getZoom()
      const pitch = this.map.getPitch()
      const bearing = this.map.getBearing()
      this.mapTileIndex = index
      const newConfig = this.mapTileList[index]
      window.coordinateSystem = newConfig.coordinateSystem
      const coordSystemChanged = !oldConfig || oldConfig.coordinateSystem !== newConfig.coordinateSystem
      // setStyle 只接受 style JSON 或 URL（center/zoom/pitch 不是它的选项，
      // 误传会被当成 style 校验，报 missing required property "version"）。
      // diff:false 强制完整重建，确保触发 style.load 来重建用户图层。
      this.map.setStyle(this.styleForIndex(index, mapParam), { diff: false })
      if (coordSystemChanged) {
        // 相机不会被 setStyle 重置，但坐标系变了以后同一屏幕位置对应的 WGS84 点会偏移，
        // 需要按新坐标系重新投影原来的中心点
        this.map.jumpTo({ center: fromLonLat(centerWgs), zoom, pitch, bearing })
        this.$emit('coordinateSystemChange', newConfig.coordinateSystem)
      }
    },
    getCoordSys() {
      return this.mapTileList[this.mapTileIndex].coordinateSystem
    }
  }
}
</script>

<style>
.maplibregl-canvas {
  outline: none;
}
</style>

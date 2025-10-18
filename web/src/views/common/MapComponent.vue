<template>
  <div id="mapContainer" ref="mapContainer" style="width: 100%;height: 100%;" />
</template>

<script>
import 'ol/ol.css'
import Map from 'ol/Map'
import OSM from 'ol/source/OSM'
import XYZ from 'ol/source/XYZ'
import VectorSource from 'ol/source/Vector'
import Tile from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import LayerGroup from 'ol/layer/Group'
import WebGLPointslayer from 'ol/layer/WebGLPoints'
import Style from 'ol/style/Style'
import Stroke from 'ol/style/Stroke'
import Icon from 'ol/style/Icon'
import View from 'ol/View'
import Feature from 'ol/Feature'
import Overlay from 'ol/Overlay'
import { Point, LineString } from 'ol/geom'
import { get as getProj } from 'ol/proj'
import { containsCoordinate } from 'ol/extent'
import { defaults as defaultInteractions } from 'ol/interaction'
import Draw, { createBox } from 'ol/interaction/Draw'
import DragInteraction from './map/DragInteraction'
import { fromLonLat, toLonLat } from './map/TransformLonLat'

import { v4 } from 'uuid'
import { getUid } from 'ol'
import {Fill} from "ol/style";

let olMap, tileLayer = null
export default {
  name: 'MapComponent',
  props: [],
  data() {
    return {
      overlayId: null,
      dragInteraction: new DragInteraction(),
      mapTileList: [],
      mapTileIndex: 0
    }
  },
  created() {
    this.$nextTick(() => {
      this.init()
    })

  },
  mounted() {

  },
  destroyed() {
  },
  methods: {
    init() {
      this.$store.dispatch('server/getMapConfig')
        .then(mapConfigList => {
          console.log(mapConfigList.length)
          if (mapConfigList.length === 0) {
           if (window.mapParam.tilesUrl) {
             this.mapTileList.push({
               tilesUrl: window.mapParam.tilesUrl,
               coordinateSystem: window.mapParam.coordinateSystem
             })
           }
          }else {
            this.mapTileList = mapConfigList
          }
          this.initMap()
      })
    },
    initMap(){
      let center = fromLonLat([116.41020, 39.915119])
      window.coordinateSystem = this.mapTileList[this.mapTileIndex].coordinateSystem
      if (window.mapParam.center) {
        center = fromLonLat(window.mapParam.center)
      }
      const view = new View({
        center: center,
        zoom: window.mapParam.zoom || 10,
        projection: this.projection,
        maxZoom: window.mapParam.maxZoom || 19,
        minZoom: window.mapParam.minZoom || 1
      })

      if (this.mapTileList.length > 0 && this.mapTileList[this.mapTileIndex].tilesUrl) {
        tileLayer = new Tile({
          source: new XYZ({
            projection: getProj('EPSG:3857'),
            wrapX: false,
            tileSize: 256 || window.mapParam.tileSize,
            url: this.mapTileList[this.mapTileIndex].tilesUrl
          })
        })
      } else {
        tileLayer = new Tile({
          preload: 4,
          source: new OSM()
        })
      }
      olMap = new Map({
        interactions: defaultInteractions().extend([this.dragInteraction]),
        target: this.$refs.mapContainer, // 容器ID
        layers: [tileLayer], // 默认图层
        view: view, // 视图
        controls: [ // 控件
        ]
      })
      // olMap.addControl(new ZoomSlider({
      //   className: 'zoom-slider'
      // }))
      olMap.once('loadend', event => {
        this.$emit('loaded')
      })
      olMap.on('click', event => {
        let features = {}
        let layers = {}
        // 单个元素事件传递
        olMap.forEachFeatureAtPixel(event.pixel, (featureAtPixel, layerAtPixel) => {

          if (layerAtPixel) {
            let ol_uid = 'key' + getUid(layerAtPixel)
            layers[ol_uid] = layerAtPixel
            if (Object.hasOwn(features, ol_uid)) {
              features[ol_uid].push(featureAtPixel)
            } else {
              features[ol_uid] = new Array(featureAtPixel)
            }
          }
        })
        // 遍历图层，传递事件
        for (const key in layers) {
          if (Object.hasOwn(layers, key)) {
            var layer = layers[key]
            layer.dispatchEvent({ type: 'click', event: event, features: features[key], outParam: { layersCount: Object.keys(layers).length } });
          }
        }
        features = {}
        layer = {}
      })
      olMap.getView().on('change:resolution', () => {
        this.$emit('zoomChange', olMap.getView().getZoom())
      })
    },
    setCenter(point) {

    },
    getCenter() {
      return toLonLat(olMap.getView().getCenter())
    },
    getZoom() {
      return olMap.getView().getZoom()
    },
    zoomIn() {
      let zoom = olMap.getView().getZoom()
      if (zoom >= olMap.getView().getMaxZoom()) {
        return
      }
      olMap.getView().animate({
        zoom: Math.trunc(zoom) + 1,
        duration: 600
      })
    },
    zoomOut() {
      let zoom = olMap.getView().getZoom()
      if (zoom <= olMap.getView().getMinZoom()) {
        return
      }
      olMap.getView().animate({
        zoom: Math.trunc(zoom) - 1,
        duration: 400
      })
    },
    centerAndZoom(point, zoom, callback) {
      var zoom_ = olMap.getView().getZoom()
      zoom = zoom || zoom_
      var duration = 600
      olMap.getView().setCenter(fromLonLat(point))
      olMap.getView().animate({
        zoom: zoom,
        duration: duration
      })
    },
    coordinateInView: function(point) {
      return containsCoordinate(olMap.getView().calculateExtent(), fromLonLat(point))
    },
    panTo(point, zoom, endCallback) {
      const duration = 1500
      var coordinate = fromLonLat(point)
      if (containsCoordinate(olMap.getView().calculateExtent(), coordinate)) {
        olMap.getView().setCenter(coordinate)
        if (zoom !== olMap.getView().getZoom()) {
          olMap.getView().setZoom(zoom)
        }
        if (endCallback) {
          endCallback()
        }
        return
      }

      olMap.getView().cancelAnimations()
      olMap.getView().animate({
        center: coordinate,
        duration: duration
      })
      olMap.getView().animate({
        zoom: zoom -2,
        duration: duration / 2
      }, {
        zoom: zoom || olMap.getView().getZoom(),
        duration: duration / 2
      })
      setTimeout(endCallback, duration + 100)
    },
    fit(layer) {
      const extent = layer.getSource().getExtent()
      if (extent) {
        olMap.getView().fit(extent, {
          duration: 600,
          padding: [100, 100, 100, 100]
        })
      }
    },
    openInfoBox(position, content, offset) {
      if (this.overlayId !== null) {
        this.closeInfoBox(this.overlayId)
        this.overlayId = null
      }
      const id = v4()
      // let infoBox = document.createElement('div')
      // infoBox.setAttribute('id', id)
      // infoBox.innerHTML = content
      const overlay = new Overlay({
        id: id,
        autoPan: true,
        autoPanAnimation: {
          duration: 250
        },
        element: content,
        positioning: 'bottom-center',
        offset: offset,
        position: fromLonLat(position)
        // className:overlayStyle.className
      })
      olMap.addOverlay(overlay)
      this.overlayId = id
      return id
    },
    closeInfoBox(id) {
      let overlay = olMap.getOverlayById(id)
      if (overlay) {
        olMap.removeOverlay(overlay)
      }
      var element = document.getElementById(id)
      if (element) {
        element.remove()
      }
    },
    /**
     * 添加图层， 数据坐标系由控件内完成，输入和输出永远是wgs84
     * [
     *     {
     *
     *         position: [119.1212,45,122],
     *             image: {
     *                 src:"/images/123.png",
     *                 anchor: [0.5, 0.5]
     *
     *             }
     *     }
     *
     * ]
     * @param data
     * @param clickEvent
     * @param option
     */
    addPointLayer(data, clickEvent, option) {
      if (data.length > 0) {
        let vectorLayer = this.createPointLayer(data, clickEvent, option)
        olMap.addLayer(vectorLayer)
        return vectorLayer
      }
    },
    createPointLayer(data, clickEvent, option){
      if (data.length > 0) {
        const features = []
        let maxZoom = (option && option.maxZoom) ? option.maxZoom : olMap.getView().getMaxZoom()
        let minZoom = (option && option.minZoom) ? option.minZoom : olMap.getView().getMinZoom()
        let declutter = option && option.declutter

        for (let i = 0; i < data.length; i++) {
          const feature = new Feature(new Point(fromLonLat(data[i].position)))
          feature.setId(data[i].id)
          feature.customData = data[i].data
          // const style = new Style()
          // style.setImage(new Icon({
          //   anchor: data[i].image.anchor,
          //   crossOrigin: 'Anonymous',
          //   src: data[i].image.src,
          //   opacity: 1
          // }))
          // feature.setStyle(style)
          features.push(feature)
        }
        const source = new VectorSource()
        source.addFeatures(features)
        const vectorLayer = new WebGLPointslayer({
          source: source,
          maxZoom: maxZoom,
          minZoom: minZoom,
          style: {
            // 必须提供 style 配置，可以是对象或函数
            // 'circle-radius': 3,
            // 'circle-fill-color': 'red',
            // 'circle-stroke-color': 'white',
            // 'circle-stroke-width': 0.5
            'icon-src': 'static/images/gis/camera1.png',
            'icon-offset': [0, 12],
            'icon-width': 40,
            'icon-height': 40
          }
        })
        if (clickEvent && typeof clickEvent === 'function') {
          vectorLayer.on('click', (event) => {

            if (event.features.length > 0) {
              const items = []
              for (let i = 0; i < event.features.length; i++) {
                items.push(event.features[i].customData)
              }
              clickEvent(items)
            }
          })
        }

        return vectorLayer
      }
    },
    createPointLayer2(data, clickEvent, option){
      if (data.length > 0) {
        const features = []
        let maxZoom = (option && option.maxZoom) ? option.maxZoom : olMap.getView().getMaxZoom()
        let minZoom = (option && option.minZoom) ? option.minZoom : olMap.getView().getMinZoom()
        let declutter = option && option.declutter

        for (let i = 0; i < data.length; i++) {
          const feature = new Feature(new Point(fromLonLat(data[i].position)))
          feature.setId(data[i].id)
          feature.customData = data[i].data
          const style = new Style()
          style.setImage(new Icon({
            anchor: data[i].image.anchor,
            crossOrigin: 'Anonymous',
            src: data[i].image.src,
            opacity: 1
          }))
          feature.setStyle(style)
          features.push(feature)
        }
        const source = new VectorSource()
        source.addFeatures(features)
        const vectorLayer = new VectorLayer({
          source: source,
          renderMode: 'image',
          declutter: declutter,
          maxZoom: maxZoom,
          minZoom: minZoom
        })
        if (clickEvent && typeof clickEvent === 'function') {
          vectorLayer.on('click', (event) => {

            if (event.features.length > 0) {
              const items = []
              for (let i = 0; i < event.features.length; i++) {
                items.push(event.features[i].customData)
              }
              clickEvent(items)
            }
          })
        }

        return vectorLayer
      }
    },
    updatePointLayer(layer, data, postponement) {
      layer.getSource().clear(true)
      const features = []
      for (let i = 0; i < data.length; i++) {
        const feature = new Feature(new Point(fromLonLat(data[i].position)))
        feature.setId(data[i].id)
        feature.customData = data[i].data
        const cloneStyle = new Style()
        cloneStyle.setImage(new Icon({
          anchor: data[i].image.anchor,
          crossOrigin: 'Anonymous',
          src: data[i].image.src
        }))
        feature.setStyle(cloneStyle)
        features.push(feature)
      }
      layer.getSource().addFeatures(features)
      if (postponement) {
        olMap.removeLayer(layer)
        setTimeout(() => {
          olMap.addLayer(layer)
        }, 100)
      }
      return layer
    },
    addPointLayerGroup(data, clickEvent) {

      let keys = Array.from(data.keys())

      let layers = []
      for (let i = 0; i < keys.length; i++) {
        let zoom = keys[i]
        console.log(zoom)
        let vectorLayer = this.createPointLayer(data.get(zoom), clickEvent, {
          minZoom : zoom
        })
        if (vectorLayer) {
          layers.push(vectorLayer)
        }
      }
      let groupLayer = new LayerGroup({
        layers: layers
      })
      olMap.addLayer(groupLayer)
      return groupLayer
    },
    updatePointLayerGroup(layer, data, postponement) {

    },

    removeLayer(layer) {
      olMap.removeLayer(layer)
    },
    setFeatureImageById(layer, featureId, image) {
      let feature = layer.getSource().getFeatureById(featureId)
      if (!feature) {
        console.error('更改feature的图标时未找到图标')
        return
      }
      let style = feature.getStyle()
      style.setImage(new Icon({
        anchor: image.anchor,
        crossOrigin: 'Anonymous',
        src: image.src
      }))
      feature.setStyle(style)
      olMap.render()
    },
    setFeaturePositionById(layer, featureId, data) {
      let featureOld = layer.getSource().getFeatureById(featureId)
      if (featureOld) {
        layer.getSource().removeFeature(featureOld)
      }

      const feature = new Feature(new Point(fromLonLat(data.position)))
      feature.setId(data.id)
      feature.customData = data.data
      const style = new Style()
      style.setImage(new Icon({
        anchor: data.image.anchor,
        crossOrigin: 'Anonymous',
        src: data.image.src
      }))
      feature.setStyle(style)
      layer.getSource().addFeature(feature)
    },

    addLineLayer(positions) {
      if (positions.length > 0) {
        const points = []
        for (let i = 0; i < positions.length; i++) {
          points.push(fromLonLat(positions[i]))
        }
        const line = new LineString(points)
        const lineFeature = new Feature(line)
        lineFeature.setStyle(new Style({
          stroke: new Stroke({
            width: 4,
            color: '#0c6d6a'
          })
        }))
        const source = new VectorSource()
        source.addFeature(lineFeature)
        const vectorLayer = new VectorLayer({
          source: source
        })
        olMap.addLayer(vectorLayer)
        return vectorLayer
      }
    },
    getCurrentCoordinateSystem() {
      return this.mapTileList[this.mapTileIndex].coordinateSystem
    },
    changeMapTile(index) {
      let center = this.getCenter()
      let mapTileConfig = this.mapTileList[this.mapTileIndex]
      this.mapTileIndex = index
      window.coordinateSystem = this.mapTileList[this.mapTileIndex].coordinateSystem
      tileLayer.getSource().setUrl(this.mapTileList[index].tilesUrl)
      if (mapTileConfig.coordinateSystem !== this.mapTileList[this.mapTileIndex].coordinateSystem) {
        // 发送通知
        this.$emit('coordinateSystemChange', this.mapTileList[this.mapTileIndex].coordinateSystem)
        // 修正地图的中心点
        olMap.getView().setCenter(fromLonLat(center))
      }
    },
    getZoomExtent(){
      return [olMap.getView().getMinZoom(), olMap.getView().getMaxZoom()]
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
      let resolution
      if (!zoom) {
        resolution = olMap.getView().getResolution()
      }else {
        resolution = olMap.getView().getResolutionForZoom(zoom)
      }
      let diff = resolution * distance
      return toLonLat([diff, diff])[0]


      // let extent = olMap.getView().calculateExtent(olMap.getSize())
      //
      //
      // let minLng = extent[0]
      // let maxLng = extent[2]
      // let minLat = extent[1]
      // let maxLat = extent[3]
      //
      // let style = new Style({
      //   stroke: new Stroke({
      //     width: 1,
      //     color: 'rgba(65,65,65,0.8)'
      //   })
      // })
      // const source = new VectorSource()
      // let lng = minLng
      // while (lng <= maxLng) {
      //
      //   const points = [[lng, minLat], [lng, maxLat]]
      //   const line = new LineString(points)
      //   const lineFeature = new Feature(line)
      //   lineFeature.setStyle(style)
      //   source.addFeature(lineFeature)
      //   lng += diff
      // }
      //
      // let lat = minLat
      // while (lat <= maxLat) {
      //
      //   const points = [[minLng, lat], [maxLng, lat]]
      //   console.log(points)
      //   const line = new LineString(points)
      //   const lineFeature = new Feature(line)
      //   lineFeature.setStyle(style)
      //   source.addFeature(lineFeature)
      //   lat += diff
      // }
      //
      // const vectorLayer = new VectorLayer({
      //   source: source
      // })
      // olMap.addLayer(vectorLayer)
    },
    startDrawBox(callback) {

      const source = new VectorSource({ wrapX: false })

      const vectorLayer = new VectorLayer({
        source: source,
        style: new Style({
          fill: new Fill({
            color: 'rgba(255, 97, 97, 0.24)'
          }),
          stroke: new Stroke({
            color: 'rgba(255, 97, 97, 0.84)',
            width: 0
          })
        })
      })
      olMap.addLayer(vectorLayer)
      let draw = new Draw({
        source: source,
        type: 'Circle',
        geometryFunction: createBox(),
        style: new Style({
          fill: new Fill({
            color: 'rgba(255, 97, 97, 0.24)'
          }),
          stroke: new Stroke({
            color: 'rgba(255, 97, 97, 0.84)',
            width: 0
          }),
          freehand: true
        })
      })
      olMap.addInteraction(draw)
      // 添加事件
      draw.on('drawstart', function (event) {
        source.clear()
      })
      draw.on('drawend', function (event) {
        let geometry = event.feature.getGeometry()
        let extent = geometry.getExtent()
        let min = toLonLat([extent[0], extent[1]])
        let max = toLonLat([extent[2], extent[3]])

        callback([min[0], min[1], max[0], max[1]])
        draw.abortDrawing()
        olMap.removeInteraction(draw)
        source.clear(true)
        olMap.removeLayer(vectorLayer)
      })
    }
  }
}
</script>

<style>
#mapContainer .zoom-slider {
  width: 14px;
  height: 200px;
  right: 20px;
  bottom: 400px;
  border-bottom: 1px #dfdfdf solid;
  border-right: 1px #dfdfdf solid;
  cursor: pointer;
  background-color: #FFFFFF;
  border-radius: 3px;

}
#mapContainer .zoom-slider button {
  position: relative;
  width: 10px;
  height: 10px;
  border-radius: 5px;
  margin: 0;
  background-color: #606266;
}
</style>

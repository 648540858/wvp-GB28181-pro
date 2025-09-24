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
import Style from 'ol/style/Style'
import Stroke from 'ol/style/Stroke'
import Icon from 'ol/style/Icon'
import View from 'ol/View'
import Feature from 'ol/Feature'
import Overlay from 'ol/Overlay'
import { Point, LineString } from 'ol/geom'
import { get as getProj } from 'ol/proj'
import { containsCoordinate } from 'ol/extent'
import {
  defaults as defaultInteractions
} from 'ol/interaction'
import DragInteraction from './map/DragInteraction'
import { fromLonLat, toLonLat } from './map/TransformLonLat'

import { v4 } from 'uuid'
import { getUid } from 'ol'

let olMap = null

export default {
  name: 'MapComponent',
  props: [],
  data() {
    return {
      overlayId: null,
      dragInteraction: new DragInteraction()
    }
  },
  created() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.init()
      }, 100)
    })
  },
  mounted() {

  },
  destroyed() {
  },
  methods: {
    init() {
      let center = fromLonLat([116.41020, 39.915119])
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
      let tileLayer = null
      if (window.mapParam.tilesUrl) {
        tileLayer = new Tile({
          source: new XYZ({
            projection: getProj('EPSG:3857'),
            wrapX: false,
            tileSize: 256 || window.mapParam.tileSize,
            url: window.mapParam.tilesUrl
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
    },
    setCenter(point) {

    },
    getCenter() {
      return toLonLat(olMap.getView().getCenter())
    },
    zoomIn(zoom) {

    },
    zoomOut(zoom) {

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
        zoom: 12,
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
        console.log(this.overlayId)
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
       * 添加图层
       * @param data
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
       */
    addLayer(data, clickEvent) {
      if (data.length > 0) {
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
        const source = new VectorSource()
        source.addFeatures(features)
        const vectorLayer = new VectorLayer({
          source: source,
          renderMode: 'image',
          declutter: false
        })
        olMap.addLayer(vectorLayer)
        if (typeof clickEvent === 'function') {
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
    updateLayer(layer, data, postponement) {
      console.log(layer)
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
    }
  }
}
</script>

<style>

</style>

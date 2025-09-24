import PointerInteraction from 'ol/interaction/Pointer'
import { toLonLat } from './TransformLonLat'

class DragInteraction extends PointerInteraction {
  constructor() {
    super({
      handleDownEvent: (evt) => {
        const map = evt.map

        const feature = map.forEachFeatureAtPixel(evt.pixel, (feature) => {
          if (this.featureIdMap_.has(feature.getId())) {
            return feature
          }else {
            return null
          }
        })
        if (feature) {
          this.coordinate_ = evt.coordinate
          this.feature_ = feature
          let eventCallback = this.featureIdMap_.get(this.feature_.getId())
          if (eventCallback && eventCallback.startEvent) {
            eventCallback.startEvent(evt)
          }
          return !!feature
        }
      },
      handleDragEvent: (evt) => {
        const deltaX = evt.coordinate[0] - this.coordinate_[0]
        const deltaY = evt.coordinate[1] - this.coordinate_[1]

        const geometry = this.feature_.getGeometry()
        geometry.translate(deltaX, deltaY)

        this.coordinate_[0] = evt.coordinate[0]
        this.coordinate_[1] = evt.coordinate[1]

        let eventCallback = this.featureIdMap_.get(this.feature_.getId())
        if (eventCallback && eventCallback.moveEvent) {
          eventCallback.moveEvent(evt)
        }

      },
      handleMoveEvent: (evt) => {
        if (this.cursor_) {
          const map = evt.map
          const feature = map.forEachFeatureAtPixel(evt.pixel, function(feature) {
            return feature
          })
          const element = evt.map.getTargetElement()
          if (feature) {
            if (element.style.cursor != this.cursor_) {
              this.previousCursor_ = element.style.cursor
              element.style.cursor = this.cursor_
            }
          } else if (this.previousCursor_ !== undefined) {
            element.style.cursor = this.previousCursor_
            this.previousCursor_ = undefined
          }
        }
      },
      handleUpEvent: (evt) => {
        let eventCallback = this.featureIdMap_.get(this.feature_.getId())
        if (eventCallback && eventCallback.endEvent) {
          evt.lonLat = toLonLat(this.feature_.getGeometry().getCoordinates())
          eventCallback.endEvent(evt)
        }
        this.coordinate_ = null
        this.feature_ = null
        return false
      }
    })

    /**
     * @type {import('../src/ol/coordinate.js').Coordinate}
     * @private
     */
    this.coordinate_ = null

    /**
     * @type {string|undefined}
     * @private
     */
    this.cursor_ = 'pointer'

    /**
     * @type {Feature}
     * @private
     */
    this.feature_ = null

    /**
     * @type {string|undefined}
     * @private
     */
    this.previousCursor_ = undefined

    this.featureIdMap_ = new Map()

    this.addFeatureId = (id, moveEndEvent) => {
      if (this.featureIdMap_.has(id)) {
        return
      }
      this.featureIdMap_.set(id, moveEndEvent)
    }

    this.removeFeatureId= (id) => {
      this.featureIdMap_.delete(id)
    }

    this.hasFeatureId= (id) => {
      this.featureIdMap_.has(id)
    }
  }
}

export default DragInteraction

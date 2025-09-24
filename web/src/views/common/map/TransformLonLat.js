import { fromLonLat as projFromLonLat, toLonLat as projToLonLat } from 'ol/proj'
import gcoord from 'gcoord'

export function fromLonLat(coordinate) {
  return projFromLonLat(gcoord.transform(coordinate, gcoord.WGS84, gcoord.GCJ02))
}
export function toLonLat(coordinate) {
  return gcoord.transform(projToLonLat(coordinate), gcoord.GCJ02, gcoord.WGS84)
}

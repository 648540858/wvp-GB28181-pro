import request from '@/utils/request'

// 前端控制

export function setSpeedForScan([deviceId, channelDeviceId, scanId, speed]) {
  return request({
    method: 'get',
    url: `/api/front-end/scan/set/speed/${deviceId}/${channelDeviceId}`,
    params: {
      scanId: scanId,
      speed: speed
    }
  })
}

export function setLeftForScan([deviceId, channelDeviceId, scanId]) {
  return request({
    method: 'get',
    url: `/api/front-end/scan/set/left/${deviceId}/${channelDeviceId}`,
    params: {
      scanId: scanId
    }
  })
}

export function setRightForScan([deviceId, channelDeviceId, scanId]) {
  return request({
    method: 'get',
    url: `/api/front-end/scan/set/right/${deviceId}/${channelDeviceId}`,
    params: {
      scanId: scanId
    }

  })
}

export function startScan([deviceId, channelDeviceId, scanId]) {
  return request({
    method: 'get',
    url: `/api/front-end/scan/start/${deviceId}/${channelDeviceId}`,
    params: {
      scanId: scanId
    }
  })
}

export function stopScan([deviceId, channelDeviceId, scanId]) {
  return request({
    method: 'get',
    url: `/api/front-end/scan/stop/${deviceId}/${channelDeviceId}`,
    params: {
      scanId: scanId
    }

  })
}

export function queryPreset([deviceId, channelDeviceId]) {
  return request({
    method: 'get',
    url: `/api/front-end/preset/query/${deviceId}/${channelDeviceId}`
  })
}

export function addPointForCruise([deviceId, channelDeviceId, cruiseId, presetId]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/point/add/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId,
      presetId: presetId
    }
  })
}

export function deletePointForCruise([deviceId, channelDeviceId, cruiseId, presetId]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/point/delete/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId,
      presetId: presetId
    }
  })
}

export function setCruiseSpeed([deviceId, channelDeviceId, cruiseId, cruiseSpeed]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/speed/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId,
      speed: cruiseSpeed
    }
  })
}

export function setCruiseTime([deviceId, channelDeviceId, cruiseId, cruiseTime]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/time/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId,
      time: cruiseTime
    }
  })
}

export function startCruise([deviceId, channelDeviceId, cruiseId]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/start/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId
    }
  })
}

export function stopCruise([deviceId, channelDeviceId, cruiseId]) {
  return request({
    method: 'get',
    url: `/api/front-end/cruise/stop/${deviceId}/${channelDeviceId}`,
    params: {
      cruiseId: cruiseId
    }
  })
}

export function addPreset([deviceId, channelDeviceId, presetId]) {
  return request({
    method: 'get',
    url: `/api/front-end/preset/add/${deviceId}/${channelDeviceId}`,
    params: {
      presetId: presetId
    }
  })
}

export function callPreset([deviceId, channelDeviceId, presetId]) {
  return request({
    method: 'get',
    url: `/api/front-end/preset/call/${deviceId}/${channelDeviceId}`,
    params: {
      presetId: presetId
    }
  })
}

export function deletePreset([deviceId, channelDeviceId, presetId]) {
  return request({
    method: 'get',
    url: `/api/front-end/preset/delete/${deviceId}/${channelDeviceId}`,
    params: {
      presetId: presetId
    }
  })
}

/**
 * command: on 开启， off 关闭
 */
export function auxiliary([deviceId, channelDeviceId, command, switchId]) {
  return request({
    method: 'get',
    url: `/api/front-end/auxiliary/${deviceId}/${channelDeviceId}`,
    params: {
      command: command,
      switchId: switchId
    }
  })
}
/**
 * command: on 开启， off 关闭
 */
export function wiper([deviceId, channelDeviceId, command]) {
  return request({
    method: 'get',
    url: `/api/front-end/wiper/${deviceId}/${channelDeviceId}`,
    params: {
      command: command
    }
  })
}

export function ptz([deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed]) {
  return request({
    method: 'get',
    url: `/api/front-end/ptz/${deviceId}/${channelId}`,
    params: {
      command: command,
      horizonSpeed: horizonSpeed,
      verticalSpeed: verticalSpeed,
      zoomSpeed: zoomSpeed
    }
  })
}

export function iris([deviceId, channelId, command, speed]) {
  return request({
    method: 'get',
    url: `/api/front-end/fi/iris/${deviceId}/${channelId}`,
    params: {
      command: command,
      speed: speed
    }
  })
}

export function focus([deviceId, channelDeviceId, command, speed]) {
  return request({
    method: 'get',
    url: `/api/front-end/fi/focus/${deviceId}/${channelDeviceId}`,
    params: {
      command: command,
      speed: speed
    }
  })
}

import request from '@/utils/request'

export function pageMotorVehicle(data) {
    return request({
        url: '/api/viid/motorvehicles/page',
        method: 'get',
        params: data
    })
}

export function addMotorVehicle(data) {
    return request({
        url: '/api/viid/motorvehicles',
        method: 'post',
        data: data
    })
}

export function updateMotorVehicle(data) {
    return request({
        url: '/api/viid/motorvehicles',
        method: 'put',
        data: data
    })
}

export function getMotorVehicle(id) {
    return request({
        url: '/api/viid/motorvehicles/' + id,
        method: 'get'
    })
}

export function delMotorVehicles(ids) {
    return request({
        url: '/api/viid/motorvehicles/' + ids,
        method: 'delete'
    })
}

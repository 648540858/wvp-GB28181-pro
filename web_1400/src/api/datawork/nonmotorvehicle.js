import request from '@/utils/request'

export function pageNonMotorVehicle(data) {
    return request({
        url: '/api/viid/nonmotorvehicles/page',
        method: 'get',
        params: data
    })
}

export function addNonMotorVehicle(data) {
    return request({
        url: '/api/viid/nonmotorvehicles',
        method: 'post',
        data: data
    })
}

export function updateNonMotorVehicle(data) {
    return request({
        url: '/api/viid/nonmotorvehicles',
        method: 'put',
        data: data
    })
}

export function getNonMotorVehicle(id) {
    return request({
        url: '/api/viid/nonmotorvehicles/' + id,
        method: 'get'
    })
}

export function delNonMotorVehicles(ids) {
    return request({
        url: '/api/viid/nonmotorvehicles/' + ids,
        method: 'delete'
    })
}

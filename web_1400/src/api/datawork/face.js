import request from '@/utils/request'

export function pageFace(data) {
    return request({
        url: '/api/viid/faces/page',
        method: 'get',
        params: data
    })
}

export function addFace(data) {
    return request({
        url: '/api/viid/faces',
        method: 'post',
        data: data
    })
}

export function updateFace(data) {
    return request({
        url: '/api/viid/faces',
        method: 'put',
        data: data
    })
}

export function getFace(id) {
    return request({
        url: '/api/viid/faces/' + id,
        method: 'get'
    })
}

export function delFaces(ids) {
    return request({
        url: '/api/viid/faces/' + ids,
        method: 'delete'
    })
}

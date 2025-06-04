import request from '@/utils/request'

export function pagePerson(data) {
    return request({
        url: '/api/viid/persons/page',
        method: 'get',
        params: data
    })
}

export function addPerson(data) {
    return request({
        url: '/api/viid/persons',
        method: 'post',
        data: data
    })
}

export function updatePerson(data) {
    return request({
        url: '/api/viid/persons',
        method: 'put',
        data: data
    })
}

export function getPerson(id) {
    return request({
        url: '/api/viid/persons/' + id,
        method: 'get'
    })
}

export function delPersons(ids) {
    return request({
        url: '/api/viid/persons/' + ids,
        method: 'delete'
    })
}

import request from '@/utils/request'

export function getBasicTypeOptions() {
    return request({
        url: '/api/dwn/common/basic/type/options',
        method: 'get'
    })
}

export function getBasicFormatOptions(type) {
    return request({
        url: '/api/dwn/common/basic/format/options',
        method: 'get',
        params: { type: type || '' }
    })
}

export function sqlParseColumns(data) {
    return request({
        url: '/api/dwn/common/sql/parse/columns',
        method: 'post',
        data: data
    })
}

export function getJobSelectOptions(params) {
    return request({
        url: '/api/dwn/common/job/options',
        method: 'get',
        params: params
    })
}

export function getDbTableNames(params) {
    return request({
        url: '/api/dwn/common/db/tables',
        method: 'get',
        params: params
    })
}

export function getDbTableColumnNames(params) {
    return request({
        url: '/api/dwn/common/db/columns',
        method: 'get',
        params: params
    })
}

export function metadataMark(data) {
    return request({
        url: '/api/dwn/metadata/mark',
        method: 'post',
        data: data
    })
}
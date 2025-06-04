import request from '@/utils/request'

export function viidMetrics() {
    return request({
        url: '/api/admin/viid/metrics',
        method: 'get',
        params: {s: new Date().getTime()}
    })
}


import { rewriteStyleOrigin, rewriteUrlOrigin } from '@/utils/mapStyle.js'

const STYLE_URL = 'https://192.168.1.55:3003/style/default'
const ORIGIN = 'https://192.168.1.55:3003'

describe('Utils:mapStyle rewriteStyleOrigin', () => {
  it('重写绝对 tiles 地址，保留 {z}/{x}/{y} 模板', () => {
    const style = {
      version: 8,
      sources: {
        base: { type: 'vector', tiles: ['http://192.168.1.66:3000/base/{z}/{x}/{y}'] }
      },
      layers: []
    }
    const result = rewriteStyleOrigin(style, STYLE_URL)
    expect(result.sources.base.tiles).toEqual([`${ORIGIN}/base/{z}/{x}/{y}`])
  })

  it('外网绝对地址只替换协议/IP/端口，保留 path 和 query', () => {
    const style = {
      version: 8,
      sources: {
        satellite: {
          type: 'raster',
          tiles: ['https://webst01.is.autonavi.com:443/appmaptile?x={x}&y={y}&z={z}&style=6']
        }
      },
      layers: []
    }
    const result = rewriteStyleOrigin(style, 'https://192.168.1.55:3005/satellite.json')
    expect(result.sources.satellite.tiles).toEqual([
      'https://192.168.1.55:3005/appmaptile?x={x}&y={y}&z={z}&style=6'
    ])
  })

  it('重写 TileJSON url 和 GeoJSON data', () => {
    const style = {
      version: 8,
      sources: {
        tilejson: { type: 'vector', url: 'http://192.168.1.66:3000/tiles/base.json' },
        geojson: { type: 'geojson', data: 'http://192.168.1.66:3000/data/a.geojson' }
      },
      layers: []
    }
    const result = rewriteStyleOrigin(style, STYLE_URL)
    expect(result.sources.tilejson.url).toBe(`${ORIGIN}/tiles/base.json`)
    expect(result.sources.geojson.data).toBe(`${ORIGIN}/data/a.geojson`)
  })

  it('以样式地址为基准解析相对地址', () => {
    expect(rewriteUrlOrigin('tiles/a.json', STYLE_URL, ORIGIN))
      .toBe(`${ORIGIN}/style/tiles/a.json`)
    expect(rewriteUrlOrigin('../tiles/a.json', STYLE_URL, ORIGIN))
      .toBe(`${ORIGIN}/tiles/a.json`)
    expect(rewriteUrlOrigin('/absolute/{z}/{x}/{y}', STYLE_URL, ORIGIN))
      .toBe(`${ORIGIN}/absolute/{z}/{x}/{y}`)
    expect(rewriteUrlOrigin('tiles/a.json?token=1#part', STYLE_URL, ORIGIN))
      .toBe(`${ORIGIN}/style/tiles/a.json?token=1#part`)
  })

  it('处理协议相对地址', () => {
    expect(rewriteUrlOrigin('//192.168.1.66:3000/base/{z}/{x}/{y}', STYLE_URL, ORIGIN))
      .toBe(`${ORIGIN}/base/{z}/{x}/{y}`)
  })

  it('重写 sprite（字符串与数组）和 glyphs', () => {
    const style = {
      version: 8,
      sources: {},
      sprite: 'http://192.168.1.66:3000/sprite/default',
      glyphs: 'http://192.168.1.66:3000/fonts/{fontstack}/{range}.pbf',
      layers: []
    }
    const result = rewriteStyleOrigin(style, STYLE_URL)
    expect(result.sprite).toBe(`${ORIGIN}/sprite/default`)
    expect(result.glyphs).toBe(`${ORIGIN}/fonts/{fontstack}/{range}.pbf`)

    const multi = rewriteStyleOrigin({
      version: 8,
      sources: {},
      sprite: [
        { id: 'default', url: 'http://192.168.1.66:3000/sprite/default' },
        'http://192.168.1.66:3000/sprite/extra'
      ],
      layers: []
    }, STYLE_URL)
    expect(multi.sprite).toEqual([
      { id: 'default', url: `${ORIGIN}/sprite/default` },
      `${ORIGIN}/sprite/extra`
    ])
  })

  it('跳过 data: / blob: 等非 http 地址', () => {
    expect(rewriteUrlOrigin('data:application/json,{}', STYLE_URL, ORIGIN))
      .toBe('data:application/json,{}')
    expect(rewriteUrlOrigin('blob:https://a/b', STYLE_URL, ORIGIN))
      .toBe('blob:https://a/b')
    expect(rewriteUrlOrigin('pmtiles://bucket/a.pmtiles', STYLE_URL, ORIGIN))
      .toBe('pmtiles://bucket/a.pmtiles')
  })

  it('不修改传入的样式对象', () => {
    const style = {
      version: 8,
      sources: { base: { type: 'vector', tiles: ['http://192.168.1.66:3000/base/{z}/{x}/{y}'] } },
      layers: []
    }
    rewriteStyleOrigin(style, STYLE_URL)
    expect(style.sources.base.tiles).toEqual(['http://192.168.1.66:3000/base/{z}/{x}/{y}'])
  })

  it('样式或地址缺失时原样返回', () => {
    expect(rewriteStyleOrigin(null, STYLE_URL)).toBe(null)
    expect(rewriteStyleOrigin({ version: 8 }, '')).toEqual({ version: 8 })
  })
})

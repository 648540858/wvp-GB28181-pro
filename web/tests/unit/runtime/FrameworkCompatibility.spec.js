import fs from 'fs'
import path from 'path'

const packageJson = require('../../../package.json')

describe('frontend framework compatibility', () => {
  it('uses Vue 3, Vite and Ant Design Vue', () => {
    expect(packageJson.dependencies.vue).toMatch(/^\^3\./)
    expect(packageJson.dependencies['ant-design-vue']).toBeDefined()
    expect(packageJson.devDependencies.vite).toBeDefined()
  })

  it('does not retain Vue 2-only packages or source APIs', () => {
    const removedPackages = [
      'element-ui',
      'v-charts',
      '@wchbrad/vue-easy-tree',
      '@femessage/log-viewer',
      'vue-clipboard2',
      'vue-contextmenujs'
    ]
    removedPackages.forEach(name => {
      expect(packageJson.dependencies[name]).toBeUndefined()
      expect(packageJson.devDependencies[name]).toBeUndefined()
    })

    const srcDir = path.resolve(__dirname, '../../../src')
    const matches = []
    const forbidden = [
      /from\s+['"](?:element-ui|v-charts|@wchbrad\/vue-easy-tree|@femessage\/log-viewer|vue-clipboard2|vue-contextmenujs)['"]/,
      /\bVue\.use\(/,
      /\bnew Vue\(/,
      /\bthis\.\$set\(/,
      /\bthis\.\$destroy\(/,
      /\$listeners\b/,
      /\bslot-scope\s*=/,
      /\.native\b/,
      /\.sync\s*=/
    ]

    const scan = directory => {
      fs.readdirSync(directory, { withFileTypes: true }).forEach(entry => {
        const file = path.join(directory, entry.name)
        if (entry.isDirectory()) {
          scan(file)
        } else if (/\.(?:js|vue)$/.test(entry.name)) {
          fs.readFileSync(file, 'utf8').split(/\r?\n/).forEach((line, index) => {
            if (forbidden.some(pattern => pattern.test(line))) {
              matches.push(`${path.relative(srcDir, file)}:${index + 1}`)
            }
          })
        }
      })
    }

    scan(srcDir)
    expect(matches).toEqual([])
  })
})

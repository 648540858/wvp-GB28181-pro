/**
 * constrcut 方法
 * 根据提供的 id, pid 和 children 将一个个节点构建成一棵或者多棵树
 * @param nodes 节点对象
 * @param config 配置对象
 */
export function construct(nodes, config) {
  const id = (config && config.id) || 'id'
  const pid = (config && config.pid) || 'pid'
  const children = (config && config.children) || 'children'
  const idMap = {}
  const jsonTree = []
  nodes.forEach(v => { idMap[v[id]] = v })
  nodes.forEach(v => {
    const parent = idMap[v[pid]]
    if (parent) {
      !parent[children] && (parent[children] = [])
      parent[children].push(v)
    } else {
      jsonTree.push(v)
    }
  })
  return jsonTree
}

/**
 * destruct 方法
 * 根据配置的 id, pid 和 children 把解构化的树型对象拆解为一个个节点
 * @param forest 单个或者多个树型对象
 * @param config 配置
 */
export function destruct(nodes, config) {
  const id = (config && config.id) || 'id'
  const pid = (config && config.pid) || 'pid'
  const children = (config && config.children) || 'children'
  function flatTree(tree) {
    const queue = [tree]
    const result = []
    while (queue.length) {
      let currentNode = queue.shift()
      // eslint-disable-next-line no-prototype-builtins
      if (currentNode.hasOwnProperty(id)) {
        // eslint-disable-next-line no-prototype-builtins
        if (!currentNode.hasOwnProperty(pid)) {
          currentNode = { ...currentNode, [pid]: null }
        }
        if (currentNode[children]) {
          currentNode[children].forEach((v) => { v && queue.push({ ...v, [pid]: currentNode[id] }) })
        }
        result.push(currentNode)
        delete currentNode[children]
      } else {
        throw new Error('you need to specify the [id] of the json tree')
      }
    }
    return result
  }

  if (Array.isArray(nodes)) {
    return nodes.map((v) => flatTree(v)).reduce((pre, cur) => pre.concat(cur))
  } else {
    return flatTree(nodes)
  }
}

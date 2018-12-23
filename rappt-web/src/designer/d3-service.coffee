angular.module 'rappt.io.d3-service', [
  'rappt.io.graph-service'
]

.factory 'd3Layout', (graphService) ->
  nodes = []
  links = []

  layout: () ->
    @transformFromGraph()
    force = d3.layout.force()
    .nodes(nodes)
    .links(links)
    .size([document.body.clientWidth - 100, document.body.clientHeight - 100])
    .linkDistance 200
    .on 'tick', =>
      q = d3.geom.quadtree(nodes)
      for n in nodes
        q.visit(@collide(n))
    force.start()
    force.tick() for i in [1..1000000]
    force.stop()
    @transformToGraph()
    # .size([width, height]);

  # Transform our visual graph to a D3.js friendly graph
  transformFromGraph: () ->
    graph = graphService.getGraph()
    for graphNode in graph.nodes
      node =
        attr: graphNode
        id: graphNode.id
        radius: graphNode.width / 2 + 20
      nodes.push node

    for graphLink in graph.links
      link =
        source: graphService.findNodeIndex(graphLink.sourceID)
        target: graphService.findNodeIndex(graphLink.targetID)
      links.push link

  transformToGraph: () ->
    graphNodes = []
    for node in nodes
      graphService.updateNode(node.id, node.x, node.y)

    # graphService.setNodes(nodes)
    nodes = []
    links = []

  collide: (node) ->
    r = node.radius + 16
    nx1 = node.x - r
    nx2 = node.x + r
    ny1 = node.y - r
    ny2 = node.y + r
    return (quad, x1, y1, x2, y2) ->
      if (quad.point && (quad.point != node))
        x = node.x - quad.point.x
        y = node.y - quad.point.y
        l = Math.sqrt(x * x + y * y)
        r = node.radius + quad.point.radius
        if (l < r)
          l = (l - r) / l * .5
          node.x -= x *= l
          node.y -= y *= l
          quad.point.x += x
          quad.point.y += y
      return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1

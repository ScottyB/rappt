angular.module 'rappt.io.graph-service',[
  'rappt.io.project'
]

# Stores Visual Model
.factory 'graphService', (projectService) ->
  class GraphService
    graph =
      links: []
      nodes: []
    nodeNameHeight = 10
    connectorHeight = 60
    connectorWidth = 10

    linkIdCounter = 0

    loadData: () ->
      graph.nodes = []
      graph.links = []
      @addNode(screen.id, 100,100) for screen in projectService.model.screens
      for screen in projectService.model.screens
        for btn in screen.buttons
          @addLink @findNode(btn.from.id), @findNode(btn.to.id)

    newLink: (sourceNode, targetNode, linkType = 'screenTransitionLink') ->
      @addLink sourceNode, targetNode, linkType
      projectService.model
      screen = projectService.model.screen sourceNode.id
      screen.addButton targetNode
      projectService.dirty('visual')

    addLink: (sourceNode, targetNode, linkType) ->
      sourcePoint = @computeConnectorPos(sourceNode, 1)
      targetPoint = @computeConnectorPos(targetNode, 1, true)
      link =
        id: "link_#{linkIdCounter++}"
        linkType: linkType
        sourceID: sourceNode.id
        targetID: targetNode.id
        points: @bezierPoints(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y)
      graph.links.push link

    findNode: (id) -> graph.nodes.find (node) -> node.id is id
    findNodeIndex: (id) ->
      for node, i in graph.nodes
        if node.id is id
          return i

    updateNode: (id, x, y, isDelta) ->
      node = @findNode id
      node.x = if isDelta then node.x + x else x
      node.y = if isDelta then node.y + y else y
      @updateNodeLinks node

    updateNodeLinks: (node) ->
    # todo optimise
      for link in graph.links
        if link.targetID is node.id
          sourceNode = @findNode link.sourceID
          sourcePoint = @computeConnectorPos(sourceNode, 1)
          targetPoint = @computeConnectorPos(node, 1, true)
          link.points = @bezierPoints(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y)
        if link.sourceID is node.id
          sourcePoint = @computeConnectorPos(node, 1)
          targetNode = @findNode link.targetID
          targetPoint = @computeConnectorPos(targetNode, 1, true)
          link.points = @bezierPoints(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y)

    addNode: (id, x, y) ->
      width = 140
      node =
        id: id
        svgId: "svg-#{id}"
        htmlId: "html-#{id}"
        x: x
        y: y
        width: width
        height: 90 # todo: make variable and set dom height
        inConnectors: @createConnectorsView("From", 0)
        outConnectors: @createConnectorsView("Navigate To", width)
      graph.nodes.push node
      return node

    newScreen: (x, y) ->
      id = projectService.model.freshId()
      node = @addNode id, x, y
      projectService.model.addScreen id: id
      projectService.dirty('visual')
      return node

    setNodes: (nodes) ->
      graph.nodes = nodes
      graph.links = graph.links

    isBlankGraph: () ->
      Object.keys(graph.nodes).length == 0 and Object.keys(graph.links).length == 0

    getGraph: () ->
      if not projectService.hasSavedData() and not @isBlankGraph()
        graph =
          nodes : []
          links : []
      graph

    computeConnectorY: (connectorIndex) ->
      nodeNameHeight + connectorIndex * connectorHeight

    createConnectorsView: (name, xIndex, parentNode) ->
      connectors = []
      x = if (xIndex == 0) then (xIndex - 10) else (xIndex + 10)
      connector =
        name: name
        x: x
        y: @computeConnectorY(1)
        r: connectorWidth
      connectors.push connector
      connectors

    computeConnectorPos: (node, connectorIndex, inputConnector) ->
      point =
        x: node.x + if inputConnector then 0 - connectorWidth else node.width + connectorWidth
        y: node.y + @computeConnectorY(connectorIndex)

    updateSourcePoints: (link, x1, y1) ->
      offset = Math.abs(x1 - link.points.x2) / 2
      link.points.x1 = x1
      link.points.y1 = y1
      link.points.cx1 = x1 + offset
      link.points.cy1 = y1

    updateTargetPoints: (link, x2, y2) ->
      offset = Math.abs (link.points.x1 - x2) / 2
      link.points.x2 = x2
      link.points.y2 = y2
      link.points.cx2 = x2 - offset
      link.points.cy2 = y2

    bezierPoints: (x1, y1, x2, y2) ->
      offset = Math.abs(x1 - x2) / 2
      bezier =
        x1: x1
        y1: y1
        cx1: x1 + offset
        cy1: y1
        x2: x2
        y2: y2
        cx2: x2 - offset
        cy2: y2

  return new GraphService()

#     _selectedNode: null
#     _focused: false
#     mouseGraphX: 0
#     mouseGraphY: 0

#     constructor: ->
#       @dispatch = d3.dispatch 'mouseLinkUp', 'nodeUp', 'linkRemoved', 'nodeDown'

#     # elm is the DOM element to put the graph in
#     element: (elm) ->
#       if not elm?
#         return @_element

#       @_element = elm
#       elm.addEventListener 'blur', =>
#         @_focused = false
#         @selectedNode null

#       @_g = nodeLinkGraphFactory elm, @_getNodeVisualProperties, @_getIconVisualProperties
#       @_g.collision = yes
#       @_g.dispatch.on 'nodeMouseUp', (node) =>
#         @focus()
#         @dispatch.nodeUp node
#       @_g.dispatch.on 'nodeMouseDown', (node) =>
#         @focus()
#         @dispatch.nodeDown node
#       @_g.dispatch.on 'canvasMouseDown', => @focus()
#       @_initMouseLinking()

#     _initMouseLinking: ->
#       @_mouseTrackingNode = null
#       @_g.dispatch.on 'mouseMove', (point) =>
#         [@mouseGraphX, @mouseGraphY] = point
#         if @_mouseTrackingNode?
#           [@_mouseTrackingNode.px, @_mouseTrackingNode.py] = point
#           @_g.updatePositions()

#       @_g.dispatch.on 'linkMouseUp', (link) =>
#         @focus()

#         # Removes the button if you click on the link
#         if not @_mouseTrackingNode?
#           @removeLink link.source, link
#           @dispatch.linkRemoved {}

#         if link.target.nodeType is '_mouse'
#           mouseNode = @_element.querySelector('image[node-type="_mouse"]').parentElement
#           ebounds = mouseNode.getBoundingClientRect()

#           if (node = @nodeAt ebounds.left, ebounds.top, mouseNode)?
#             source = @_mouseTrackingNode.mouseLink.source
#             return if node is source # Don't link to self
#             @stopMouseLinking()
#             @dispatch.mouseLinkUp {source: source, target: node}

#     # ex and ey are relative to the viewport
#     nodeAt: (ex, ey, ignore = null) ->
#       nodeElements = Array::slice.call @_element.querySelectorAll('g.node')
#       if ignore?
#         nodeElements.splice nodeElements.indexOf(ignore), 1

#       hitElem = nodeElements.find (e) ->
#         bounds = e.getBoundingClientRect()
#         return bounds.left < ex < bounds.right and
#                bounds.top  < ey < bounds.bottom

#       if hitElem?
#         @_g.findNode hitElem.id[5..]  # Strip "node_"
#       else
#         null

#     # Translate client (viewport) x coord to graph coord
#     clientToGraphX: (clientX) -> clientX - @_element.getBoundingClientRect().left
#     # Translate client (viewport) y coord to graph coord
#     clientToGraphY: (clientY) -> clientY - @_element.getBoundingClientRect().top

#     startMouseLinking: (node) ->
#       return if @_mouseTrackingNode?

#       @_mouseTrackingNode = @addNode '_mouse'
#       @_mouseTrackingNode.mouseLink = @addLink node, @_mouseTrackingNode

#     stopMouseLinking: ->
#       return if not @_mouseTrackingNode?
#       @_g.removeLink @_mouseTrackingNode.mouseLink.id
#       @_g.removeNode @_mouseTrackingNode.id
#       @_mouseTrackingNode = null

#     node: (id) => @_g.findNode id

#     addNode: (name, attrs = {}, {x,y} = {}) ->
#       x ?= @mouseGraphX
#       y ?= @mouseGraphY
#       @_g.addNode x, y, attrs, name, attrs.id

#     removeNode: (node) ->
#       @_selectedNode = null if node is @_selectedNode
#       @_g.removeNode node.id

#     selectedNode: (node) ->
#       return @_selectedNode if typeof node is 'undefined'
#       if @_selectedNode?
#         document.getElementById("node_#{@_selectedNode.id}").setAttribute 'filter', null
#       @_selectedNode = node
#       if node?
#         document.getElementById("node_#{node.id}").setAttribute 'filter', 'url(#glow)'

#     addIcon: (node, name, attrs = {}) -> @_g.addIcon node, attrs, name

#     addLink: (source, target, linkType = 'screenTransitionLink') ->
#       @_g.addLink source, target, {}, linkType

#     removeLink: (sourceNode, linkToRemove) ->
#       sourceNode.attributes.removeButton linkToRemove.id
#       @_g.removeLink linkToRemove.id


#     clear: -> @_g.removeNode node.id for node in @_g.data().nodes

#     update: -> @_g.updateAll()

#     reposition: => @_g.reposition 0.05, 200

#     focus: =>
#       @_element.focus()
#       @_focused = true

#     # Analyses the node's attributes and returns a list of visual properties that can be bound
#     # to the node DOM element.
#     _getNodeVisualProperties: (nodeType, attributes) ->
#       if nodeType is '_mouse'
#         return {
#           radius: -1
#           width: 0
#           height: 0
#           textProperties: {}
#         }

#       nodeProperties = rapptModelDef.nodes[nodeType]
#       if not nodeProperties?
#         console.error "Node type #{nodeType} is not defined in rapptModelDef."
#         return undefined

#       return nodeProperties.visualProperties

#     _getIconVisualProperties: (iconType, attributes) ->
#       rapptModelDef.components[iconType].visualProperties

#   return new GraphService()
# ]

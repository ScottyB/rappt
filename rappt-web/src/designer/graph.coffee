angular.module 'rappt.io.graph', [
  'rappt.io.graph-service'
  'rappt.io.draggable'
  'rappt.io.d3-service'
]

.directive 'graph', ($compile, graphService, d3Layout) ->
  restrict: 'E'
  templateUrl: 'designer/graph.html'
  replace: true
  controller: ($scope) ->
    $scope.isDraggingConnection = false
    $scope.draggingConnection = {}
    $scope.draggingNode

    $scope.links = []
    $scope.nodes = []

    graphService.loadData()
    d3Layout.layout()

    $scope.connectorMouseUp = (event, connector, node) ->
      if $scope.isDraggingConnection
        graphService.newLink $scope.draggingNode, node
      $scope.stopConnecting()
      return false

    $scope.stopConnecting = () ->
      $scope.isDraggingConnection = false
      $scope.draggingNode = {}
      $scope.draggingConnection = {}

    $scope.connectorMouseDown = (event, connector, node) ->
      point = graphService.computeConnectorPos(node, 1, false)
      $scope.draggingConnection =
        x1: point.x
        y1: point.y
      $scope.draggingNode = node
      $scope.isDraggingConnection = true

    $scope.connectorMouseMove = (event) ->
      if $scope.isDraggingConnection
        x = $scope.draggingConnection.x1
        y = $scope.draggingConnection.y1
        $scope.draggingConnection = graphService.bezierPoints(x,y, event.clientX, event.clientY)


  link: (scope, element, attrs) ->
    scope.$watch((() -> graphService.getGraph()), (value) ->
      scope.nodes = value.nodes
      scope.links = value.links
    )

    scope.mouseUp = (event) ->
      if scope.isDraggingConnection
        node = graphService.newScreen event.clientX, event.clientY
        graphService.newLink scope.draggingNode, node
      scope.stopConnecting()


    # Depends upon parent
.directive 'node', (graphService, dragging, modelManipulator) ->
  templateUrl: 'designer/node.html'
  scope:
    id: '@'
    screenId: '@'
  controller: ($scope) ->
    $scope.screen = modelManipulator.screen $scope.screenId
    $scope.lastMouseCoords = {}

  link: (scope, elem, attrs) ->
    scope.onMouseDown = (event) ->
      dragging.startDrag(event,
        dragStarted: (x, y) ->
          scope.lastMouseCoords.x = x
          scope.lastMouseCoords.y = y
        dragging: (x, y) ->
          deltaX = x - scope.lastMouseCoords.x
          deltaY = y - scope.lastMouseCoords.y
          graphService.updateNode(scope.screenId, deltaX, deltaY, true)
          scope.lastMouseCoords.x = x
          scope.lastMouseCoords.y = y
      )

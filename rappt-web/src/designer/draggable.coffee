# From: http://www.codeproject.com/Articles/709340/Implementing-a-Flowchart-with-SVG-and-AngularJS

angular.module 'rappt.io.draggable', []

.factory 'mouseCapture', ($rootScope) ->

  $element = document
  mouseCaptureConfig = null
  mouseMove = (evt) ->
    if (mouseCaptureConfig && mouseCaptureConfig.mouseMove)
      mouseCaptureConfig.mouseMove(evt)
      $rootScope.$digest()

  mouseUp = (evt) ->
    if (mouseCaptureConfig && mouseCaptureConfig.mouseUp)
      mouseCaptureConfig.mouseUp(evt)
      $rootScope.$digest()

  registerElement: (element) ->
    $element = element

  acquire: (evt, config) ->
    @release()
    mouseCaptureConfig = config
    $element.on 'mousemove', mouseMove
    $element.on 'mouseup', mouseUp

  release: () ->
    if (mouseCaptureConfig)
      if (mouseCaptureConfig.released)
        mouseCaptureConfig.released()
      mouseCaptureConfig = null

    $element.unbind("mousemove", mouseMove)
    $element.unbind("mouseup", mouseUp)

.directive 'mouseCapture', () ->
  restrict: 'A',
  controller: ($scope, $element, $attrs, mouseCapture) ->
    mouseCapture.registerElement($element);

.factory 'dragging', ($rootScope, mouseCapture) ->
  threshold = 5
  startDrag: (evt, config) ->
    dragging = false
    x = evt.pageX
    y = evt.pageY

    mouseMove = (evt) ->
      if (!dragging)
        if (evt.pageX - x > threshold || evt.pageY - y > threshold)
          dragging = true
          if (config.dragStarted)
            config.dragStarted(x, y, evt)

          if (config.dragging)
            config.dragging(evt.pageX, evt.pageY, evt)
      else
        if (config.dragging)
          config.dragging(evt.pageX, evt.pageY, evt)
        x = evt.pageX
        y = evt.pageY

    released = () ->
      if (dragging)
        if (config.dragEnded)
          config.dragEnded()
      else
        if (config.clicked)
          config.clicked()

    mouseUp = (evt) ->
      mouseCapture.release()
      evt.stopPropagation()
      evt.preventDefault()

    mouseCapture.acquire(evt,
        mouseMove: mouseMove,
        mouseUp: mouseUp,
        released: released,
    )

    evt.stopPropagation()
    evt.preventDefault()

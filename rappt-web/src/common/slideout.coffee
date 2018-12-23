angular.module 'rappt.io.slideout', []

.directive 'slideout', ->
  restrict: 'E',
  scope:
    title: '@slideoutTitle'
    width: '@slideoutWidth'
  transclude: true
  templateUrl: 'common/slideout.html'
  link: (scope, element) ->
    SLIDEOUT_TAB_WIDTH = "5ex" # Matches slideout.less
    scope.width ?= "710px"

    element[0].querySelector('.slideout-content').style.width = scope.width

    slideoutActive = false
    scope.toggleSlideout = ->
      slideoutActive = !slideoutActive
      if slideoutActive
        element.addClass 'slideout-active'
        element[0].style.width = "calc(#{scope.width} + #{SLIDEOUT_TAB_WIDTH})"
        element[0].querySelector('.slideout-tab').style.right = scope.width
      else
        element.removeClass 'slideout-active'
        element[0].style.width = ''
        element[0].querySelector('.slideout-tab').style.right = ''

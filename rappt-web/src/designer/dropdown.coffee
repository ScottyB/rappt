angular.module 'rappt.io.dropdown', [
]

.directive 'dropDown', ->
  restrict: 'E'
  templateUrl: 'designer/dropdown.html'
  scope:
    items: '='
    callback: '='
  controller: ['$scope', ($scope) ->
    $scope.isVisible = false

    $scope.makeChoice = ($event, choice) ->
      $scope.isVisible = false
      $scope.callback($event, choice)
  ]
  link: (scope, element, attrs) ->

    element.parent().bind('click', (ev) ->
    # todo: Handle corner locations
      scope.isVisible = false
      scope.$apply()
      )

    element.parent().bind('contextmenu', (ev) ->
      element.css('left',ev.clientX - 5 + 'px')
      element.css('top', ev.clientY - 5 + 'px')
      scope.isVisible = true
      scope.$apply()
      ev.preventDefault()
      return false
      )

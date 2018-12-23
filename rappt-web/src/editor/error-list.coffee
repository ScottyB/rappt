angular.module 'rappt.io.error-list', []

.directive 'errorList', ->
  restrict: 'E'
  scope:
    project: '=project'
    goToError: '=goToError'
    viewType: '@viewType'
  replace: true
  templateUrl: 'editor/error-list.html'
  controller: ['$scope', ($scope) ->
    _errorListVisible = true
    $scope.errorListVisible = ->
      if $scope.project.hasErrors()
        _errorListVisible
      else
        false
    $scope.flipErrorListVisibility = ->
      if $scope.project.hasErrors()
        _errorListVisible = !_errorListVisible
    $scope.validateClick = ($event) ->
      $event.stopPropagation()  # Prevent showing error list
      $scope.validating = true
      project = $scope.project
      viewType = $scope.viewType
      project.viewToModel(viewType, project.viewCache[viewType]).finally ->
        $scope.validating = false
  ]

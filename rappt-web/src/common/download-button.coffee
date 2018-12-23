angular.module 'rappt.io.download-button', [
  'rappt.io.environment'
]

.directive 'downloadButton', ->
  templateUrl: 'common/download-button.html'
  scope:
    project: '=project'
  controller: ['$scope', '$state', 'apiUrl', ($scope, $state, apiUrl) ->
    $scope.generating = false
    $scope.toBeValidated = ->
      not $scope.project.hasNamesSet() or
        ($state.current.data?.viewType? and $scope.project.dirtied)

    openZip = (path) -> window.open "#{apiUrl}#{path}"

    $scope.generate = ->
      if $scope.project.viewCache.zip?
        openZip $scope.project.viewCache.zip
      else
        $scope.generating = true
        $scope.project.modelToView('zip').then (path) ->
          $scope.generating = false
          openZip path
        , (error) ->  # on error
          # TODO this could be much nicer
          window.alert "Sorry, an error occurred generating your app.
                        Why not send us some feedback?\n\n(#{error})"
      return  # Prevent Angular complaining about window return value
]

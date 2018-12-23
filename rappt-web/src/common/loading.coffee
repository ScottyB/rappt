angular.module 'rappt.io.loading', []

# project, viewType and stateName passed as $modal resolve properties
.controller 'LoadingCtrl', ['$scope', '$state', '$q', 'project', 'viewType', 'stateName', ($scope, $state, $q, project, viewType, stateName) ->
  doValidate = ->
    if project.dirtied and (cvt = $state.current.data.viewType)?
      return project.viewToModel cvt, project.viewCache[cvt]
    else
      project.dirtied = false  # Prevent infinite modal loops
      return $q (r, _) -> r()

  doValidate().then ->
    if viewType? && viewType=='dsl'
      project.modelToView viewType
      .then ->
        return project.viewToModel viewType, project.viewCache[viewType]
    else if viewType? && viewType=='zip'
      project.modelToView 'dsl'
      .then ->
        project.viewToModel 'dsl', project.viewCache['dsl']
        .then ->
          return project.modelToView viewType
    else if viewType?
      project.modelToView viewType
  .then ->
    $scope.$close()
    $state.go stateName
  .catch (errors) ->
    $scope.$close()
]

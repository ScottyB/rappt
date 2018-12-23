###*
 # @ngdoc overview
 # @name rappt.io
 # @description
 # # rappt.io
 #
 # Main module of the application.
###
angular.module 'rappt.io', [
  'templates'
  'ngAnimate',
  'ngCookies',
  'ui.router',
  'ui.codemirror',
  'ui.bootstrap'
  'xeditable'

  'rappt.io.editor'
  'rappt.io.feedbackModal'
  'rappt.io.visual-model'
  'rappt.io.code-browser'
  'rappt.io.download-button'
  'rappt.io.loading'
  'rappt.io.project'
  'rappt.io.names'
  'takeFocus'
]

.config ['$stateProvider', ($stateProvider) ->
  $stateProvider
    .state 'visual',
      templateUrl: 'designer/visual-model.html'
      controller: 'VisualModelCtrl'
      data:
        name: 'visual'
        title: 'Designer'
        isTab: true
    .state 'dsl',
      templateUrl: 'editor/editor.html'
      controller: 'EditorCtrl'
      data:
        name: 'dsl'
        viewType: 'dsl'
        title: 'AML'
        isTab: true
    .state 'code-browser',
      templateUrl: 'browser/code-browser.html'
      controller: 'CodeBrowserCtrl'
      data:
        name: 'code-browser'
        viewType: 'zip'
        title: 'Code Browser'
        isTab: true
]

.controller 'AppCtrl', ['$scope', '$modal', '$state', 'projectService', ($scope, $modal, $state, project) ->
  $scope.project = project
  $scope.$state = $state

  welcomeModal = ->
    $modal.open
      templateUrl: 'common/welcome-modal.html'
      backdrop: 'static'  # Prevent closing by clicking outside
      keyboard: false     # Prevent closing with ESC
      scope: $scope

  if project.hasSavedData()
    $state.go project.getSavedDataState()
  else
    $state.go 'visual'
    welcomeModal()

  $scope.newProject = ->
    $modal.open
      templateUrl: 'common/are-you-sure.html'
      backdrop: 'static'
      keyboard: false
      scope: $scope

  # todo: confirmation dialog needs to be separate component
  $scope.clearProject = ->
    project.clearProject()
    $state.go 'visual'
    welcomeModal()

  $scope.$on '$stateChangeStart', (event, toState) ->
    tab = $state.get toState
    viewType = tab.data.viewType
    if(project.model.screens.length == 0) and not project.dirtied
      return
    if project.dirtied or (viewType? and not project.viewCache[viewType]?)
      event.preventDefault()
      $modal.open
        templateUrl: 'common/loading.html'
        controller: 'LoadingCtrl'
        backdrop: 'static'  # Prevent closing by clicking outside
        keyboard: false     # Prevent closing with ESC
        windowClass: 'loading-modal'
        resolve:
          project: -> project
          viewType: -> viewType
          stateName: -> toState

  $scope.openFeedbackForm = ->
    $modal.open
      templateUrl: 'common/feedback-modal.html'
      controller: 'FeedbackModalCtrl'

]

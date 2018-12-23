###*
 # @ngdoc function
 # @name rappt.controller:FeedbackModalCtrl
 # @description
 # # EditorCtrl is the main modal controller of the app
 # Controller of the feedback Modal
###
angular.module 'rappt.io.feedbackModal', [
  'takeFocus'
]

.controller 'FeedbackModalCtrl', ['$scope', '$http', '$modalInstance', '$timeout', 'apiUrl', ($scope, $http, $modalInstance, $timeout, apiUrl) ->
  $scope.sendFeedback = ->
    data =
      email: $scope.email
      message: $scope.message
    $http.post("#{apiUrl}/feedback", data).success (response) ->
      $scope.success = true
      $timeout $modalInstance.close, 2000
    .error (response) ->
      $scope.error = true

  $scope.closeFeedback = -> $modalInstance.close()
]

angular.module 'takeFocus', []

.directive 'takeFocus', ['$timeout', ($timeout) ->
  restrict: 'A'
  link: ($scope, $element) ->
    $timeout (-> $element[0].focus()), 500
]

angular.module 'rappt.io.visual-model', [
  'rappt.io.dropdown'
  'rappt.io.graph'
  'rappt.io.graph-service'
]

.controller 'VisualModelCtrl', ($scope, $q, graphService) ->
  _linking = false
  $scope.linking = (val) ->
    if val?
      _linking = val
    else
      _linking

  $scope.items = [
      text: 'Screen'
  ]

  $scope.graphControl = {}      # Access to the graph directive

  $scope.selectionMade = (event, choice) ->
    graphService.newScreen event.clientX, event.clientY

  $scope.uniqueScreenId = (modelValue, viewValue) ->
    return true if modelValue is viewValue
    return not project.model.screen(viewValue)?


# Validation that doesn't set the model value to undefined if the view value
# is invalid. Based on ui-validate.
.directive 'carefulValidator', ->
  restrict: 'A'
  require: 'ngModel'
  link: (scope, element, attrs, ctrl) ->
    validateExpr = scope.$eval attrs.carefulValidator
    return if not validateExpr

    angular.forEach validateExpr, (exprssn, key) ->
      validateFn = (newViewValue) ->
        expression = scope.$eval exprssn,
          $value: newViewValue,
          $modelValue: ctrl.$modelValue
        if expression
          ctrl.$setValidity key, true
          return newViewValue
        else
          ctrl.$setValidity key, false
          return ctrl.$modelValue
      ctrl.$formatters.push validateFn
      ctrl.$parsers.push validateFn

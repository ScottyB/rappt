angular.module 'rappt.io.names', []

.directive 'validProjectName', ->
  restrict: 'A',
  require: 'ngModel',
  link: (scope, elm, attrs, ctrl) ->
    ctrl.$validators.validProjectName = (modelValue, viewValue) ->
      return /^[a-zA-Z][a-zA-Z_$0-9]*$/.test viewValue

.directive 'validPackageName', ->
  restrict: 'A',
  require: 'ngModel',
  link: (scope, elm, attrs, ctrl) ->
    ctrl.$validators.validPackageName = (modelValue, viewValue) ->
      return /^[a-z][a-z_$0-9]*(?:\.[a-z][a-z_$0-9]*)*$/.test viewValue

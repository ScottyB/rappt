###*
 # @ngdoc function
 # @name rappt.io.controller:EditorCtrl
 # @description
 # # EditorCtrl is the main editor controller of the app
 # Controller of the rappt.io
###
angular.module 'rappt.io.editor', [
  'rappt.io.slideout'
  'rappt.io.error-list'
  'rappt.io.environment'
]

.controller 'EditorCtrl', ['$scope', '$http', '$modal', 'apiUrl', 'projectService', ($scope, $http, $modal, apiUrl, project) ->
  $scope.project = project

  CodeMirror.defineSimpleMode 'rappt',
    start: [{
      regex: /"(?:[^\\]|\\.)*?"/
      token: 'string'
      },{
      regex: /\/\/.*/
      token: 'comment'
      },{
      regex: /\/\*/
      token: 'comment'
      next: 'comment'
      },{
      regex: /\{/
      token: 'bracket'
      indent: true
      },{
      regex: /\}/
      token: 'bracket'
      dedent: true
      },{
      # <keyword> <new-identifier> <existing-idenfitier>
      # If accepts string as well, should go into <keyword> <new-identifier> also
      regex: ///
        (label|tab|GET|image|input)
        (\s+)
        ([A-z$][\w$]*)
        (\s+)
        ([A-z$][\w$]*)///,
      token: ['keyword', null, 'def', null, 'variable-2']
      },{
      # <keyword> <existing-identifier> <attribute>
      regex: ///
        (pass)
        (\s+)
        ([A-z$][\w$]*)
        (\s+)
        ([A-z$][\w$]*)///,
      token: ['keyword', null, 'variable-2', null, 'attribute']
      },{
      # <keyword> <new-identifier>
      regex: ///
        (api|screen|group|drawer
        |list|row|passed|map|marker|button|tabbar
        |label|tab|GET|image|input)  # <-- accepts string after (see above)
        (\s+)
        ([A-z$][\w$]*)///,
      token: ['keyword', null, 'def']
      },{
      # <keyword> <existing-identifier>
      # If accepts string as well, should go into <keyword> also
      regex: ///
        (call|navigate-to|landing-page|map-key|on-click)
        (\s+)
        ([A-z$][\w$]*)///,
      token: ['keyword', null, 'variable-2']
      },{
      # <keyword>
      regex: ///(?:
        app|on-load|on-item-click|behaviour|list
        |call     # <-- can be alone
        |map-key  # <-- accepts string after
        )\b///
      token: 'keyword'
    }]
    comment: [{
      regex: /.*?\*\//
      token: 'comment'
      next: 'start'
      },{
      regex: /.*/
      token: 'comment'
    }]
  CodeMirror.extendMode 'rappt', electricChars: '}'

  $scope.editorOpts =
    lineWrapping: true
    lineNumbers: true
    mode: 'rappt'
    theme: 'default rappt'
    indentWithTabs: false
    tabSize: 4
    indentUnit: 4
    matchBrackets: true
    continueComments: 'Enter'
    styleActiveLine: true
    gutters: ['CodeMirror-foldgutter']
    foldGutter: rangeFinder: CodeMirror.fold.brace
    extraKeys:
      'Tab': (cm) ->
        if cm.doc.somethingSelected()
          return CodeMirror.Pass
        else
          cm.execCommand 'insertSoftTab'
      'Shift-Tab': (cm) -> cm.indentSelection 'subtract'

  # load in samples
  $http.get("#{apiUrl}/samples").success (data) ->
    $scope.samples = data

  selectedSample = null
  $scope.selectSample = ($event, sample) ->
    # Somewhat hacky way to ignore clicking on accordion content
    return if $event.target.textContent isnt sample.name
    selectedSample = if selectedSample?.name is sample.name then null else sample

  $scope.sampleClass = (sample) ->
    if selectedSample?.name is sample.name then 'sample-active' else ''

  codemirrorInstance = null
  $scope.codemirrorLoaded = (editor) ->
    codemirrorInstance = editor

  $scope.goToError = (error) ->
    # parse out the line and column number
    match = /line (\d+)\:(\d+).*/.exec error
    line = parseInt match[1] - 1
    col = parseInt match[2]
    codemirrorInstance.doc.setCursor line, col
    codemirrorInstance.focus()
]

angular.module 'rappt.io.code-browser', [
  'treeControl',
  'rappt.io.project',
  'rappt.io.environment'
]

.controller 'CodeBrowserCtrl', ['$scope', '$http', 'apiUrl', 'projectService', ($scope, $http, apiUrl, project) ->
  $scope.editorOpts =
    lineWrapping : true
    lineNumbers: true
    mode: 'text/x-java'
    indentWithTabs: false
    matchBrackets: true
    continueComments: 'Enter'
    styleActiveLine: true
    readOnly: 'nocursor'

  $scope.showingImage = false
  $scope.codeIn = ''
  $scope.treeOptions = dirSelectable: false
  $scope.codeBrowserTree = []
  $scope.image = ''

  zip.useWebWorkers = false
  zipfs = new zip.fs.FS()

  $http.get("#{apiUrl}#{project.viewCache.zip}", responseType: 'blob').then (response) ->
    zipfs.importBlob response.data, ->
      tree = processZipEntry(zipfs.root).children
      $scope.codeBrowserTree = tree
      $scope.treeExpanded = [tree[0], tree[0].children[0]]
      defaultFile = tree[0].children[0].children.find (e) -> e.name is 'build.gradle'
      if defaultFile?
        $scope.treeSelected = defaultFile
        $scope.showFile defaultFile
      $scope.$applyAsync()
    , (error) ->
      console.error "Could not read zip file: #{error}"

  processZipEntry = (entry) ->
    node = name: entry.name, id: entry.id
    if entry.children.length > 0
      node.children = entry.children.map processZipEntry
    return node

  $scope.showFile = (node) ->
    endsWith = (str, suffix) ->
      str.indexOf(suffix, str.length - suffix.length) != -1

    if endsWith(node.name, ".png") or endsWith(node.name, "jpg") or endsWith(node.name, "jpeg")
      zipfs.getById(node.id).getData64URI("image/png", (data) -> $scope.image = data)
      $scope.showingImage = true

    else
      zipfs.getById(node.id).getText (text) ->
        $scope.codeIn = text
        $scope.$applyAsync()
      , undefined, true, 'utf8'
      $scope.showingImage = false



  $scope.codemirrorLoaded = (editor) ->
    $scope.codemirrorInstance = editor
]

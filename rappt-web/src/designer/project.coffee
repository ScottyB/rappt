angular.module 'rappt.io.project', [
  'rappt.io.environment'
  'rappt.io.model-manipulation'
]

.factory 'projectService', (apiUrl, asyncpoll, modelManipulator, $http, $q) ->
  class ProjectService
    model: modelManipulator

    _statusCheck: null
    viewCache: {}               # todo: merge with local storage option to have one source of truth
    errors: []
    dirtied: true  # true if hasn't changed since last validate with no errors

    localStorageSupported = () ->
      return window['localStorage']

    constructor: () ->
      if localStorageSupported()
        if localStorage['viewCache']?
          data = JSON.parse localStorage['viewCache']
          @model.name = localStorage.projectName
          @model.packageName = localStorage.packageName
          if Object.keys(data).length == 0 then return
          if data.dsl?
            @viewCache['dsl'] = data.dsl
          else
            @model.json = data.visual
        else
          data = {"projectName":"Test","packageName":"io.rappt.test","screens":[{"view":{"buttons":[{"label":"To Secondary","styleName":"","id":"gen2","onTouch":{"instructions":[{"type":"org.scott.rapt.model.Instruction$Navigate","data":{"toScreenId":"gen1","parameterId":"","fieldParameter":{"path":[]},"parameters":[],"id":"gen2_gen1"}}],"doesCloseActivity":false},"onLoad":{"instructions":[],"doesCloseActivity":false}}],"labels":[],"pictures":[],"textInputs":[],"webs":[],"staticLists":[],"dynamicLists":[],"map":{},"stateValue":"","layoutSpecification":"","id":"gen0MainGrouping","onTouch":{"instructions":[],"doesCloseActivity":false},"onLoad":{"instructions":[],"doesCloseActivity":false}},"actions":[],"featureIds":[],"id":"gen0","label":"Visual model generated screen","enableHomeBack":false,"pullToRefresh":false},{"view":{"buttons":[],"labels":[],"pictures":[],"textInputs":[],"webs":[],"staticLists":[],"dynamicLists":[],"map":{},"stateValue":"","layoutSpecification":"","id":"gen1MainGrouping","onTouch":{"instructions":[],"doesCloseActivity":false},"onLoad":{"instructions":[],"doesCloseActivity":false}},"actions":[],"featureIds":[],"id":"gen1","label":"Visual model generated screen","enableHomeBack":false,"pullToRefresh":false}],"navigation":{"tabs":[]},"landingPage":"gen0"}
          localStorage['viewCache'] = JSON.stringify {"visual": data}
          localStorage['projectName'] = "Test"
          localStorage['packageName'] = "io.rappt.test"
          @model.json = data
          @model._nextId = 3

    clearProject: () ->
      @viewCache = {}
      @dirtied = false
      localStorage['viewCache'] = JSON.stringify {}
      @errors = []
      @model.clear()

    hasSavedData: () ->
      if localStorageSupported() and localStorage['viewCache']?
        data = JSON.parse localStorage['viewCache']
        return Object.keys(data).length > 0

    getSavedDataState: () ->
      data = JSON.parse localStorage['viewCache']
      if data.dsl? then return "dsl" else return "visual"

    dirty: (keepView) ->
      if keepView?
        if keepView is 'visual' and not @viewCache[keepView]?
          @viewCache[keepView] = @model.json
        [@viewCache, @viewCache[keepView]] = [{}, @viewCache[keepView]]
      if localStorageSupported()
        localStorage.removeItem 'viewCache'
        localStorage['viewCache'] = JSON.stringify @viewCache
        localStorage['projectName'] = @model.name
        localStorage['packageName'] = @model.packageName

      @dirtied = true
      @_resetRemoteStatus()

    _resetRemoteStatus: -> @_statusCheck?.cancel()

    hasErrors: -> @errors.length > 0
    compiled: -> @viewCache.zip?

    hasNamesSet: -> @model.packageName? and @model.name?

    _handleHTTPError: (res) =>
      statusText = if res.status is 0 then "Could not contact API" else res.statusText
      errors = ["HTTP #{res.status} - #{statusText}"]
      @errors = errors
      return $q.reject errors

    viewToModel: (endpoint, viewData) ->
      req =
        view: viewData
        package: @model.packageName
        'project-name': @model.name

      @_resetRemoteStatus()

      return (@_statusCheck = asyncpoll.post("#{apiUrl}/validate/#{endpoint}", req)).then (data) =>
        if data.errors.length is 0
          @model.json = data.model
          @viewCache['visual'] = data.model
          @dirtied = false
          @errors = []
        else
          @errors = data.errors
          return $q.reject data.errors
      , @_handleHTTPError

    # Assumes no errors
    modelToView: (type) ->
      req =
        model: @model.json
        package: @model.packageName
        'project-name': @model.name
      return (@_statusCheck = asyncpoll.post("#{apiUrl}/generate/#{type}", req)).then (data) =>
        @viewCache[type] = data.view
        return data.view
      , @_handleHTTPError

  return new ProjectService()


.factory 'asyncpoll', ($http, $q, $timeout) ->
  class AsyncPoll
    _poll = (url, resolve, reject, cancelRef) ->
      return if cancelRef.cancelled  # abort if cancelled
      $http.get(url).then (res) ->
        return if cancelRef.cancelled  # abort if cancelled
        if res.headers('Content-Type')?.indexOf('application/vnd.rappt.asyncpoll-v1+json') is 0
          $timeout _poll.bind(undefined, url, resolve, reject, cancelRef), 500, false
        else  # follows 303 automatically
          resolve res.data
      , reject  # on error

    post: (url, data, options = {}) ->
      options.responseType = 'text'
      cancelRef = cancelled: false
      q = $q (resolve, reject) ->
        $http.post(url, data, options).then (res) ->
          if res.status is 202
            url = res.headers 'Location'
            $timeout _poll.bind(undefined, url, resolve, reject, cancelRef), 200, false
          else
            reject res
        , (res) -> reject res
      q.cancel = -> cancelRef.cancelled = true
      return q

  return new AsyncPoll()

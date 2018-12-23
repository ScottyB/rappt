# Define properties a little easier
# From http://bl.ocks.org/joyrexus/65cb3780a24ecd50f6df
# Note that this doesn't play nice with CoffeeScript's `super`
# https://gist.github.com/davepeck/4236746
Function::property = (prop, desc) ->
  Object.defineProperty @prototype, prop, desc

angular.module 'rappt.io.model-manipulation', []

.factory 'modelManipulator', ->
  class TabBar
    constructor: (@_model) ->

    @property 'screens',
      get: -> @_model.screen tab.to.toScreenId for tab in @_model.json.navigation.tabs

    contains: (screen) =>
      @_model.json.navigation.tabs.some (t) -> t.to.toScreenId is screen.id

    push: (screen) =>
      if @_model.json.navigation.navigationMethod not in [undefined, 'TABBAR']
        throw new Error "Cannot add tabs when navigationMethod is #{navigationMethod}"

      @_model.json.navigation.navigationMethod = 'TABBAR'
      @_model.json.navigation.id = @_model.freshId()

      id = @_model.freshId()
      tab =
        id: id
        text: screen.label
        to:
          toScreenId: screen.id
          parameterId: ''
          fieldParameter:
            path: []
          parameters: []
          id: "#{id}_#{screen.id}"
      @_model.json.navigation.tabs.push tab
      return @

    remove: (screen) =>
      idx = @_model.json.navigation.tabs.findIndex (t) -> t.to.toScreenId is screen.id
      return if idx is -1
      @_model.json.navigation.tabs.splice idx, 1
      if @_model.json.navigation.tabs.length is 0
        @_model.json.navigation.navigationMethod = undefined
      return @

  class Screen
    constructor: (@_model, @_json) ->

    @create: (model, id) ->
      json = {  # TODO: some of these could be removed
        "view": {
          "buttons": [],
          "labels": [],
          "pictures": [],
          "textInputs": [],
          "webs": [],
          "staticLists": [],
          "dynamicLists": [],
          "map": {},
          "stateValue": "",
          "layoutSpecification": "",
          "id": "#{id}MainGrouping",
          "onTouch": {
            "instructions": [],
            "doesCloseActivity": false
          },
          "onLoad": {
            "instructions": [],
            "doesCloseActivity": false
          }
        },
        "actions": [],
        "featureIds": [],
        "id": id,
        "label": "Visual model generated screen",
        "enableHomeBack": false,
        "pullToRefresh": false
      }

      return new Screen model, json

    @property 'id',
      get: -> @_json.id
      set: (newId) ->
        btn._json.onTouch.instructions[0].data.toScreenId = newId for btn in @buttonsTo
        tab.to.toScreenId = newId for tab in @_model.json.navigation.tabs when tab.to.toScreenId is @id
        for btn, i in @_json.view.buttons
          btn._json?.onTouch.instructions[i].data.id = btn._json.id+'_'+btn._json.onTouch.instructions[i].data.toScreenId
        @_json.view.id="#{newId}MainGrouping"
        if @_model.json.landingPage is @_json.id
          @_model.json.landingPage = newId
        @_json.id = newId

    @property 'label',
      get: -> @_json.label
      set: (nv) -> @_json.label = nv

    @property 'buttons',
      get: ->
        for btn in @_json.view.buttons
          inst = btn.onTouch.instructions.find (i) ->
            i.type is 'org.scott.rapt.model.Instruction$Navigate'
          continue if not inst?
          from: @
          to: @_model.screen btn.onTouch.instructions[0].data.toScreenId
          id: btn.id
          _json: btn

    @property 'buttonsTo',
      get: ->
        results = []
        for screen in @_model.screens
          for btn in screen.buttons when btn.to._json is @_json
            results.push btn
        return results

    addButton: (to) =>
      btnId = @_model.freshId()
      btn = {  # TODO: some of these could be removed
        "label": "To Secondary",
        "styleName": "",
        "id": btnId,
        "onTouch": {
          "instructions": [
            {
              "type": "org.scott.rapt.model.Instruction$Navigate",
              "data": {
                "toScreenId": to.id,
                "parameterId": "",
                "fieldParameter": {
                  "path": []
                },
                "parameters": [],
                "id": "#{btnId}_#{to.id}"
              }
            }
          ],
          "doesCloseActivity": false
        },
        "onLoad": {
          "instructions": [],
          "doesCloseActivity": false
        }
      }
      @_json.view.buttons.push btn
      return btn

    removeButton: (btn) =>
      buttons = @_json.view.buttons
      buttons.splice buttons.findIndex((b) -> b.id is btn.id), 1
      return @

    @property 'hasMap',
      get: -> @_json.view.map.value?
      set: (should) ->
        if should and not @hasMap
          @_json.view.map.value =
            dynamicMarkers: []
            id: @_model.freshId()
            noInteractions: false
            onLoad:
              doesCloseActivity: false
              instructions: []
            onMapClick:
              doesCloseActivity: false
              instructions: []
            onTouch:
              doesCloseActivity: false
              instructions: []
            polyLines: []
            staticMarkers: []
        else if not should
          screen.view.map.value = {}

    _prepRemove: =>
      btn.from.removeButton btn for btn in @buttonsTo
      @_model.tabbar.remove @


  class ModelManipulator
    constructor: ->
      @clear()

    clear: ->
      @_nextId = 0
      @json =
        projectName: 'Test'
        packageName: 'io.rappt.test'
        screens: []
        navigation:
          tabs: []

    freshId: => "gen#{@_nextId++}"

    @property 'name',
      get: -> @json.projectName
      set: (name) -> @json.projectName = name

    @property 'packageName',
      get: -> @json.packageName
      set: (name) -> @json.packageName = name

    @property 'screens',
      get: -> @json.screens.map (s) => new Screen @, s

    @property 'tabbar',
      get: -> new TabBar @

    screen: (id) => @screens.find (s) -> s.id is id

    addScreen: ({id} = {}) =>

      # if the screen id exists, genereate a new one until one is available
      if @screen(id)?
        id = @freshId() while @screen(id)?

      screen = Screen.create @, id
      @json.screens.push screen._json

      if @screens.length is 1
        @json.landingPage = id

      return screen

    removeScreen: (screen) =>
      screen._prepRemove()

      @json.screens.splice (@json.screens.indexOf screen._json), 1

      if @json.landingPage is screen.id
        @json.landingPage = @screens[0]?.id

  return new ModelManipulator()

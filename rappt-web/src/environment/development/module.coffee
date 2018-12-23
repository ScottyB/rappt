angular.module 'rappt.io.environment', [
  'rappt.io.project'
]

.constant 'apiUrl', 'http://localhost:3000'

# Debug names
.run ['projectService', (project) ->
  window.project = project
]

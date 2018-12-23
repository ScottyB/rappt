# Polyfills for Chrome

# Array::find polyfill from MDN
# https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find
Array::find ?= (predicate, thisArg) ->
  if not this?
    throw new TypeError 'Array.prototype.find called on null or undefined'
  else if typeof predicate isnt 'function'
    throw new TypeError 'predicate must be a function'

  list = Object @
  length = list.length >>> 0

  for i in [0...length]
    value = list[i]
    return value if predicate.call thisArg, value, i, list

  return undefined


Array::findIndex ?= (predicate, thisArg) ->
  if not this?
    throw new TypeError 'Array.prototype.findIndex called on null or undefined'
  else if typeof predicate isnt 'function'
    throw new TypeError 'predicate must be a function'

  list = Object @
  length = list.length >>> 0

  for i in [0...length]
    value = list[i]
    return i if predicate.call thisArg, value, i, list

  return -1

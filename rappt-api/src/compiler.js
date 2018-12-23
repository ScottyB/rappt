require('array.prototype.find');

var fs = require('fs');
var mkdirp = require('mkdirp'); // To create tmp dir
var Q = require('q');
var rimraf = require('rimraf'); // To delete tmp dir

var config = require('../config');
var myRedis = require('./myredis');
var operations = require('./operations').operations;

var rabbitOutgoing;

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str) {
    return this.slice(0, str.length) === str;
  };
}

var onIncoming = function (msgStr) {
  var msg = JSON.parse(msgStr);
  var id = msg.id;
  var data = msg.data;
  var operation = operations.find(function (op) { return op.path == msg.operation; });
  if (operation != null) {
    operation.onResponse(id, data)
    .then(function (data) {
      return myRedis.setExpiring(config.REDIS_PREFIX+id, JSON.stringify(data),
        config.DEFAULT_KEY_EXPIRY);
    })
    .catch(console.error.bind(console));
  } else {
    console.log('Unknown operation ' + msg.operation + ' in compiler response');
  }
}

var errorCallbacks = [];

var initRabbit = function () {
  console.log('Trying to contact Rabbit at ' + config.RABBIT_SERVER);
  var rabbit = require('rabbit.js').createContext(config.RABBIT_SERVER);
  rabbit.on('error', console.log.bind(console));

  var defer = Q.defer();
  rabbit.on('ready', function () {
    console.log('Rabbit connected');
    var incoming = rabbit.socket('SUBSCRIBE');
    rabbitOutgoing = rabbit.socket('PUBLISH');
    incoming.setEncoding('utf8');
    incoming.on('data', onIncoming);
    incoming.connect('user-bound', function () {
      rabbitOutgoing.connect('compiler-bound', function () {
        console.log('Rabbit wired');
        defer.resolve(rabbit);
      });
    });
  })
  rabbit.on('error', function (err) {
    defer.reject(err);
    errorCallbacks.forEach(function (f) { f(err); });
  });
  return defer.promise;
}

exports.ready = function () {
  return Q.nfcall(rimraf, config.COMPILER_OUTPUT_DIR)
  .then(Q.nfcall.bind(Q, mkdirp, config.COMPILER_OUTPUT_DIR))
  .then(function () { console.log('tmp dir created successfully'); })
  .then(myRedis.config.bind(myRedis, 'notify-keyspace-events', 'Ex'))
  .then(function () {
    myRedis.subscribe('__keyevent@0__:expired', function (key) {
      if (key.startsWith(config.REDIS_PREFIX)) {
        var id = key.slice(config.REDIS_PREFIX.length);
        fs.unlink(config.COMPILER_OUTPUT_DIR + '/' + id + '.zip');
        console.log('Expired files for id ' + id);
      }
    });
  })
  .then(initRabbit);
}

exports.onError = function (f) {
  errorCallbacks.push(f);
}

exports.startCompiling = function (id, operation, body) {
  var msgStr = JSON.stringify({
    id: id,
    operation: operation,
    data: body
  });
  console.log(msgStr);
  rabbitOutgoing.write(msgStr, 'utf8');
  return Q.fcall(function () {});
}

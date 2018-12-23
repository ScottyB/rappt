var redisModule = require('redis');
var Q = require('q');

// Message subscriptions
var redisSub;
var subscriptions = {};

exports.subscribe = function (channel, callback) {
  redisSub.subscribe(channel);
  subscriptions[channel] = subscriptions[channel] || [];
  subscriptions[channel].push(callback);
};

// Actions
var redis;

exports.get = function (key) {
  return Q.ninvoke(redis, 'get', key);
};

exports.incr = function (key) {
  return Q.ninvoke(redis, 'incr', key);
};

exports.setExpiring = function (key, value, time) {
  return Q.ninvoke(redis, 'set', key, value, 'EX', time);
};

exports.config = function (key, value) {
  return Q.ninvoke(redis, 'config', 'SET', key, value);
};

// Prep
exports.ready = function () {
  redis = redisModule.createClient();
  return Q.ninvoke(redis, 'on', 'connect')
  .then(function () {
    console.log('Connected to redis');
    redisSub = redisModule.createClient();
    redisSub.on('message', function (channel, message) {
      subscriptions[channel].forEach(function (callback) {
        callback(message);
      });
    });
  });
}
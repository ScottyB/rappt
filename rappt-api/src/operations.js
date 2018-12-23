var fs = require('fs');
var Q = require('q');

var config = require('../config');

var defaultOperation = function (path) {
  return {
    path: path,
    onResponse: function (id, data) {
      return Q.fcall(function () { return data; });
    }
  };
}

var zipOperation = function (path) {
  return {
    path: path,
    onResponse: function (id, data) {
      var b64 = data.view;
      data.view = '/download/' + id;
      return Q.nfcall(fs.writeFile, config.COMPILER_OUTPUT_DIR + '/' + id + '.zip', b64, {encoding: 'base64'})
        .then(function () { return data; });
    }
  };
}

exports.operations = [
  defaultOperation('/validate/dsl'),
  defaultOperation('/generate/dsl'),
  zipOperation('/generate/zip')
];

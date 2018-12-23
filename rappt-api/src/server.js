var app = require('express')();

var bodyParser = require('body-parser');
var cors = require('cors');
var fs = require('fs');
var url = require('url');

var config = require('../config');
var compiler = require('./compiler');
var myRedis = require('./myredis');
var operations = require('./operations').operations;

// For sending the feedback form email
var mailgun;
(function () {
  var Mailgun = require('mailgun').Mailgun;
  mailgun = new Mailgun(config.MAILGUN_API_KEY);
})();

app.use(bodyParser.json());
app.use(cors({exposedHeaders: ['Location']}));

console.log('Collating samples...');
var SAMPLES = [];
fs.readdirSync(config.SAMPLES_DIR).forEach(function(filename){
  SAMPLES.push({
    name: filename.split('.')[0],
    code: fs.readFileSync(config.SAMPLES_DIR + '/' + filename, {encoding: 'utf8'})
  });
});
console.log('' + SAMPLES.length + ' samples collated');

app.get('/', function(req, res) {
  res.send('API RUNNING');
});

var absAppUrl = function (req, path) {
  return url.format({protocol: req.protocol, host: req.get('Host'), pathname: path});
}

// Express middleware to ensure IDs are numbers
var checkId = function (req, res, next) {
  var inputId = req.params.id;
  var asNumber = Number(inputId);
  var asInt = parseInt(inputId, 10);
  if (!isNaN(asInt) && asNumber === asInt) {
    req.id = inputId;
    next();
  } else {
    res.status(404).end();
  }
}

// Wrap handler to 500 on promise error
var asyncErrors = function (handler) {
  return function (req, res) {
    var promise = handler(req, res);
    promise.catch(function (err) {
      console.error(err);
      res.status(500).end();
    });
  }
}

operations.forEach(function (operation) {
  app.post(operation.path, asyncErrors(function(req, res) {
    req.accepts('json');

    return myRedis.incr('nextid').then(function (id) {
      return myRedis.setExpiring(config.REDIS_PREFIX + id, '', config.DEFAULT_KEY_EXPIRY)
      .then(compiler.startCompiling.bind(compiler, id, operation.path, req.body))
      .then(function () {
        res.status(202).location(absAppUrl(req, '/status/'+id)).end();
      });
    });
  }));
});

// Check the status of some ongoing process.
app.get('/status/:id', checkId, asyncErrors(function (req, res) {
  return myRedis.get(config.REDIS_PREFIX + req.id).then(function (status) {
    if (status == null) {
      res.status(404).end();
    } else if (status === '') {
      res.status(200).set({
        'Expires': 'Thu, 01 Dec 1994 16:00:00 GMT', // for HTTP/1.0 proxies
        'Cache-Control': 'no-cache',
        'Content-Type': 'application/vnd.rappt.asyncpoll-v1+json'
      }).send('{"status": "processing"}');
    } else {
      res.status(303).location(absAppUrl(req, '/response/'+req.id)).end();
    }
  })
}));

// Get the response of some completed process.
app.get('/response/:id', checkId, asyncErrors(function (req, res) {
  return myRedis.get(config.REDIS_PREFIX + req.id).then(function (status) {
    if (status == null || status === '') {
      res.status(404).end();
    } else {
      console.log('Sending response for id ' + req.id);
      var json = JSON.parse(status);
      if (json.zip) {
        json.zip = absAppUrl(req, json.zip);
        status = JSON.stringify(json);
      }
      res.status(200).type('application/vnd.rappt-v1+json').send(status);
    }
  });
}));

// Download the compiled zip. This may disappear in the future.
app.get('/download/:id', checkId, function(req, res) {
  var zipFile = config.COMPILER_OUTPUT_DIR + '/' + req.id + '.zip';
  res.status(200).attachment('' + req.id + '.zip').sendFile(zipFile);
});

app.get('/samples', function(req, res) {
  res.json(SAMPLES);
});

app.post('/feedback', bodyParser.json(), function(req, res) {
  if (!req.body || !req.body.email || !req.body.message) {
    return res.status(400).send('Email and message required to give feedback');
  }

  console.log('Received feedback from ' + req.body.email);

  mailgun.sendText(
    'no-reply@rappt.io',
    'rappt.feedback@gmail.com',
    'rappt.io: Feedback form submission',
    'The following message is feedback submitted by a user from http://rappt.io/\n\n' +
      'Email: ' + req.body.email + '\n' + 'Message: ' + req.body.message,
    function (err) {
      if (err) {
        console.error(err);
      } else {
        console.log('Feedback email sent');
      }
    });

  res.status(200).end();
});

// Old code: outputs rappt tmp directory
// app.get('/raptdir', function(req, res){
//   res.send(config.COMPILER_OUTPUT_DIR);
// });

myRedis.ready()
.then(compiler.ready)
.then(function () {
  myRedis.config('save', '');
  compiler.onError(function () {
    process.exit(2);
  });

  var server = app.listen(3000, function() {
    console.log('Listening on port %d', server.address().port);
  });
})
.catch(function (err) {
  console.error('Failed to start server or prerequisites: ' + err);
  process.exit(1);
});

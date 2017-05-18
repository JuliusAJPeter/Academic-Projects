var express = require('express'),
    app = express(),
    bodyParser = require('body-parser'),
    //urlencodedParser = bodyParser.urlencoded({extended: true}),
    // Port
    port = Number(process.env.PORT || 8000),
    // Database
    sqlite3 = require('sqlite3').verbose(),
    sequelize = require('sequelize');
    auth = require('./auth.js'),
    inside = require('point-in-polygon');
    db = new sqlite3.Database('projectDB.db');
    
var models;
require('./models.js')('sqlite://projectDB.db').then(m => {
  models = m;
  m.CapturePoint.findOne().then(point => {
    if(point === null) {
      require('./mockdata/create_points.js')(m);
    }
  });
});

app.use(bodyParser.json());
auth.initSessions(app);

app.post('/login', function(req, res) {
  models.User.findOne({
    where: {
      username: req.body.loginUser,
      password: req.body.loginPass,
    }
  }).then(user => {
    var json;
    if(user !== null) {
      auth.loginSession(req, user.username);
      json = {
        username: user.username,
        status: 'success',
        error: '',
      };
    } else {
      json = {
        status: 'failure',
        error: 'Invalid username and/or password',
      };
    }
    jsonResponse(res, 200, json);
  });
});

app.post('/logout', function(req, res) {
  auth.logoutSession(req);
  return jsonResponse(res, 200, {
    status: 'success',
    error: ''
  });
});

app.post('/register', function(req, res) {
  
  var username = req.body.newUser;
  var password = req.body.newPassword;
  var email = req.body.newEmail;
  
  if(!(username && password && email)) {
    jsonResponse(res, 200, {
      status: 'failure',
      error: 'Invalid input',
    });
    return;
  }
  
  models.User.findOne({
    where: {
      username: username,
    }
  }).then(user => {
    if(user === null) {
      models.User.create({
        username: username,
        password: password,
        email: email,
      }).then(_ => {
        jsonResponse(res, 200, {
          status: 'success',
          error: '',
        });
      })
    } else {
      jsonResponse(res, 200, {
        status: 'failure',
        error: 'User exists!',
      });
    }
  })
});

app.post('/save', function(req, res) {
  var username = auth.authenticateSession(req);
  var title = req.body.title || 'no title';
  var distance = req.body.distance;
  var latLng = req.body.latLng;
  
  if(!(username && title && distance && latLng)) {
    jsonResponse(res, 200, {
      status: 'failure',
      error: 'Invalid input',
    });
    return;
  }
  
  models.Area.create({
    UserUsername: username,
    title: title,
    distance: distance
  }).then(area => {
    for(var i = 0; i < latLng.length; i++) {
      latLng[i].sequence = i;
      latLng[i].AreaId = area.id;
    }
    models.AreaPoint.bulkCreate(latLng).then(_ => {
      jsonResponse(res, 200, {
        status: 'success',
        error: '',
      });
      capturePoints(username, latLng.map(p => [p.lat, p.lng]));
    });
  });
});

function capturePoints(username, areaPoints) {
    models.CapturePoint.findAll().then(capturePoints => {
        var pointsInside = [];
        capturePoints.forEach(p => {
            if(p.UserUsername !== username && inside([p.lat, p.lng], areaPoints)) {
                pointsInside.push(p);
            }
        });
        return pointsInside;
    }).then(pointsInside => capturePointUpdateOwner(username, pointsInside));
}

function capturePointUpdateOwner(username, pointsInside) {
    for(var i = 0; i < pointsInside.length; i++) {
        pointsInside[i].update({ 'UserUsername': username });
    }
}

app.get('/userdata', function(req, res) {
  var username = auth.authenticateSession(req);
  getPointsHighscore(3).then(pointsHighscore => {
    getRecent(username, 3).then(recent => {
      getHighscore(3).then(highscore => {
        getPointsOwned(username).then(points => {
          jsonResponse(res, 200, {
            recent: recent,
            highscore: highscore,
            points: points,
            pointsHighscore: pointsHighscore,
            status: 'success',
            error: '',
          });
        });
      });
    });
  });
});

app.get('/points', function(req, res) {
  models.CapturePoint.findAll().then(points => {
    jsonResponse(res, 200, {
      points: points,
      status: 'success',
      error: '',
    });
  })
});

app.get('/run/:runId', function(req, res) {
  var runId = req.params.runId;
  if(!runId) {
    jsonResponse(res, 200, {
      status: 'failure',
      error: 'Run id missing!'
    });
    return;
  }
  models.Area.findOne({
    where: {
      id: runId
    }
  }).then(area => {
    return models.AreaPoint.findAll({
      where: {
        AreaId: area.id 
      }
    });
  }).then(areaPoints => {
    jsonResponse(res, 200, {
      points: areaPoints,
      status: 'success',
      error: '',
    });
  })
});

function getRecent(username, count) {
  if(!username) {
    return Promise.resolve([]);
  }
  return models.Area.findAll({
    where: {
      UserUsername: username,
    },
    order: [
       ['createdAt', 'DESC'],
    ]
  }).then(areas => areas.slice(0, count))
}

function getHighscore(count) {
  return models.Area.findAll({
    order: [
      ['distance', 'DESC'],
    ]
  }).then(areas => areas.slice(0, count))
}

function getPointsOwned(username) {
  return models.CapturePoint.findAll({
    attributes: [ [ sequelize.fn('COUNT', sequelize.col('UserUsername')), 'pointCount'] ],
    where: {
      UserUsername: username,
    }
  }).then(result => result[0].dataValues.pointCount);
}

function getPointsHighscore(count) {
  var points
  return models.CapturePoint.findAll().then(result => {
    var counts = result.reduce((a, v) => {
      a[v.UserUsername] = a[v.UserUsername] + 1 || 1;
      return a;
    }, {});
    var countArray = [];
    for(var k in counts) {
      if(k === 'null') {
        continue;
      }
      countArray.push([k, counts[k]]);
    }
    return countArray.sort((a, b) => a[0] > b[0]).slice(0, count);
  });
}

var server = app.listen(port, function() {
  console.log("MaprunR server listening at http://localhost:%s", port);
});

function jsonResponse(response, code, data) {
  console.log("JSON sent: " + JSON.stringify(data));
  response.writeHead(code, {'content-type': 'application/json; charset=utf-8'});
  response.end(JSON.stringify(data));
}


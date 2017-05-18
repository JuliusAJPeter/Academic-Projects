// config
var allowedUsers = {user: 'pass'}; //'9d4e1e23bd5b727046a9e3b4b7db57bd8d6ee684'}; // username:password_hash
var allowedFormats = ["image/png", "image/jpg", "image/jpeg"];
var secret = "SECRET";
var jwt = require('jsonwebtoken');
var express = require('express');
var bodyParser = require('body-parser');
var formidable = require('formidable');
var multer = require('multer');
var mime = require('mime');
var tesseract = require('node-tesseract');
var easyimg = require('easyimage');
var fs = require('fs');
var session = require('express-session');
var passport = require('passport');
var app = express();
var urlencodedParser = bodyParser.urlencoded({extended: true});
// required for passport
app.use(session({
    secret: 'testsessionsecret',
    resave: true,
    saveUninitialized: true
})); // session secret
app.use(passport.initialize());
app.use(passport.session()); // persistent login sessions
//require('./config/passport')(passport); // pass passport for configuration

// configuration for http and https communication
var https = require('https');
var privateKey  = fs.readFileSync('key.pem');
var certificate = fs.readFileSync('cert.pem');
var credentials = {key: privateKey, cert: certificate, passphrase: 'secret'};
var httpsServer = https.createServer(credentials, app);

//configuration for multer upload
var storage = multer.diskStorage({
   destination: function (req, file, callback) {
     callback(null, './uploads/src');
   },
   filename: function (req, file, callback) {
     callback(null, file.fieldname + '-' + Date.now()
                                   + '.' + mime.extension(file.mimetype));
   }
});
var upload = multer({storage : storage}).array('ocrPhoto', 2);
//Options for tessercat cmd
var ocrOpts = {
    l: 'eng',
    psm: 3,
    binary: 'tesseract'
};
var sqlite3 = require('sqlite3').verbose();
var db = new sqlite3.Database('projectDB.db');

function isAuthorizedUser(username, passwordHash) {
    return allowedUsers.hasOwnProperty(username)
        && allowedUsers[username] == passwordHash;
}

function generateToken(req) {
    var expiresDefault = Math.floor(new Date().getTime() / 1000) + 7 * 24 * 60 * 60;
    var token = jwt.sign({
        auth: 'secret',
        agent: req.headers['user-agents'],
        exp: expiresDefault
    }, secret);
    return token;
}

function verify(token) {
    var decoded = false;
    try {
        decoded = jwt.verify(token, secret);
    } catch (e) {
        decoded = false; // still false
    }
    return decoded;
}

//Function accepts image and resturns thumbnail:text as JSON
function convertOCR(response, serverFile, onSuccess){
  var start = Date.now();
  tesseract.process(__dirname + '/uploads/src/'
                              + serverFile, ocrOpts, function(err, text) {
      if(err) {
        response.writeHead(500, {'content-type': 'application/json'});
        response.end(JSON.stringify({error:err}));
      }
      var end = (Date.now() - start) + "ms";
      //console.log(timeOut);
      onSuccess(text, serverFile, end);
  });
}

//function to generate thumbnail. /src contains original upload and /thumbs contains generated thumbnail
function thumbnail(serverFile){
  easyimg.rescrop({
   src:'./uploads/src/' + serverFile,
   dst:'./uploads/thumbs/' + serverFile,
   width:100, height:100});
  //console.log("Thumbnail generated " + serverFile);
  return;
}

function sendJsonResponse(response, responseCode, details){
  console.log("JSON response: " + JSON.stringify(details));
  response.writeHead(responseCode, {'content-type': 'application/json'});
  response.end(JSON.stringify(details));
}

function authenticateOrDie(response, token) {
    var decoded = verify(token);
    if (!decoded || decoded.auth !== 'secret') {
        console.log("Authentication failed for token '" + token);
        sendJsonResponse(response, 403, {error: "Authentication failed for token '" + token + "' (decoded: '" + decoded + "')"});
        return false;
    } else{
      console.log("Authenticated");
      return true;
    }
}

//Function to allow image type png, jpg, jpeg only
function allowedFormat(response, userFiletype){
  if (allowedFormats.indexOf(userFiletype) < 0){
    sendJsonResponse(response, 500, {error: "Format " + userFiletype + " is not supproted currently"});
    return false;
  } else{
    //console.log("Allowed format");
    return true;
  }
}

app.use(bodyParser.json());

// GET /login
app.get('/login', function (req, res) {
    //console.log(req + res);
    res.sendFile(__dirname + "/login.html");
});

// POST /login
// username=<username>&password=<password_hash>
app.post('/login', urlencodedParser, function (req, res) {
    var nameValue = req.body.username;
    var passValue = req.body.password;
    var loginJSON = {
        username: nameValue,
        token: "",
        login: ""
    };
    if (isAuthorizedUser(nameValue, passValue)) {
        console.log("Logged in, user: " + nameValue);
        loginJSON.login = "Success";
        loginJSON.token = generateToken(req);
        sendJsonResponse(res, 200, loginJSON);
    } else {
        console.log("Login failed for " + nameValue);
        loginJSON.login = "Failure";
        sendJsonResponse(res, 403, loginJSON);
    }
});

app.get('/login/facebook', passport.authenticate('facebook'));

// handle the callback after facebook has authenticated the user
app.get('/login/facebook/callback',
    passport.authenticate('facebook', {
        session:false
    }), function(req, res) {
        var loginJSON = {
          username: 'Facebook account',
          token: "",
          login: ""
        };
        loginJSON.token = generateToken(req);
        sendJsonResponse(res, 200, loginJSON);
        return;
    });

// GET /convert
app.get('/image', function(req, res){
   res.sendFile(__dirname + "/upload.html");
});

// POST /convert
app.post('/image', function(req, res){
  var convert = new formidable.IncomingForm();
  var ocrJSON = [];
  //var fileArray = [];
  var appToken;
  convert.multiples = true;
  convert.on('field', function(field, value){
    appToken = value;
  });
  convert.on('file', function(file, value){
    userFiletype = value;
  },
  upload(req, res, function(err, userFiletype) {
    if(err) {
      console.log("Upload error:" + err);
      sendJsonResponse(res, 500, {error: "Error uploading file"});
      return;
    }
    if (!authenticateOrDie(res, appToken)){
      return;
    }
//check for non supproted file formats
    for(var i = 0; i < req.files.length; ++i){
      if (!allowedFormat(res, req.files[i].mimetype)){
        return;
      }
    }
    var lastFile = req.files[(req.files.length)-1].filename;
    for(var i = 0; i < req.files.length; ++i){
      var dateFormat = new Date();
      var createTs = dateFormat.getDate() + '-' + (dateFormat.getMonth() + 1) + '-' +
                 dateFormat.getFullYear() + '-' + dateFormat.getHours() + '.' +
                 dateFormat.getMinutes() + '.' + dateFormat.getSeconds() + '.' +
                 dateFormat.getMilliseconds();
      thumbnail(req.files[i].filename);
      convertOCR(res, req.files[i].filename, function(text, serverFile, end){
        if (text == " \n\n"){
          ocrJSON.push({
            createTs: '-1',
            thumbnail: 'Poor picture quality',
            text: 'Text not extracted. Poor picture quality or image does not contain text',
            time: '0ms'
          });
        } else{
          ocrJSON.push({
            createTs: createTs,
            thumbnail: fs.readFileSync('./uploads/thumbs/' + serverFile, 'base64'),
            text: text,
            time: end
          });
          var row = [createTs, 'user', fs.readFileSync('./uploads/thumbs/' + serverFile, 'base64'), text, end];
          tableInsert(res, row);
        }

        console.log("text: " + text);
        if (lastFile == serverFile){
          //console.log("JSON response: " + JSON.stringify(ocrJSON));
          res.writeHead(200, {'content-type': 'application/json'});
          res.end(JSON.stringify(ocrJSON));
        }
     });
   }
 }));

  convert.parse(req);
});

// GET /store returns transaction history for user
app.get('/store', function(req, res){
  if (!authenticateOrDie(res, req.query.appToken)) {
        return;
  }
  var rowJSON = [];
  db.all("SELECT createTs, thumbnail, thumbText, time FROM user_log", function(err, rows) {
       //console.log(row.length);
       if (err){
         console.log("Table select error:" + err);
         sendJsonResponse(res, 500, {createTs: '', thumbnail: '', text: '', time: ''});
         return;
       }
       rows.forEach(function (row) {
         rowJSON.push({
           createTs: row.createTs,
           thumbnail: row.thumbnail,
           text: row.thumbText,
           time: row.time
         });
       });
       console.log(JSON.stringify(rowJSON));
       sendJsonResponse(res, 200, rowJSON);
  });
  //db.close();
});

// POST /store inserts transaction to user_hist
app.post('/store', urlencodedParser, function(req, res){
  if (!authenticateOrDie(res, req.body.appToken)) {
        return;
  }
  for (var i=0; i<req.body.text.length; i++){
    var dateFormat = new Date();
    var createTs = dateFormat.getDate() + '-' + (dateFormat.getMonth() + 1) + '-' +
               dateFormat.getFullYear() + '-' + dateFormat.getHours() + '.' +
               dateFormat.getMinutes() + '.' + dateFormat.getSeconds() + '.' +
               dateFormat.getMilliseconds();
    if (req.body.thumbnail[i] != ''){
      var row = [createTs, 'user', req.body.thumbnail[i], req.body.text[i], req.body.time[i]];
      tableInsert(res, row);
    }
  }
  //db.close();
  console.log('Inserted rows');
  sendJsonResponse(res, 200, {success: "true", error: ""});
});

function tableInsert(res, row){
  db.serialize(function(res, err) {
    if (err){
      console.log("Table insert error:" + err);
      sendJsonResponse(res, 500, {success: "fail", error: err});
    }
    db.run("CREATE TABLE if not exists user_log (createTs TEXT, userName TEXT, thumbnail TEXT, thumbText TEXT, time TEXT)");
    db.run("INSERT INTO user_log VALUES (?, ?, ?, ?, ?)", row);
  });
}

// Serve our static files (where the actual frontend files are)
app.use("/", express.static(__dirname + "/../frontend/web"));

httpsServer.listen(8443, function(){
  console.log("Https server listening on 8443");
});

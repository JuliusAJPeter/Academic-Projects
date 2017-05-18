// config/auth.js

// expose our config directly to our application using module.exports
module.exports = {

    'facebookAuth' : {
        'clientID'      : '1190238214396750', // your App ID
        'clientSecret'  : '5b56b83ce2f8c24244e79a3bf04d0502', // your App Secret
        'callbackURL'   : 'http://localhost:8080/login/facebook/callback'
    }

};
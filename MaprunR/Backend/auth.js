var session = require('express-session');

module.exports = {
    
    initSessions: function(app) {
        app.use(session({
            secret: 'placeholder_secret',
            resave: false,
            saveUninitialized: true
        }));
    },

    loginSession: function (req, username) {
        req.session.username = username;
    },

    logoutSession: function(req) {
        delete req.session.username;
    },

    authenticateSession: function(req) {
        if(req.session.username) {
            return req.session.username;
        } else {
            return null;
        }
    }
}

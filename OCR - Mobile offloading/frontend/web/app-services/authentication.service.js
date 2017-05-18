(function () {
    'use strict';

    angular
        .module('app1')
        .factory('AuthenticationService', AuthenticationService);

    AuthenticationService.$inject = ['$http', '$cookieStore', '$rootScope', '$timeout','CryptoService','CookieService'];
    function AuthenticationService($http, $cookieStore, $rootScope, $timeout, CryptoService, CookieService) {
        var service = {};

        service.Login = Login;
        service.SetCredentials = SetCredentials;
        service.ClearCredentials = ClearCredentials;

        return service;

        function Login(username, password, type) {
            if(type && type == 'facebook')  return $http.get('/login/facebook');
            
            var passwordSHA1 = CryptoService.SHA1Hash(password);
             
            return $http.post('/login', { username: username, password: password });
                

        }

        function SetCredentials(username, password, token) {
            $rootScope.globals = {
                currentUser: {
                    username: username,
                    authdata: token 
                }
            };

            $http.defaults.headers.common['Authorization'] = 'Basic ' + token; 
            CookieService.set('globals', $rootScope.globals, { expires : 1 });
            console.log('SETTING globals', $rootScope.globals);
            console.log('getting globals',  CookieService.get('globals'));
            //$cookieStore.put('globals', $rootScope.globals);
        }

        function ClearCredentials() {
            $rootScope.globals = {};
            CookieService.remove('globals');
            //$cookieStore.remove('globals');
            $http.defaults.headers.common.Authorization = 'Basic';
        }
    }

   

    
    
})();
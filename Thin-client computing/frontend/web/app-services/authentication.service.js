(function () {
    'use strict';

    angular
        .module('app')
        .factory('AuthenticationService', AuthenticationService);

    AuthenticationService.$inject = ['$http', '$cookieStore', '$rootScope', '$timeout','CryptoService'];
    function AuthenticationService($http, $cookieStore, $rootScope, $timeout, CryptoService) {
        var service = {};

        service.Login = Login;
        service.SetCredentials = SetCredentials;
        service.ClearCredentials = ClearCredentials;

        return service;

        function Login(username, password, callback) {
            var passwordSHA1 = CryptoService.SHA1Hash(password);
             
            return $http.post('/login', { username: username, password: passwordSHA1 });
                

        }

        function SetCredentials(username, password, token) {
            $rootScope.globals = {
                currentUser: {
                    username: username,
                    authdata: token
                }
            };

            $http.defaults.headers.common['Authorization'] = 'Basic ' + token; 
            $cookieStore.put('globals', $rootScope.globals);
        }

        function ClearCredentials() {
            $rootScope.globals = {};
            $cookieStore.remove('globals');
            $http.defaults.headers.common.Authorization = 'Basic';
        }
    }

   

    
    
})();
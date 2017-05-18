(function () {
    'use strict';

    var cookieConstant = function(){
        return window.Cookies;
    }

    angular
        .module('app1', ['ngRoute', 'ngCookies', 'ngWebSocket'])
        .factory('COOKIES', cookieConstant)
        .config(config)
        .run(run);

    
    config.$inject = ['$routeProvider', '$locationProvider'];
    function config($routeProvider, $locationProvider) {
        $routeProvider
            .when('/', {
                controller: 'HomeController',
                templateUrl: 'home/home.view.html',
                controllerAs: 'vm'
            })

            .when('/login', {
                controller: 'LoginController',
                templateUrl: 'login/login.view.html',
                controllerAs: 'vm'
            })

            .when('/register', {
                controller: 'RegisterController',
                templateUrl: 'register/register.view.html',
                controllerAs: 'vm'
            })
            .when('/image/:id', {
                controller: 'ImageController',
                templateUrl: 'images/image.view.html',
                controllerAs: 'vm'
            })
            .otherwise({ redirectTo: '/login' });
    }

    run.$inject = ['$rootScope', '$location', '$cookieStore', '$http','CookieService', 'COOKIES'];
    function run($rootScope, $location, $cookieStore, $http, CookieService, COOKIES) {
        // keep user logged in after page refresh

        $rootScope.globals = COOKIES.get('globals') ? JSON.parse(COOKIES.get('globals')) : {};
        if ($rootScope.globals.currentUser) {
            $http.defaults.headers.common['Authorization'] = 'Basic ' + $rootScope.globals.currentUser.authdata; 
        }

        $rootScope.$on('$locationChangeStart', function (event, next, current) {
            // redirect to login page if not logged in and trying to access a restricted page
            var restrictedPage = $.inArray($location.path(), ['/login', '/register']) === -1;
            var loggedIn = $rootScope.globals['currentUser'];
            console.log('$rootScope.globals 909', $rootScope.globals);
            if (restrictedPage && !loggedIn) {
                $location.path('/login');
            }
        });
    }

})();
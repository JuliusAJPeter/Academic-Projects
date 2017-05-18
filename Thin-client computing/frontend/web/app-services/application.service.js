(function () {
    'use strict';

    angular
        .module('app')
        .factory('ApplicationService', ApplicationService);

    ApplicationService.$inject = ['$http', '$q', '$timeout', '$rootScope'];
    function ApplicationService($http, $q, $timeout, $rootScope) {
        var service = {};


        function GetAppliactionList() {
            /* Dummy applications for testing, uses $timeout to simulate api call
             ----------------------------------------------*/
           /*var deferred = $q.defer();

            $timeout(function () {
                var response;
                deferred.resolve([{name:'Incscape'}, {name:'OpenOffice'}, {name:'Random'}]);
            }, 1000);
            return deferred.promise;*/

            var token = null; 
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.post('/listInstance', {appToken: token});
        }
        function StartApplication(name) {
            /* Dummy applications for testing, uses $timeout to simulate api call
             ----------------------------------------------*/
            /*var deferred = $q.defer();

            $timeout(function () {
                var response;
                deferred.resolve({ipAddress:123});
            }, 1000);
            return deferred.promise;
            */
            var token = null;
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.post('/appStart', {appName:name, appToken: token});
        }
        function StopApplication(name) {
            /* Dummy applications for testing, uses $timeout to simulate api call
             ----------------------------------------------*/
            /*var deferred = $q.defer();

            $timeout(function () {
                var response;
                deferred.resolve(true);
            }, 1000);
            return deferred.promise;*/
            var token = null;
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.post('/appStop', {instancename: name, appToken: token});
        }


        return {
            GetAppliactionList : GetAppliactionList,
            StopApplication:StopApplication,
            StartApplication:StartApplication
        };

    }

})();

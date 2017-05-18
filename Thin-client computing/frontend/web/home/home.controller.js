(function () {
    'use strict';

    angular
        .module('app')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$rootScope', '$scope', 'GeolocationService', 'ApplicationService','$location','FlashService'];
    function HomeController($rootScope, $scope, GeolocationService, ApplicationService, $location, FlashService) {
        var vm = this;

        vm.application;
        vm.availableApplications = [];
        vm.user = null;
        vm.allUsers = [];

        vm.loadingPosition = true;
        vm.loadingApplications = true;
        var positionObj = null;


        initController();

        function initController() {
            loadCurrentUser();
            calculateLocation();
            initApplicationList();
        }

        function initApplicationList(){
            ApplicationService.GetAppliactionList()
             .then(function(success){
                vm.availableApplications = success.data;
                vm.application = success.data[0].name;
                vm.loadingApplications = false;
            }, function(error){
                FlashService.Error('Could not get the list of applications');
            })
            .finally(function(){
            });

            
            vm.availableApplications
        }
        function calculateLocation(){
            GeolocationService.getCurrentPosition()
            .then(function(success){
                positionObj = success;
            }, function(error){
                console.log(error);
            })
            .finally(function(){
                if(positionObj != null){
                    var isInCampusArea = GeolocationService.isInRange(positionObj.coords);
                    if(isInCampusArea){
                        vm.availableApplications = orderApplicationsByLocation(vm.availableApplications);
                        vm.application = vm.availableApplications[0].name;
                        vm.loadingPosition = false; 
                    }
                }
                vm.loadingPosition = false; 
                
            });
        }

        function orderApplicationsByLocation(locations){
            var list1 = [];
            var list2 = [];
            for (var i = 0; i < locations.length; i++) {
                if(locations[i].name.toLowerCase() === 'openoffice' ){
                    list1.push(locations[i]);     
                }else{
                    list2.push(locations[i]);
                }

             } 
            return list1.concat(list2);
        }

        vm.runApplication = function ( ) {
            var path = '/vrdesktop/' + vm.application;
            $location.path(path);
        };

        function loadCurrentUser() {
            vm.username = $rootScope.globals.currentUser.username
        }

    }

})();
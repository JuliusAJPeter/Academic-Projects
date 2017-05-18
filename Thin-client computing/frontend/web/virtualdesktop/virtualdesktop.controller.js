(function () {
    'use strict';

    angular
        .module('app')
        .controller('VirtualDesktopController', VirtualDesktopController);

    VirtualDesktopController.$inject = ['$rootScope', '$scope', 'GeolocationService', 
        '$routeParams', 'WebSocketService', 'ApplicationService', '$location', '$timeout', 'FlashService'];
    function VirtualDesktopController($rootScope, $scope, GeolocationService,
         $routeParams, WebSocketService, ApplicationService, $location, $timeout, FlashService) {
        console.log('routeParams', $routeParams);
        var vm = this;

        vm.selectedApplication = $routeParams.id;
        vm.vrReady = false;
        vm.address;
        vm.pass;
        vm.port;
        vm.connectionAddress;

        var socketStream = WebSocketService.connectTo('127.0.0.1','3030');
      
        

        function initController() {
            startApplication(vm.selectedApplication);
            /*$timeout(function() {
                socketStream.close(true);
            }, 60000);*/
        }
        initController();

        vm.closeApplication = function(){
            closeApplication(vm.selectedApplication);
        }
        vm.logout = function(){
            socketStream.close(true);
            $location.path('#/login');            
        }
        function startApplication(appName){

            ApplicationService.StartApplication(appName.toLowerCase())
            .then(function(success){
                console.log(success);
                FlashService.clearMessages();
                vm.selectedApplication = appName;
                vm.address = success.data.ip;
                vm.port = success.data.port_w;
                vm.pass = success.data.pass;
                
                vm.connectionAddress = 'virtualdesktop/vnc_auto.html?host='+vm.address+'&port='+vm.port+'&password='+vm.pass;
                console.log(' vm.connectionAddress', vm.connectionAddress);
                /* create the virtual machine iframe dynamically here. update not needed
                var iframe = document.createElement('iframe');
                iframe.src = vm.connectionAddress;
                iframe.width=800;
                iframe.height=600;
                iframe.allowfullscreen=true;
                iframe.frameborder= 0;
                document.getElementById('iframeContainer1').appendChild(iframe);
                */
                vm.vrReady = true;
            }, function(error){
                console.log(error);
                FlashService.Error('Could not start the application or application still not ready. Trying to start the application again');
                startApplication(appName);
            })
            .finally(function(){

            });
            
        }
        function closeApplication(appName, logout){
            console.log('appname', appName);
            ApplicationService.StopApplication(appName.toLowerCase())
            .then(function(success){
                console.log(success);
                vm.selectedApplication = null;
                
                FlashService.Success('Successfully closed the application, '+ appName + '. Redirecting...');
                
            }, function(error){
                console.log(error);
                //FlashService.Error('Could not close the application');
            })
            .finally(function(){
                $timeout(function() {
                    if(logout){
                        $location.path('#/login');
                    } else {
                        $location.path('/');
                    }
                }, 1500);
            });
        }

    }

})();
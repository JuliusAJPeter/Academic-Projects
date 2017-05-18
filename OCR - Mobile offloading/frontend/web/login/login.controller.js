(function () {
    'use strict';

    angular
        .module('app1')
        .controller('LoginController', LoginController);

    LoginController.$inject = ['$location', 'AuthenticationService', 'FlashService'];
    function LoginController($location, AuthenticationService, FlashService) {
        var vm = this;

        vm.login = login;
        vm.facebookLogin = false;

        (function initController() {
            // reset login status
            AuthenticationService.ClearCredentials();
        })();

        function login() {
            // AuthenticationService.SetCredentials('iiro', 'vm.password', 'abdc123');
            //    $location.path('/');
            vm.dataLoading = true;
            var type = vm.facebookLogin ? 'facebook':null;
            AuthenticationService.Login(vm.username, vm.password, type)
            .then(function(success){
                console.log(success.data.token);
                AuthenticationService.SetCredentials(vm.username, vm.password, success.data.token);
                $location.path('/');
            }, function(error){
                console.log(error);
                FlashService.Error('Wrong username or password');
                    
            })
            .finally(function(){
                vm.dataLoading = false;
            });

        };
    }

})();

(function () {
    'use strict';

    angular
        .module('app1')
        .controller('ImageController', ImageController);

    ImageController.$inject = ['$location', 'AuthenticationService', 'FlashService', '$rootScope', 'ImageService'];
    function ImageController($location, AuthenticationService, FlashService, $rootScope, ImageService) {
        var vm = this;

        vm.user = null;
        vm.allUsers = [];

        vm.loading = true;
        vm.files = [];
        vm.userFile = {};
        initController();

        function initController() {
            loadCurrentUser();
            getFile();
        }

        function loadCurrentUser() {
            vm.username = $rootScope.globals.currentUser.username
        }
        function getFile(){
            console.log($rootScope.fileSelected);
            vm.selectedFile = $rootScope.fileSelected;
            console.log('vm.selectedFile.sourceID', vm.selectedFile.sourceID);
            ImageService.GetImage(vm.selectedFile.sourceID)
            .then( function(success){ 
                console.log('success', success);
                vm.userFile = success.data[0];
            }, function(error){
                console.log('error', error);
            })
            .finally( function(){
                vm.userFile.text = vm.selectedFile.text;
                vm.userFile.time = vm.selectedFile.time;
                vm.userFile.createdTime = vm.selectedFile.createTs;

                vm.loading = false;
            });
        }
        
       
    }

})();

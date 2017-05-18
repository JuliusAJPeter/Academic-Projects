(function () {
    'use strict';

    angular
        .module('app1')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$rootScope', '$scope', 'ImageService','$location','FlashService', '$window'];
    function HomeController($rootScope, $scope, ImageService, $location, FlashService, $window) {
        var vm = this;

        vm.user = null;
        vm.allUsers = [];

        vm.loading = true;
        vm.files = [];
        vm.userFiles = [];
        vm.selectFile = function (file) {
            $rootScope.fileSelected = file;
            console.log('rootscope ',$rootScope.fileSelected );
            $location.path('/image/' + file.sourceID);
        };
        initController();

        function initController() {
            loadCurrentUser();
            getUserFiles();
        }

        function loadCurrentUser() {
            vm.username = $rootScope.globals.currentUser.username
        }
        function getUserFiles(){
           /* vm.userFiles = [
                {
                    sourceID : 0,
                    text:'test',
                    imagePath:'http://placekitten.com/g/200/200'
                },
                {
                    text:'test2',
                    imagePath:'http://placekitten.com/g/300/300'
                }
            ];
            */
            ImageService.GetImages(vm.username)
            .then( function(success){ 
                console.log('success', success);
                vm.userFiles = success.data;
            }, function(error){
                console.log('error', error);
            })
            .finally( function(){
                vm.loading = false;
            });
            
        }
        function uploadImagesFront(){
            window.Tesseract.recognize(vm.files,{
            progress: function(e){
                console.log(e)
            }
            }).then( function(d){ document.getElementById('display').innerHTML+=d.text } )
        }
        function uploadImagesBackend(){
            console.log('test',1234);
            console.log('vm.files', vm.files);
            ImageService.UploadImages(vm.files)
            .then( function(success){ 
                console.log('success', success);
            }, function(error){
                console.log('error', error);
            });
        }
        vm.uploadImages = uploadImagesFront;
        vm.uploadImagesBackend = uploadImagesBackend;

        $window.Tesseract = Tesseract.create({
            workerPath: '../tesseract/worker.js',
            langPath: 'https://cdn.rawgit.com/naptha/tessdata/gh-pages/3.02/',
            corePath: 'https://cdn.rawgit.com/naptha/tesseract.js-core/0.1.0/index.js',
        })

    }

})();
(function () {
    'use strict';

    angular.module('app1')
    .directive('fileInput', ['$parse', function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, element, attributes) {
                element.bind('change', function () {
                    console.log(element[0].files);
                    $parse(attributes.fileInput)
                    .assign(scope,element[0].files)
                    scope.$apply()
                });
            }
        };
    }]);

})();

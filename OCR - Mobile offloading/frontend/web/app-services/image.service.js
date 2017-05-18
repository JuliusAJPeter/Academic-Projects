(function () {
    'use strict';

    angular
        .module('app1')
        .factory('ImageService', ImageService);

    ImageService.$inject = ['$http', '$q', '$timeout', '$rootScope'];
    function ImageService($http, $q, $timeout, $rootScope) {
        var service = {};

        function UploadImages(imageList) {

            var token = null; 
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.post('https://130.211.52.123:443/image', {appToken: token, images:imageList});
        }
        function GetImages(username) {
            var token = null; 
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.get('/store', { params: {appToken: token}});
        }
        function GetImage(id) {
            var token = null; 
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.get('/source', { params: {appToken: token, id: id}});
        }
        /*function SaveImages(imageList) {
            // imagelist should be this format: JSON array of objects: {image: [image], text: [text of image] }
            var token = null; 
            if($rootScope.globals.currentUser){
                token = $rootScope.globals.currentUser.authdata;
            }
            return $http.post('/source', {appToken: token});
        }*/

        
        return {
            UploadImages : UploadImages,
            GetImages : GetImages,
            GetImage : GetImage
        };

    }

})();

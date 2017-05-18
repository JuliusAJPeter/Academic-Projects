(function () {
    'use strict';

    angular
        .module('app1')
        .factory('CookieService', CookieService);

 	function CookieService() {
 		return window.Cookies;
 	}

    
})();
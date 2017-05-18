(function () {
    'use strict';

    angular
        .module('app')
        .factory('GeolocationService', GeolocationService);

    GeolocationService.$inject = ['$q','$window' ,'$timeout'];

     function GeolocationService($q, $window, $timeout) {

        'use strict';

        function getCampusPosition(){
            // lang and long of konemiehentie 60.186654, 24.822166
            var position = {
                coords: {
                    latitude: 60.186654,
                    longitude:24.822166
                }
            };
            return position;
        }
        function getFurthestAllowedPosition(){ 
            // lang and long from konemiehentie furthest allowed position
            var position = {
                coords: {
                    latitude: 60.187341, 
                    longitude: 24.820638
                }
            };
            return position;
        }
        function getMaxDistance(){
            var campusPos = getCampusPosition();
            var maxAllowed = getFurthestAllowedPosition();
            return getDistance(maxAllowed.coords, campusPos.coords);
        }

        function isInRange(currentPosition){
            var campusPos = getCampusPosition();
            // mock distance should be inside:
            /* var mockCoordsInRange = {
                latitude: 60.187218,
                longitude: 24.821542
            };
            var mockCoordsNotRange = {
                latitude: 60.189394,
                longitude: 24.818634
            }
            */
            return (getDistance(currentPosition, campusPos.coords) <= getMaxDistance());
        }

        function getCurrentPosition() {
            var deferred = $q.defer();

            if (!$window.navigator.geolocation) {
                deferred.reject('Geolocation not supported.');
            } else {
                $window.navigator.geolocation.getCurrentPosition(
                    function (position) {
                        deferred.resolve(position);
                    },
                    function (err) {
                        deferred.reject(err);
                    });
            }

            $timeout(function() {
                deferred.reject('Geolocation could not be read.');
            }, 10000);

            return deferred.promise;
        }
        var getRadius = function(x) {
            return x * Math.PI / 180;
        };

        var getDistance = function(p1, p2) {
            var R = 6378137; // Earth’s mean radius in meter
            var dLat = getRadius(p2.latitude - p1.latitude);
            var dLong = getRadius(p2.longitude - p1.longitude);
            var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(getRadius(p1.latitude)) * Math.cos(getRadius(p2.latitude)) *
                Math.sin(dLong / 2) * Math.sin(dLong / 2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            var d = R * c;

            return d; // returns the distance in meter
        };

        return {
            getCurrentPosition: getCurrentPosition,
            isInRange: isInRange
        };
    }
    
})();
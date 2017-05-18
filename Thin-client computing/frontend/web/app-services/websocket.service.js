(function () {
    'use strict';

    angular
    .module('app')
    .factory('WebSocketService', WebSocketService);

    WebSocketService.$inject = ['$websocket'];
    function WebSocketService($websocket) {
        // Open a WebSocket connection
        function connectStream(ip, port){
            return $websocket('ws://'+ip+':'+port);
        }
        
        return {
            connectTo: connectStream
        }
    }
})();
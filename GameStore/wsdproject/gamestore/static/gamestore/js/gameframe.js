/*Function for receiving the messages
Includes the ajax call for Django and Setting changes to Gameframe*/
function receiveMessage(event)
{
  console.log(event.type);
  console.log(JSON.stringify(event.data));
  if (event.data.messageType =="SCORE" ||event.data.messageType =="SAVE" || event.data.messageType =="LOAD_REQUEST"||
    event.data.messageType =="LOAD"||event.data.messageType =="ERROR"||event.data.messageType =="SETTING"){
  callAjax(event.data.messageType, JSON.stringify(event.data),"POST");
  }
  if(event.data.messageType =="SETTING"){
      console.log(event.data.options.width);
      document.getElementById("gameframe").style.width =event.data.options.width+"px";

      document.getElementById("gameframe").style.height =event.data.options.height+"px";
    }


}
// Got from https://docs.djangoproject.com/en/1.10/ref/csrf/
function getCookie(name) {
    var cookieValue = null;
    if (document.cookie && document.cookie !== "") {
        var cookies = document.cookie.split(";");
        for (i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i]);
            // Does this cookie string begin with the name we want?
            if (cookie.substring(0, name.length + 1) === (name + "=")) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}
function callAjax(destination,jsonString,callType){
  console.log(window.location.href+destination);

  //For CSRFToken
  $.ajaxSetup({
    beforeSend: function(xhr, settings) {
        if (callType == "POST") {
            xhr.setRequestHeader("X-CSRFToken", getCookie("csrftoken"));
        }
    }
    });
  /*The ajax call, which will be send to django and also send to iframe */
  $.ajax({
    url : window.location+destination+"/",
    type : callType,
    data : jsonString,
     success : function(json) {
    document.getElementById("gameframe").contentWindow.postMessage(json,document.getElementById("gameframe").src);
   

   },
     error : function(xhr,errmsg,err) {
       console.log(xhr.responseText);
   }
 });
  
}
/*The listener for messages*/
window.addEventListener("message", receiveMessage, false);
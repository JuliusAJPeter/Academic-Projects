<?php



// CONFIG
$allowedUsers = [
    "amazing" => "alpacas"
];
$apps = [
    "is" => "Inkscape",
    "oo" => "OpenOffice"
];

error_reporting(0);

authenticate();


$pathInfo = $_SERVER["PATH_INFO"];
$pathInfo = trim($pathInfo, "/");
$pathParts = explode('/', $pathInfo);


if (count($pathParts) == 0) {
    send404Error();
}

$firstPart = array_shift($pathParts);
// POST /apps
// return JSON with apps list
if ($firstPart == "apps" && count($pathParts) == 0) {
    appsAction($pathParts);
}
// POST /apps/{app_name}/start
// return JSON with VNC connection details
else if ($firstPart == "app" && count($pathParts) == 2 &&
        array_key_exists($pathParts[0], $apps) && $pathParts[1] == "start") {
    vncConnectAction($pathParts[0]);
}
else {
    send404Error();
}
exit;

function send403Error() {
    header($_SERVER["SERVER_PROTOCOL"]." 403 Not Authorized", true, 403);
    print("not authorized");
    exit;
}

function send404Error() {
    header($_SERVER["SERVER_PROTOCOL"]." 404 Not Found", true, 404);
    print("not found");
    exit;
}

function authenticate() {
    global $allowedUsers;

    if (empty($_POST["user"]) || empty($_POST["pass"])) {
        send403Error();
    }

    $user = $_POST["user"];
    $pass = $_POST["pass"];

    // super simple authentication
    if (!array_key_exists($user, $allowedUsers) || $pass != $allowedUsers[$user]) {
        send403Error();
    }

    return true;
}

function appsAction($params = []) {
    global $apps;
    print(json_encode($apps));
}

function vncConnectAction($app) {
    global $apps;

    if (!array_key_exists($app, $apps)) {
        send404Error();
    }

    // TODO start app and send back address of the correct app
    print(json_encode([
        "app" => $apps[$app],
        "ip" => "", // TODO fill server ip
        "port" => "" // TODO fill vnc port
    ]));

}

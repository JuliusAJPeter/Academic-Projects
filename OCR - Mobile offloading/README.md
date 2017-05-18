# OCR - Mobile offloading
Mobile Cloud Computing - Academic project

Collaboration
* 5 member team (fall 2016)

This project shows how computationally expensive tasks can be offloaded to a cloud service. The presented backend cloud service and corresponding frontend application (Android and web-based) allow the user to perform Optical character recognition (OCR) from a mobile device, either directly on the device or in the cloud.

Following functions are available:

* log in into an account (maintained by the cloud service),
* select a picture from a gallery or take a photo with the built-in camera and
* perform OCR detection on the image, either locally, remotely in the cloud, or in benchmark mode, comparing the two techniques,
* save the resulting text to a file,
* view history of previous OCR requests and view the thumbnail, resulting text, creation time and source image (if available).

## Special features to note

* All communication between the mobile device and the cloud service is done by HTTPS with a self-signed certificate.
* If the device is offline, the login screen is by-passed and only local OCR functionality is available. The history of older requests is loaded from local cache. (Android)
* Multiple images can be selected for OCR at the same time, if the built-in gallery supports it. (Android)

## Installation

1. Clone the repository or move files to Google Cloud
2. Run `./backend/install.sh`
This will install all the dependencies needed, create a docker file for the server, start it in 3 instances, and also start a Mongo database in 3 instances over Kubernetes. When the script finishes, it shows the ip address of the server, where a user can connect to with their browser.
3. Clone repository to local computer with Android SDK installed
4. Go to `./frontend` and run `./install.sh`

## Running

Credentials to use in the app:

* User: `user`
* Password: `pass`

## API
The communication between the mobile device and the cloud service happens through a RESTful API, running on HTTPS protocol.

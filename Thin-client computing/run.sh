 #!bin/bash
if type node; then
	echo Node already installed.
else
	echo Starting to install node.js
	startDir=`pwd`
	echo "$startDir"
	mkdir ~/node
	mkdir ~/local
	cd ~/node
	curl http://nodejs.org/dist/node-latest.tar.gz | tar xz --strip-components=1
	./configure --prefix=~/local
	make install
	cd ~/local/bin
	dire=`pwd`
	sana="PATH=$Path:$dire"
	export $sana
	cd ~/node
	curl -L https://www.npmjs.org/install.sh | sh
	cd $startDir
fi
cd backend/
echo Updating required packages.
npm install
echo Packages updated.
export GOOGLE_APPLICATION_CREDENTIALS="credentials/AmazingAlpacas mcc-2016-g09-p1-ff77d64e6bc0.json"
node appServer.js




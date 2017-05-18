## CONFIG

# set up correct server IP here
IP=104.155.37.233

## ANDROID DEPLOYMENT
# Requires installed Android SDK

# Save IP to Android resources file
echo '<?xml version="1.0" encoding="utf-8"?><resources><string name="base_url">'"$IP"'</string></resources>' \
    > android/app/src/main/res/values/url.xml

# Compile APK
cd android
chmod 774 gradlew
echo "./gradlew assembleDebug"

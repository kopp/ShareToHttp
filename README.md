# ShareToHttp
Android App to transfer texts and images quickly from your Android Device to a web browser, e.g. on a PC.

![travis build status](https://travis-ci.org/kopp/ShareToHttp.svg?branch=master)


# Installation

You can download a signed APK from the
[releases](https://github.com/kopp/ShareToHttp/releases)
section.
To install it you need to enable installation from "unknown sources" in Android settings.


# Usage

- Share text and image(s) with this app.
- The app will start a web server.
- Launch a Browser on a machine in the same WIFI network, using the address displayed in the app.
- Download text/images using the web browser.
- Kill the app using the convenient button.


# Known limitations

- Sometimes, Emojis are not rendered properly, but instead four strange characters are displayed.
  Removing some characters in front of the Emoji will make them render correctly.


# Components used

- This app uses the MIT licensed
  [Android Web Server (FireFly) by sonuauti](https://github.com/sonuauti/Android-Web-Server/).

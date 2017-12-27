counter-android-client
========================

This repository contains a simple Android app based [on the code](https://github.com/fidesmo/fidesmo-android-tutorial) for the Android NFC tutorial at [Fidesmo Developer Portal](https://developer.fidesmo.com/tutorials/android). 

Functionality
-------------
The app implements a user interface towards the [Counter Applet](https://github.com/fidesmo/counter-applet), which is running on a [Fidesmo Card](https://developer.fidesmo.com/fidesmocard). It implements two simple functions:
- A button to show the current value of the counter, as stored on the card. 
- A button to decrement the counter. If the counter's value was 0, the card will return an error. The UI reflects it by showing a red error sign.

Build instructions: command line
------------------
- Clone this repository
- In the project's root directory, type ``./gradlew build``
- To install the app into a connected phone or an emulator, type ``./gradlew installDebug`` or ``adb install build/apk/counter-android-client-debug-unaligned.apk``

Build instructions: Android Studio IDE
------------------
- Clone this repository
- In Android Studio, go to menu File -> Import Project
- In the dialog box "Select Gradle Project Import", find this project's root directory in your filesystem and click 'OK'

Releases
--------
The code in this repository is not written to be released, only as an example to be shared with a customer.


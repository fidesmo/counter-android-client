counter-android-client
========================

This repository contains a simple Android app based [on the code](https://github.com/fidesmo/fidesmo-android-tutorial) for the Android NFC tutorial at [Fidesmo Developer Portal](https://developer.fidesmo.com/tutorials/android). Build instructions are in the linked repository.

Functionality
-------------
The app implements a user interface towards the [Counter Applet](https://github.com/fidesmo/counter-applet), which is running on a [Fidesmo Card](https://developer.fidesmo.com/fidesmocard). It implements two simple functions:
- A button to show the current value of the counter, as stored on the card. 
- A button to decrement the counter. If the counter's value was 0, the card will return an error. The UI reflects it by showing a red error sign.
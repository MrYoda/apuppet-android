# aPuppet: free and open source remote control of Android devices

This is the Android application - agent for sharing the Android device screen, sending the screencast to the server, and playing gestures sent by the user controlling the device.

aPuppet website: https://apuppet.org

The aPuppet project is completely open source, both client and server modules are available. 

## Mobile device management

The aPuppet project is sponsored by [Headwind MDM](https://h-mdm.com), the open source mobile device management system for Android. 

Since you're interested in the remote control of Android devices, consider using Headwind MDM in your company. It is easily installed and makes all your Android devices controllable from a single server.

The aPuppet Premium:

* is seamlessly integrated into Headwind MDM as a module;
* automatically starts by a command from the remote server;
* doesn't require user interaction and is suitable for kiosk devices;
* supports any HTTPS certificates.

## Building aPuppet

To build aPuppet, open the project in Android Studio, place the SDK location in the *local.properties* file, and build the project. 

Once the project built successfully, you can set up your aPuppet server URL and secret in the *app/build.gradle*. This will simplify the initial setup of the application by setting your server as default.

## Running the app

The application uses accessibility services to play the remote gestures. Please enable accessibility services when the application prompts.

While sharing the screen, the application displays a flashing green dot in the top left corner. This dot doesn't only display that your screen is casting to a remote peer, it generates a small video traffic stabilizing the picture and keeping the client alive. To enable the dot, allow the aPuppet agent to draw overlays (display on top of other apps).

## Compatibility

aPuppet is using native Android API and is therefore compatible with all Android devices and builds since Android 7 and above. AOSP and custom Android OS are also supported.

More details about the software and purchasing the premium version can be found at https://apuppet.org.
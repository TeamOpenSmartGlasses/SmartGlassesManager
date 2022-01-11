main/ is the main Android application. Open and run this in Android Studio

mediapipe/ is the mobile/edge AIML system, which is a (stickied) fork of the Google MediaPipe library and Android example program. The holistic android app and holisitic graph have been extended to include a number of new neural networks and processes. 

If you want to edit the application, go here: `main/`
If you want to edit mediapipe library, go here: `mediapipe/`

You can either use the officially released APK (on Github or <emexwearables.com>) or build your own locally after following the instructions in the main README.md and then the instruction below:

## Install and build

### Main Application

Open, build, and run the app in `main/` from Android Studio, just like any other Android app.

### Mediapipe AAR

1. Follow these instructions to setup Bazel and MediaPipe: https://google.github.io/mediapipe/getting_started/android.html (including the external link on this page on how to install MediaPipe)
    - don't forget to follow these instructions on that same page: https://google.github.io/mediapipe/getting_started/install.html
3. Change the SDK and NDK in ./main/WORKSPACE to point to your own Android SDK install (if you don't have one, install Android Studio and download an SDK and NDK)
4. Run this command:
```
bazel build -c opt --config=android_arm64 --java_runtime_version=1.8 --noincremental_dexing --verbose_failures mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:wearableai;
```
5. You have now built the application!
6. For subsequent builds where you don't change anything in WORKSPACE file, use the following command for faster build:
```
bazel build -c opt --config=android_arm64 --java_runtime_version=1.8 --noincremental_dexing --verbose_failures --fetch=false mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:wearableai;
```

## Architecture

The system uses JSON IPC between the ASP and ASG.

#### Files
`MainActivity.java` - in charge of the UI, launching the background service
`WearableAiAspService.java` - where everything happens. This launches connections, moves data around, and stays alive in the background.
`ASGRepresentative.java` - a system that communicates with the ASG
`GLBOXRepresentative.java` - a system that communicates with the GLBOX

#### Comms/Events/Data

Data and function calls are passed around the application on an _event bus_. Right now, we are using RXJAVA as the event bus, with our own custom parsing, and all event keys can be found in `/commes/MessageTypes.java`.

Instead of calling functions directly, which requires passing many objects around and becomes too complex with a big system like this, we only pass around the "dataObservable" rxjava object, which handles sending data and triggering messages anywhere in the app. These events are multicast, so multiple different systems can respond to the same message.

* Soon, we'll move RXJAVA to Android EventBus

### Autociter - temporary note
For right now, the number we send references to is hardcoded in nlp/WearableReferencerAutocite.java. Change the line that says "PUT NUMBER HERE" to contain the phone number you wish to send to. This will soon be changed to be user-supplied value in the ASP ui.

Further, for now, to add references to your database, update the Csv in assets/assets/wearable_referencer_references.csv. This also will be moved to a user-facing ui shortly.
 
### Note / WARNING

There may be some issues with hard links to Android Studio executables in some Bazel configs in main/ app. We are working on making everything easy for dev setup, but if you get weird errors on running the above command, try to change hard links to point to your android studio/android sdk/android ndk, then make an issue on Github or reach out to cayden@emexwearables.com

## Speech Recognition / ASR / Vosk

We are using the vosk-api for local speech recognition.

Since we use our own model, we have our own android library with an assets folder that holds the model, and we import that android library as a dependency in our main app.

We can (obviously) use the main Vosk android model: `vosk-model-small-en-us-0.15`  
However we have successfully tested using both `vosk-model-en-us-0.22` and `vosk-model-en-us-0.22-lgraph`. The problem with `vosk-model-en-us-0.22` is that it makes the build time ~10 minutes, which is unreasonable. For now we will use `vosk-model-en-us-0.22-lgraph`, and later we will change the system to automatically download `vosk-model-en-us-0.22` from our server so the model doesn't have to be packed into the APK, but we can get the best recognition possible. We may also want to give users option on which to use, as older/slow devices may have a hard time with the month 2Gb+ model that is `vosk-model-en-us-0.22`.

## Wearable AI Pipeline

[Google MediaPipe](https://github.com/google/mediapipe) is way to define intellgience graphs ("perception pipelines") which take input, do intelligence processing (by creating flow of data between machine learning models and hard coded functions known as "Calculators"). This app is built on the Google MediaPipe even though ./main/ is not currently tracking Google MediaPipe repo. In the future, if we want to pull in new work from the main MediaPipe repository, we will set things up again to track Google Mediapipe.

## Notes

Keras-VGG16-places365/ is the places365 system converted to a tensorflowlite model for our WearableAI graph that is currently running on the ASP

## TODO

- save face rec bounding box in database and in face encoding object 
    - and display cropped face in face rec ui, so we can tag multiple multiple people

## References / Acknowledgements

- Build system taken from MediaPipe
- Facial recognition from: https://github.com/shubham0204/FaceRecognition_With_FaceNet_Android

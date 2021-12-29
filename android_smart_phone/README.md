main/ is the main system, which is a (stickied) fork of the Google MediaPipe library and Android example program. The holistic android app and holisitic graph have been extended to include a number of new neural networks and processes. Because Bazel is complex and there's no benefit to changing things yet, we develop our main app in the Android examples folder, mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai  

REPEAT, if you want to edit the application, go here: `mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai`

You can either use the officially released APK (on Github or <emexwearables.com>) or build your own locally after following the instructions in the main README.md and then the instruction below:

## Install and build

1. Follow these instructions to setup Bazel and MediaPipe: https://google.github.io/mediapipe/getting_started/android.html (including the external link on this page on how to install MediaPipe)
    - don't forget to follow these instructions on that same page: https://google.github.io/mediapipe/getting_started/install.html
3. Change the SDK and NDK in ./main/WORKSPACE to point to your own Android SDK install (if you don't have one, install Android Studio and download an SDK and NDK)
4. Run this command:
```
./build_single_android.sh mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai
```
5. You have now built the application!
 
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

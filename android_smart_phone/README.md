Keras-VGG16-places365/ is the places365 system converted to a tensorflowlite model for our WearableAI graph that is currently running on the ASP

main/ is the main system, which is a fork of the Google MediaPipe library and Android example program. The app and holisitic graph have been extended to include a number of new neural networks (e.g. facial emotion) and processes (e.g. server connection to ASG)

You can either use the officially released APK (on Github or <emexwearables.com>) or build your own locally after following the instructions in the main README.md and then the instruction below:

## Install and build

1. Follow these instructions: https://google.github.io/mediapipe/getting_started/android.html (including the external link on this page on how to install MediaPipe)
2. Change the SDK and NDK in ./main/WORKSPACE to point to your own Android SDK install (if you don't have one, install Android Studio and download an SDK and NDK)
3. Run this command:
```
./build_single_android.sh mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai
```

### Wearable AI Pipeline

[Google MediaPipe](https://github.com/google/mediapipe) is way to define intellgience graphs ("perception pipelines") which take input, do intelligence processing (by creating flow of data between machine learning models and hard coded functions known as "Calculators"). This app is built on the Google MediaPipe even though ./main/ is not currently tracking Google MediaPipe repo. In the future, if we want to pull in new work from the main MediaPipe repository, we will set things up again to track Google Mediapipe.

### Note

There may be some issues with hard links to Android Studio executables in some Bazel configs in main/ app. We are working on making everything easy for dev setup, but if you get weird errors on running the above command, make an issue on Github or reach out to cayden@emexwearables.com

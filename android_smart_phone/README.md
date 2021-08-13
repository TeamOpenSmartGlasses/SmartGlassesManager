Keras-VGG16-places365/ is the places365 system converted to a tensorflowlite model for our WearableAI graph that is currently running on the ASP

main/ is the main system, which is a fork of the Google MediaPipe library and Android example program. The app and holisitic graph have been extended to include a number of new neural networks (e.g. facial emotion) and processes (e.g. server connection to ASG)

You can either use the officially released APK (on Github or <emexwearables.com>) or build your own locally after following the instructions in the main README.md and then running the following command:

```
./build_single_android.sh mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai
```

### Note

There may be some issues with hard links to Android Studio executables in some Bazel configs in main/ app. We are working on making everything easy for dev setup, but if you get weird errors on running the above command, make an issue on Github or reach out to cayden@emexwearables.com

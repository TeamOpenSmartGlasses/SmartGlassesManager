# Copyright 2019 The MediaPipe Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

licenses(["notice"])

package(default_visibility = ["//visibility:private"])

cc_binary(
    name = "libmediapipe_jni.so",
    linkshared = 1,
    linkstatic = 1,
    deps = [
        "//mediapipe/graphs/wearable_ai_gpu:wearable_ai_gpu_deps",
        "//mediapipe/java/com/google/mediapipe/framework/jni:mediapipe_framework_jni",
    ],
)

cc_library(
    name = "mediapipe_jni_lib",
    srcs = [":libmediapipe_jni.so"],
    alwayslink = 1,
)

#android_binary(
#    name = "wearableai",
#    srcs = glob(["*.java", "LandmarksTranslate/*.java"]),
#    assets = [
#        "//mediapipe/graphs/wearable_ai_gpu:wearable_ai_gpu.binarypb",
#        "//mediapipe/modules/face_landmark:face_landmark.tflite",
#        "//mediapipe/modules/iris_landmark:iris_landmark.tflite",
#        "//mediapipe/modules/hand_landmark:hand_landmark.tflite",
#        "//mediapipe/modules/facial_emotion_recognition:face_emotion.tflite",
#        "//mediapipe/modules/facial_emotion_recognition:test_emotion.tflite",
#        "//mediapipe/modules/facial_emotion_recognition:iris_landmark_two.tflite",
#        "//mediapipe/modules/hand_landmark:handedness.txt",
#        "//mediapipe/modules/holistic_landmark:hand_recrop.tflite",
#        "//mediapipe/modules/pose_detection:pose_detection.tflite",
#        "//mediapipe/modules/pose_landmark:pose_landmark_upper_body.tflite",
#        "//mediapipe/modules/pose_landmark:pose_landmark_full_body.tflite",
#        "//mediapipe/graphs/holistic_tracking:holistic_tracking_gpu.binarypb",
#        "//mediapipe/modules/face_detection:face_detection_front.tflite",
#    ],
#    assets_dir = "",
#    manifest = "AndroidManifest.xml",
#    resource_files = glob(["res/**"]),
#    manifest_values = {
#        "applicationId": "com.google.mediapipe.apps.wearableai",
#        "appName": "Wearable AI",
#        "mainActivity": ".MainActivity",
#        "cameraFacingFront": "True",
#        "binaryGraphName": "wearable_ai_gpu.binarypb",
#        "inputVideoStreamName": "input_video",
#        "outputVideoStreamName": "output_video",
#        "flipFramesVertically": "True",
#        "converterNumBuffers": "2",
#    },
#    multidex = "native",
#    deps = [
#            ":wearableai_deps",
#            "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai/database/phrase:phrase",
#
#    ],
#)

android_binary(
    name = "wearableai",
    custom_package = "com.google.mediapipe.apps.wearableai",
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "applicationId": "com.google.mediapipe.apps.wearableai",
        "appName": "Wearable AI",
        "mainActivity": ".MainActivity",
        "cameraFacingFront": "True",
        "binaryGraphName": "wearable_ai_gpu.binarypb",
        "inputVideoStreamName": "input_video",
        "outputVideoStreamName": "output_video",
        "flipFramesVertically": "True",
        "converterNumBuffers": "2",
    },
    multidex = "native",
    deps = [
        "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:wearableai_main",
        #"//mediapipe/examples/android/src/java/com/google/mediapipe/apps/vosk_22_lgraph:vosk_models",
    ],
    #visibility = [":__subpackages__"],
    visibility = ["//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:__subpackages__"],
)

android_library(
        name = "wearableai_main",
        #javacopts = ["-Xlint"],
        manifest = "AndroidManifest.xml",
        custom_package = "com.google.mediapipe.apps.wearableai",
        srcs = glob(["**/*.java"]),
        resource_files = glob(["res/**"]),
        deps = [
            "@maven//:net_java_dev_jna_jna",
            ":androidx_room_room_compiler_library",
            ":mediapipe_jni_lib",
            "//mediapipe/framework/formats:landmark_java_proto_lite",
            "//mediapipe/java/com/google/mediapipe/components:android_camerax_helper",
            "//mediapipe/java/com/google/mediapipe/components:android_components",
            "//mediapipe/java/com/google/mediapipe/framework:android_framework",
            "//mediapipe/java/com/google/mediapipe/glutil",
            "//third_party:androidx_appcompat",
            "//third_party:androidx_core",
            "//third_party:androidx_constraint_layout",
            "//third_party:androidx_material",
            "//third_party:androidx_recyclerview",
            "//third_party:opencv",
            "//third_party:androidx_annotation",
            "@maven//:org_java_websocket_Java_WebSocket",
            "@maven//:io_reactivex_rxjava3_rxandroid",
            "@maven//:io_reactivex_rxjava3_rxjava",
            "@maven//:com_google_android_gms_play_services_location",
            "@maven//:com_google_android_gms_play_services_tasks",
            "@maven//:com_google_code_gson_gson",
            "@maven//:androidx_legacy_legacy_support_v4",
            "@maven//:androidx_sqlite_sqlite",
            "@maven//:androidx_lifecycle_lifecycle_extensions",
            "@maven//:androidx_lifecycle_lifecycle_service",
            "@maven//:androidx_lifecycle_lifecycle_viewmodel",
            "@maven//:androidx_lifecycle_lifecycle_livedata_core",
            "@maven//:androidx_room_room_common",
            "@maven//:androidx_room_room_runtime",
            "@maven//:com_alphacephei_vosk_android",
            "@maven//:com_alphacephei_vosk_model_en",
            "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai/jniLibs:jna_libs",
            "@maven//:androidx_navigation_navigation_fragment",
            "@maven//:androidx_navigation_navigation_ui",
            "@maven//:androidx_navigation_navigation_runtime",
            "@maven//:androidx_fragment_fragment",
            "@maven//:info_debatty_java_string_similarity",
        ],
    assets = [
        "//mediapipe/graphs/wearable_ai_gpu:wearable_ai_gpu.binarypb",
        "//mediapipe/modules/face_landmark:face_landmark.tflite",
        "//mediapipe/modules/iris_landmark:iris_landmark.tflite",
        "//mediapipe/modules/hand_landmark:hand_landmark.tflite",
        "//mediapipe/modules/facial_emotion_recognition:face_emotion.tflite",
        "//mediapipe/modules/facial_emotion_recognition:test_emotion.tflite",
        "//mediapipe/modules/facial_emotion_recognition:iris_landmark_two.tflite",
        "//mediapipe/modules/hand_landmark:handedness.txt",
        "//mediapipe/modules/holistic_landmark:hand_recrop.tflite",
        "//mediapipe/modules/pose_detection:pose_detection.tflite",
        "//mediapipe/modules/pose_landmark:pose_landmark_upper_body.tflite",
        "//mediapipe/modules/pose_landmark:pose_landmark_full_body.tflite",
        "//mediapipe/graphs/holistic_tracking:holistic_tracking_gpu.binarypb",
        "//mediapipe/modules/face_detection:face_detection_front.tflite",
    ],
    assets_dir = "",

    #visibility = [":__subpackages__"],
    visibility = ["//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:__subpackages__"],
)

java_plugin(
    name = "androidx_room_room_compiler_plugin",
    processor_class = "androidx.room.RoomProcessor",
    deps = [
        "@maven//:androidx_room_room_compiler",
    ],
)

java_library(
    name = "androidx_room_room_compiler_library",
    exported_plugins = [
        ":androidx_room_room_compiler_plugin",
    ],
    neverlink = True,
    exports = [
        "@maven//:androidx_room_room_compiler",
    ],
)

#android_library(
#    name = "wearableai_deps",
#    exports = [
#        ":mediapipe_jni_lib",
#        "//mediapipe/framework/formats:landmark_java_proto_lite",
#        "//mediapipe/java/com/google/mediapipe/components:android_camerax_helper",
#        "//mediapipe/java/com/google/mediapipe/components:android_components",
#        "//mediapipe/java/com/google/mediapipe/framework:android_framework",
#        "//mediapipe/java/com/google/mediapipe/glutil",
#        "//third_party:androidx_appcompat",
#        "//third_party:androidx_core",
#        "//third_party:androidx_constraint_layout",
#        "//third_party:androidx_material",
#        "//third_party:androidx_recyclerview",
#        "//third_party:opencv",
#        "//third_party:androidx_annotation",
#        "@maven//:org_java_websocket_Java_WebSocket",
#        "@maven//:io_reactivex_rxjava3_rxandroid",
#        "@maven//:io_reactivex_rxjava3_rxjava",
#        "@maven//:com_google_android_gms_play_services_location",
#        "@maven//:com_google_android_gms_play_services_tasks",
#        "@maven//:com_google_code_gson_gson",
#        "@maven//:androidx_legacy_legacy_support_v4",
#        ],
#)
#



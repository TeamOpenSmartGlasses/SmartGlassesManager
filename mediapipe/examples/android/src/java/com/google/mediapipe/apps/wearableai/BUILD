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

android_binary(
    name = "wearableai",
    srcs = glob(["*.java"]),
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
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
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
        ":mediapipe_jni_lib",
        "//mediapipe/framework/formats:landmark_java_proto_lite",
        "//mediapipe/java/com/google/mediapipe/components:android_camerax_helper",
        "//mediapipe/java/com/google/mediapipe/components:android_components",
        "//mediapipe/java/com/google/mediapipe/framework:android_framework",
        "//mediapipe/java/com/google/mediapipe/glutil",
        "//third_party:androidx_appcompat",
        "//third_party:androidx_constraint_layout",
        "//third_party:androidx_legacy_support_v4",
        "//third_party:androidx_material",
        "//third_party:androidx_recyclerview",
        "//third_party:opencv",
        "//third_party:androidx_annotation",
    ],
)

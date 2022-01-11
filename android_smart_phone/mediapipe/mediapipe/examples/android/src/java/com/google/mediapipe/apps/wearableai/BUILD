# HOW THIS WORKS

# rules_kotlin is being used in mixed-mode to compile both java and kotlin sources as an andrid library
# then our main app imports that library
# a few of the deps are listed in in the main app because rules_kotlin refuses to build Java sources without some specific thing ("JavaInfo", or something)

package(default_visibility = ["//visibility:private"])

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")


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
    custom_package = "com.google.mediapipe.apps.wearableai",
    manifest = "AndroidManifest.xml",
    nocompress_extensions = ["tflite"],
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
        "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai/jniLibs:jna_libs",
        #"//mediapipe/examples/android/src/java/com/google/mediapipe/apps/vosk_22_lgraph:vosk_models",
        ":mediapipe_jni_lib",
        "//third_party:opencv",
    ],
    #visibility = [":__subpackages__"],
    visibility = ["//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:__subpackages__"],
)

kt_android_library(
        name = "wearableai_main",
        manifest = "AndroidManifest.xml",
        custom_package = "com.google.mediapipe.apps.wearableai",
        srcs = glob(["**/*.kt", "**/*.java"]),
        resource_files = glob(["res/**"]),
        deps = [
            #face rec lib,
            '@maven//:androidx_core_core_ktx',
            ##TensorFlow Lite dependencies,
            '@maven//:org_tensorflow_tensorflow_lite_2_4_0',
            '@maven//:org_tensorflow_tensorflow_lite_gpu_2_4_0',
            '@maven//:org_tensorflow_tensorflow_lite_support_0_1_0',
            ##Kotlin Coroutines,
            '@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_1_4_1',
            '@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_android_1_4_1',
            ##DocumentFile and ExitInterface,
            "@maven//:androidx_documentfile_documentfile_1_0_1",
            "@maven//:androidx_exifinterface_exifinterface_1_3_3",
            #materialui
            "//third_party:androidx_constraint_layout",
            "//third_party:androidx_material",
            ## MLKit Face Detection
            '@maven//:com_google_mlkit_face_detection',
            #nav + fragment
            "@maven//:androidx_navigation_navigation_fragment",
            "@maven//:androidx_navigation_navigation_ui",
            "@maven//:androidx_navigation_navigation_runtime",
            "@maven//:androidx_fragment_fragment",
            "@maven//:org_jetbrains_kotlin_kotlin_reflect",
            "@maven//:com_google_guava_guava",

            "@maven//:net_java_dev_jna_jna",
            ":androidx_room_room_compiler_library",
            "//mediapipe/framework/formats:landmark_java_proto_lite",
            "//mediapipe/java/com/google/mediapipe/components:android_camerax_helper",
            "//mediapipe/java/com/google/mediapipe/components:android_components",
            "//mediapipe/java/com/google/mediapipe/framework:android_framework",
            "//mediapipe/java/com/google/mediapipe/glutil",
            "//third_party:androidx_appcompat",
            "//third_party:androidx_core",
            "//third_party:androidx_recyclerview",
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
            "@maven//:androidx_lifecycle_lifecycle_livedata_ktx",
            "@maven//:androidx_room_room_common",
            "@maven//:androidx_room_room_runtime",
            "@maven//:com_alphacephei_vosk_android",
            "@maven//:com_alphacephei_vosk_model_en",
            "@maven//:info_debatty_java_string_similarity",
            "@maven//:org_threeten_threetenbp",
            #CSV
            "@maven//:com_opencsv_opencsv",
            #HTTP
            "@maven//:com_android_volley_volley",
            #SPECIAL DEP - this is our normal assets folder
            "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai/assets:wearableai_assets",

                ],
#        assets = glob(["kotlin_assets/*"]),
#        assets_dir = "kotlin_assets/",
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

        visibility = ["//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:__subpackages__"],
        )
#
#android_library(
#        name = "wearableai_main",
#        #javacopts = ["-Xlint"],
#        manifest = "AndroidManifest.xml",
#        custom_package = "com.google.mediapipe.apps.wearableai",
#        srcs = glob(["**/*.java"]),
#        resource_files = glob(["res/**"]),
#        deps = [
#            "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:wearableai_kotlin",
#            "@maven//:net_java_dev_jna_jna",
#            ":androidx_room_room_compiler_library",
#            ":mediapipe_jni_lib",
#            "//mediapipe/framework/formats:landmark_java_proto_lite",
#            "//mediapipe/java/com/google/mediapipe/components:android_camerax_helper",
#            "//mediapipe/java/com/google/mediapipe/components:android_components",
#            "//mediapipe/java/com/google/mediapipe/framework:android_framework",
#            "//mediapipe/java/com/google/mediapipe/glutil",
#            "//third_party:androidx_appcompat",
#            "//third_party:androidx_core",
#            "//third_party:androidx_constraint_layout",
#            "//third_party:androidx_material",
#            "//third_party:androidx_recyclerview",
#            "//third_party:opencv",
#            "//third_party:androidx_annotation",
#            "@maven//:org_java_websocket_Java_WebSocket",
#            "@maven//:io_reactivex_rxjava3_rxandroid",
#            "@maven//:io_reactivex_rxjava3_rxjava",
#            "@maven//:com_google_android_gms_play_services_location",
#            "@maven//:com_google_android_gms_play_services_tasks",
#            "@maven//:com_google_code_gson_gson",
#            "@maven//:androidx_legacy_legacy_support_v4",
#            "@maven//:androidx_sqlite_sqlite",
#            "@maven//:androidx_lifecycle_lifecycle_extensions",
#            "@maven//:androidx_lifecycle_lifecycle_service",
#            "@maven//:androidx_lifecycle_lifecycle_viewmodel",
#            "@maven//:androidx_lifecycle_lifecycle_livedata_core",
#            "@maven//:androidx_room_room_common",
#            "@maven//:androidx_room_room_runtime",
#            "@maven//:com_alphacephei_vosk_android",
#            "@maven//:com_alphacephei_vosk_model_en",
#            "//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai/jniLibs:jna_libs",
#            "@maven//:androidx_navigation_navigation_fragment",
#            "@maven//:androidx_navigation_navigation_ui",
#            "@maven//:androidx_navigation_navigation_runtime",
#            "@maven//:androidx_fragment_fragment",
#            "@maven//:info_debatty_java_string_similarity",
#            "@maven//:org_threeten_threetenbp",
#        ],
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
#
#    #visibility = [":__subpackages__"],
#    visibility = ["//mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai:__subpackages__"],
#)

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



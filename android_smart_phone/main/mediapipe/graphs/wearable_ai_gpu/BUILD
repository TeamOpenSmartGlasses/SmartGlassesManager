# Copyright 2020 The MediaPipe Authors.
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

load(
    "//mediapipe/framework/tool:mediapipe_graph.bzl",
    "mediapipe_binary_graph",
    "mediapipe_simple_subgraph",
)

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

mediapipe_simple_subgraph(
    name = "holistic_tracking_to_render_data",
    graph = "holistic_tracking_to_render_data.pbtxt",
    register_as = "HolisticTrackingToRenderData",
    deps = [
        "//mediapipe/calculators/core:concatenate_normalized_landmark_list_calculator",
        "//mediapipe/calculators/core:concatenate_vector_calculator",
        "//mediapipe/calculators/core:merge_calculator",
        "//mediapipe/calculators/core:split_normalized_landmark_list_calculator",
        "//mediapipe/calculators/core:split_vector_calculator",
        "//mediapipe/calculators/util:detections_to_render_data_calculator",
        "//mediapipe/calculators/util:landmarks_to_render_data_calculator",
        "//mediapipe/calculators/util:rect_to_render_data_calculator",
        "//mediapipe/calculators/util:rect_to_render_scale_calculator",
        "//mediapipe/framework/tool:switch_container",
        "//mediapipe/modules/holistic_landmark:hand_wrist_for_pose",
    ],
)

cc_library(
    name = "wearable_ai_gpu_deps",
    deps = [
        ":holistic_tracking_to_render_data",
        "//mediapipe/calculators/core:constant_side_packet_calculator",
        "//mediapipe/calculators/core:flow_limiter_calculator",
        "//mediapipe/calculators/image:image_properties_calculator",
        "//mediapipe/calculators/util:annotation_overlay_calculator",
        "//mediapipe/modules/holistic_landmark:holistic_landmark_gpu",
        "//mediapipe/graphs/iris_tracking/calculators:update_face_landmarks_calculator",
        "//mediapipe/graphs/iris_tracking/subgraphs:iris_and_depth_renderer_gpu",
        "//mediapipe/modules/iris_landmark:iris_landmark_left_and_right_gpu",
        "//mediapipe/modules/facial_emotion_recognition:facial_emotion_rec",
    ],
)

mediapipe_binary_graph(
    name = "wearable_ai_gpu",
    graph = "wearable_ai_gpu.pbtxt",
    output_name = "wearable_ai_gpu.binarypb",
    deps = [":wearable_ai_gpu_deps"],
)

cc_library(
    name = "holistic_tracking_cpu_graph_deps",
    deps = [
        ":holistic_tracking_to_render_data",
        "//mediapipe/calculators/core:constant_side_packet_calculator",
        "//mediapipe/calculators/core:flow_limiter_calculator",
        "//mediapipe/calculators/image:image_properties_calculator",
        "//mediapipe/calculators/util:annotation_overlay_calculator",
        "//mediapipe/modules/holistic_landmark:holistic_landmark_cpu",
    ],
)

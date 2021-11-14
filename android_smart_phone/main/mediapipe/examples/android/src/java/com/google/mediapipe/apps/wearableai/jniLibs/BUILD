licenses(["notice"])

package(default_visibility = ["//visibility:private"])

config_setting(
  name = "x86",
  values = {
    "cpu": "x86",
  }
)

config_setting(
  name = "x86_64",
  values = {
    "cpu": "x86_64",
  }
)

config_setting(
  name = "armeabi-v7a",
  values = {
    "cpu": "armeabi-v7a",
  }
)

config_setting(
  name = "arm64-v8a",
  values = {
    "cpu": "arm64-v8a",
  }
)

cc_import(
  name = "jna_libs",
  shared_library = select({
    ":x86": "x86/libjnidispatch.so",
    ":x86_64": "x86_64/libjnidispatch.so",
    ":armeabi-v7a": "armeabi-v7a/libjnidispatch.so",
    ":arm64-v8a": "arm64-v8a/libjnidispatch.so",
  }),
  visibility = ["//visibility:public"],
)

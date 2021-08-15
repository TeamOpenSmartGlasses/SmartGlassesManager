// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#import <Foundation/Foundation.h>

#include <fstream>
#include <sstream>

#include "absl/strings/match.h"
#include "mediapipe/framework/port/ret_check.h"
#include "mediapipe/util/resource_util.h"

namespace mediapipe {

namespace {
mediapipe::StatusOr<std::string> PathToResourceAsFileInternal(
    const std::string& path) {
  NSString* ns_path = [NSString stringWithUTF8String:path.c_str()];
  Class mediapipeGraphClass = NSClassFromString(@"MPPGraph");
  NSString* resource_dir =
      [[NSBundle bundleForClass:mediapipeGraphClass] resourcePath];
  NSString* resolved_ns_path =
      [resource_dir stringByAppendingPathComponent:ns_path];
  std::string resolved_path = [resolved_ns_path UTF8String];
  RET_CHECK([[NSFileManager defaultManager] fileExistsAtPath:resolved_ns_path])
      << "cannot find file: " << resolved_path;
  return resolved_path;
}
}  // namespace

mediapipe::StatusOr<std::string> PathToResourceAsFile(const std::string& path) {
  // Return full path.
  if (absl::StartsWith(path, "/")) {
    return path;
  }

  // Try to load a relative path or a base filename as is.
  {
    auto status_or_path = PathToResourceAsFileInternal(path);
    if (status_or_path.ok()) {
      LOG(INFO) << "Successfully loaded: " << path;
      return status_or_path;
    }
  }

  // If that fails, assume it was a relative path, and try just the base name.
  {
    const size_t last_slash_idx = path.find_last_of("\\/");
    CHECK_NE(last_slash_idx, std::string::npos);  // Make sure it's a path.
    auto base_name = path.substr(last_slash_idx + 1);
    auto status_or_path = PathToResourceAsFileInternal(base_name);
    if (status_or_path.ok()) LOG(INFO) << "Successfully loaded: " << base_name;
    return status_or_path;
  }
}

mediapipe::Status GetResourceContents(const std::string& path,
                                      std::string* output,
                                      bool read_as_binary) {
  if (!read_as_binary) {
    LOG(WARNING) << "Setting \"read_as_binary\" to false is a no-op on ios.";
  }
  ASSIGN_OR_RETURN(std::string full_path, PathToResourceAsFile(path));

  std::ifstream input_file(full_path);
  std::stringstream buffer;
  buffer << input_file.rdbuf();
  buffer.str().swap(*output);
  return mediapipe::OkStatus();
}

}  // namespace mediapipe
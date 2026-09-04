cmake_minimum_required(VERSION 3.22)
project(whisper_assistant_jni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

# src/whisper.cpp מצפה למאקרו הזה (בדרך כלל מוזרק ע"י ה-CMakeLists.txt
# הראשי של הפרויקט המקורי, שלא בשימוש כאן כי הוא מייבא גם git-vars/
# javascript bindings שלא רלוונטיים לבנייה הזו).
add_compile_definitions(WHISPER_VERSION="1.9.3-android")

add_subdirectory(ggml)
add_subdirectory(src)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_include_directories(whisper_jni PRIVATE include)
target_link_libraries(whisper_jni PRIVATE whisper ggml log)

#
# eSpeak NG (TTS) - מנוע דיבור מקומי, כי אין מנוע TTS מותקן בכלל במכשירי
# הבדיקה. בונים רק את הספרייה (espeak-ng, ucd, speechPlayer) - לא את
# הבינארי espeak-ng-bin/ה-CLI, שלא רלוונטי כאן.
#
set(PROJECT_VERSION "1.52.0")
set(PROJECT_VERSION_MAJOR "1")
set(ESPEAK_COMPAT OFF CACHE BOOL "" FORCE)
include(GNUInstallDirs)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/espeak/cmake")
include(deps)
include(config)

add_library(espeak-include INTERFACE)
target_include_directories(espeak-include INTERFACE espeak/include espeak/include/compat)
target_link_libraries(espeak-include INTERFACE ucd)

add_subdirectory(espeak/ucd-tools)
add_subdirectory(espeak/speechPlayer)
add_subdirectory(espeak/libespeak-ng)

add_library(espeak_jni SHARED espeak_jni.cpp)
target_link_libraries(espeak_jni PRIVATE espeak-ng log)

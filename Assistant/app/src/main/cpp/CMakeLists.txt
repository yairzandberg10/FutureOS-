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

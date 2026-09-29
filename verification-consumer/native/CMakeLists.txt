cmake_minimum_required(VERSION 3.14)
project(mmkv_ohos_native_smoke LANGUAGES CXX)

add_executable(mmkv-native-smoke MMKVSmoke.cpp)
target_include_directories(mmkv-native-smoke PRIVATE ../../Core/cbridge)
target_link_libraries(mmkv-native-smoke PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/../../KMP/mmkv/nativeInterop/build/ohosArm64/libmmkv-kmp.a
    z
    hilog_ndk.z
)

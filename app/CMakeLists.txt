cmake_minimum_required(VERSION 3.4.1)

add_library(myspeex SHARED src/main/cpp/myspeex.cpp)

target_include_directories(myspeex PRIVATE ${CMAKE_SOURCE_DIR}/src/main/cpp/include/)

add_library(speexdsp SHARED IMPORTED)
set_target_properties(speexdsp PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libspeexdsp.so)

find_library(log-lib log)

target_link_libraries(myspeex ${log-lib} speexdsp)
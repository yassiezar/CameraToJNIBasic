cmake_minimum_required(VERSION 3.11.0)

include_directories(src/main/cpp/include/)
add_library( camera
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/camera.cpp )

find_library( # Sets the name of the path variable.
              log-lib
              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

find_library( # Sets the name of the path variable.
              window-lib
              # Specifies the name of the NDK library that
              # you want CMake to locate.
              jnigraphics )

find_library( # Sets the name of the path variable.
            android-lib
            # Specifies the name of the NDK library that
            # you want CMake to locate.
            android )

target_link_libraries( # Specifies the target library.
                       camera
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       ${window-lib}
                       ${android-lib} )
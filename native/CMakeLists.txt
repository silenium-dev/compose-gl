cmake_minimum_required(VERSION 3.16)
if (NOT DEFINED PROJECT_NAME)
    set(PROJECT_NAME "compose-gl")
endif ()

if (NOT DEFINED JAVA_HOME)
    if (DEFINED ENV{JAVA_HOME})
        set(JAVA_HOME "$ENV{JAVA_HOME}")
    endif ()
endif ()

if (NOT DEFINED JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME must be defined")
else ()
    message(STATUS "JAVA_HOME: ${JAVA_HOME}")
endif ()

project(${PROJECT_NAME} LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(SKIA INTERFACE)
target_include_directories(SKIA INTERFACE
        "${CMAKE_SOURCE_DIR}/third_party/skia/include"
        "${CMAKE_SOURCE_DIR}/third_party/skia/modules/svg/include"
        "${CMAKE_SOURCE_DIR}/third_party/skia/src"
        "${CMAKE_SOURCE_DIR}/third_party/skia"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/angle2/include"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/freetype/include"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/harfbuzz/src"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/icu/source/common"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/libpng"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/libwebp/src"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/swiftshader/include"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/externals/zlib"
        "${CMAKE_SOURCE_DIR}/third_party/skia/third_party/icu"
)
target_link_directories(SKIA INTERFACE "${CMAKE_SOURCE_DIR}/third_party/skia/out/Release-windows-x64")
target_link_libraries(SKIA INTERFACE
        bentleyottmann.lib libwebp.lib skottie.lib skshaper.lib svg.lib
        d3d12allocator.lib icu.lib libwebp_sse41.lib skparagraph.lib skunicode_core.lib wuffs.lib
        expat.lib libjpeg.lib skcms.lib skresources.lib skunicode_icu.lib zlib.lib
        harfbuzz.lib libpng.lib skia.lib sksg.lib spirv_cross.lib
)

set(SOURCES

)

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    if (NOT DEFINED JAVA_HOME)
        set(JAVA_HOME "/usr/lib/jvm/java-11-openjdk")
    endif ()

    list(APPEND SOURCES
            src/cpp/linux/GLXContext.cpp
    )
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    list(APPEND SOURCES
            src/cpp/windows/D3DInterop.cpp
            src/cpp/windows/WGLContext.cpp
    )
endif ()

add_library(${PROJECT_NAME} SHARED ${SOURCES})
target_include_directories(${PROJECT_NAME} PRIVATE "src/cpp/")

message(STATUS "JAVA_HOME: ${JAVA_HOME}")
target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include")

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_compile_definitions(${PROJECT_NAME} PRIVATE -D_LINUX)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GL REQUIRED IMPORTED_TARGET gl egl glx)
    target_link_libraries(${PROJECT_NAME} PUBLIC PkgConfig::GL)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/linux")
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    target_compile_definitions(${PROJECT_NAME} PRIVATE -D_WINDOWS)
    target_compile_definitions(${PROJECT_NAME} PRIVATE SK_DIRECT3D NOMINMAX WIN32_LEAN_AND_MEAN)
    target_compile_options(${PROJECT_NAME} PRIVATE /MT)
    target_link_libraries(${PROJECT_NAME} PUBLIC opengl32 dxgi d3d12 d3dcompiler)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/win32")
    target_link_libraries(${PROJECT_NAME} PUBLIC SKIA)
endif ()

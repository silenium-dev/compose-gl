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

set(SOURCES
        src/cpp/library.cpp
)

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    if (NOT DEFINED JAVA_HOME)
        set(JAVA_HOME "/usr/lib/jvm/java-11-openjdk")
    endif ()

    list(APPEND SOURCES
    )
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    list(APPEND SOURCES
            src/cpp/windows/D3DInterop.cpp
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
    include(cmake/skia.cmake)
    target_compile_definitions(${PROJECT_NAME} PRIVATE -D_WINDOWS)
    target_compile_definitions(${PROJECT_NAME} PRIVATE SK_DIRECT3D NOMINMAX WIN32_LEAN_AND_MEAN)
    target_compile_options(${PROJECT_NAME} PRIVATE /MT)
    target_link_libraries(${PROJECT_NAME} PUBLIC opengl32 dxgi d3d12 d3dcompiler)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/win32")
    target_link_libraries(${PROJECT_NAME} PUBLIC Skia)
endif ()

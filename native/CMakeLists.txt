cmake_minimum_required(VERSION 3.16)
if (NOT DEFINED PROJECT_NAME)
    set(PROJECT_NAME "compose-gl")
endif ()
if (NOT DEFINED JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME must be defined")
else ()
    message(STATUS "JAVA_HOME: ${JAVA_HOME}")
endif ()

project(${PROJECT_NAME} LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

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
    target_link_libraries(${PROJECT_NAME} PUBLIC opengl32 dxgi d3d11 d3dcompiler)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/win32")
endif ()

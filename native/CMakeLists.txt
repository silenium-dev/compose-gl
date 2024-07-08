cmake_minimum_required(VERSION 3.21)
project(compose-gl LANGUAGES CXX)

file(GLOB_RECURSE SOURCES CONFIGURE_DEPENDS "src/*.cpp" "src/*.h" "src/*.hpp")
add_library(compose-gl SHARED ${SOURCES})

find_package(PkgConfig REQUIRED)
pkg_check_modules(GL REQUIRED IMPORTED_TARGET gl glx egl)
target_link_libraries(compose-gl PRIVATE PkgConfig::GL)
target_include_directories(compose-gl PRIVATE "/usr/lib/jvm/java-21-openjdk/include" "/usr/lib/jvm/java-21-openjdk/include/linux")

cmake_minimum_required(VERSION 3.20)

project(compose-gl LANGUAGES CXX)

add_library(compose-gl SHARED library.cpp)
target_link_libraries(compose-gl PUBLIC EGL)

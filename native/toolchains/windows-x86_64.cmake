set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR x64)
# workaround for https://gitlab.kitware.com/cmake/cmake/-/issues/19409
set(CMAKE_GENERATOR_PLATFORM x64 CACHE INTERNAL "")
set(CMAKE_C_COMPILER_TARGET x64-windows-msvc)
set(CMAKE_CXX_COMPILER_TARGET x64-windows-msvc)
set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang++)

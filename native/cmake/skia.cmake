set(SKIA_VERSION "m132-a00c390e98-1" CACHE STRING "Skia version")
set(SKIA_OS "linux")
set(SKIA_ARCH "x64")
set(SKIA_VARIANT "Debug")

if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(SKIA_VARIANT "Debug")
endif ()

if (CMAKE_BUILD_TYPE STREQUAL "Release")
    set(SKIA_VARIANT "Release")
endif ()

if (CMAKE_BUILD_TYPE STREQUAL "RelWithDebInfo")
    set(SKIA_VARIANT "Debug")
endif ()

if (CMAKE_BUILD_TYPE STREQUAL "MinSizeRel")
    set(SKIA_VARIANT "Release")
endif ()

if (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set(SKIA_OS "windows")
elseif (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    set(SKIA_OS "linux")
elseif (CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set(SKIA_OS "macos")
endif ()

string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" PROCESSOR)

if (PROCESSOR MATCHES "arm" OR PROCESSOR MATCHES "aarch64")
    set(SKIA_ARCH "arm64")
elseif (PROCESSOR MATCHES "x86_64" OR PROCESSOR MATCHES "amd64")
    set(SKIA_ARCH "x64")
endif ()

file(MAKE_DIRECTORY "${CMAKE_BINARY_DIR}/download")

file(DOWNLOAD "https://api.github.com/repos/JetBrains/skia-pack/releases/tags/${SKIA_VERSION}" "${CMAKE_BINARY_DIR}/download/skia_assets.json" STATUS GH_RELEASE_STATUS SHOW_PROGRESS)
list(GET GH_RELEASE_STATUS 0 GH_RELEASE_STATUS_CODE)
if (GH_RELEASE_STATUS_CODE)
    message(FATAL_ERROR "Failed to download GitHub release: ${GH_RELEASE_STATUS_CODE}")
    return()
endif ()

file(READ "${CMAKE_BINARY_DIR}/download/skia_assets.json" GH_RELEASE_JSON)

string(JSON ASSETS_COUNT LENGTH ${GH_RELEASE_JSON} "assets")
math(EXPR ASSETS_COUNT "${ASSETS_COUNT} - 1")

foreach (assetIdx RANGE 0 ${ASSETS_COUNT} 1)
    string(JSON assetName GET ${GH_RELEASE_JSON} "assets" "${assetIdx}" "name")
    if (assetName MATCHES "Skia-${SKIA_VERSION}-${SKIA_OS}-${SKIA_VARIANT}-${SKIA_ARCH}.zip")
        message(STATUS "Found asset ${assetIdx} of ${ASSETS_COUNT}: ${assetName}")
        set(ASSET_IDX ${assetIdx})
        set(ASSET_NAME ${assetName})
        string(JSON ASSET_URL GET ${GH_RELEASE_JSON} "assets" "${assetIdx}" "browser_download_url")
        string(JSON ASSET_HASH GET ${GH_RELEASE_JSON} "assets" "${assetIdx}" "digest")
        break()
    endif ()
endforeach ()

message(STATUS "Downloading Skia from ${ASSET_URL} with hash ${ASSET_HASH}")

set(SKIA_URL "${ASSET_URL}")
string(REGEX REPLACE ":.*" "" ASSET_HASH_ALGO "${ASSET_HASH}")
string(REGEX REPLACE ".*:" "" ASSET_HASH_VALUE "${ASSET_HASH}")
string(TOUPPER "${ASSET_HASH_ALGO}" ASSET_HASH_ALGO)

if (NOT ASSET_HASH)
    message(WARNING "Failed to find Skia hash, just checking for the files existence to determine if we need to download Skia again.")
    if (NOT EXISTS "${CMAKE_BINARY_DIR}/download/${ASSET_NAME}")
        file(DOWNLOAD ${SKIA_URL} "${CMAKE_BINARY_DIR}/download/${ASSET_NAME}" STATUS SKIA_DOWNLOAD_STATUS SHOW_PROGRESS)
    endif ()
else ()
    file(DOWNLOAD ${SKIA_URL} "${CMAKE_BINARY_DIR}/download/${ASSET_NAME}" STATUS SKIA_DOWNLOAD_STATUS EXPECTED_HASH "${ASSET_HASH_ALGO}=${ASSET_HASH_VALUE}" SHOW_PROGRESS)
endif ()

list(GET SKIA_DOWNLOAD_STATUS 0 SKIA_DOWNLOAD_STATUS_CODE)
if (SKIA_DOWNLOAD_STATUS_CODE)
    message(FATAL_ERROR "Failed to download Skia: ${SKIA_DOWNLOAD_STATUS_CODE}")
    return()
endif ()

file(MAKE_DIRECTORY "${CMAKE_BINARY_DIR}/third_party/skia")
message(STATUS "Unpacking Skia to ${CMAKE_BINARY_DIR}/third_party/skia")
file(REMOVE_RECURSE "${CMAKE_BINARY_DIR}/third_party/skia")
file(ARCHIVE_EXTRACT INPUT "${CMAKE_BINARY_DIR}/download/${ASSET_NAME}" DESTINATION "${CMAKE_BINARY_DIR}/third_party/skia")

add_library(Skia INTERFACE)
target_include_directories(Skia INTERFACE
        "${CMAKE_BINARY_DIR}/third_party/skia/include"
        "${CMAKE_BINARY_DIR}/third_party/skia/modules/svg/include"
        "${CMAKE_BINARY_DIR}/third_party/skia/src"
        "${CMAKE_BINARY_DIR}/third_party/skia"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/angle2/include"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/freetype/include"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/harfbuzz/src"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/icu/source/common"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/libpng"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/libwebp/src"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/swiftshader/include"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/externals/zlib"
        "${CMAKE_BINARY_DIR}/third_party/skia/third_party/icu"
)
target_link_directories(Skia INTERFACE "${CMAKE_BINARY_DIR}/third_party/skia/out/${SKIA_VARIANT}-${SKIA_OS}-${SKIA_ARCH}")
target_link_libraries(Skia INTERFACE
        bentleyottmann.lib libwebp.lib skottie.lib skshaper.lib svg.lib
        d3d12allocator.lib icu.lib libwebp_sse41.lib skparagraph.lib skunicode_core.lib wuffs.lib
        expat.lib libjpeg.lib skcms.lib skresources.lib skunicode_icu.lib zlib.lib
        harfbuzz.lib libpng.lib skia.lib sksg.lib spirv_cross.lib
)

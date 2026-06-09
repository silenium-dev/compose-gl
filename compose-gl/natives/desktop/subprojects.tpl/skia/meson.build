project(
  'skia', 'cpp',
  meson_version : '>=1.2.0',
)

if host_machine.cpu_family() == 'aarch64'
  skiaArch = 'arm64'
elif host_machine.cpu_family() == 'x86_64'
  skiaArch = 'x64'
endif

if host_machine.system() == 'windows'
  skiaOs = 'windows'
elif host_machine.system() == 'linux'
  skiaOs = 'linux'
endif

buildType = get_option('build_type')
skia_lib_dir = meson.current_source_dir() / 'out' / buildType + '-' + skiaOs + '-' + skiaArch
if skiaOs == 'windows'
  linkArgs = [
    skia_lib_dir / 'bentleyottmann.lib',
    skia_lib_dir / 'd3d12allocator.lib',
    skia_lib_dir / 'expat.lib',
    skia_lib_dir / 'harfbuzz.lib',
    skia_lib_dir / 'icu.lib',
    skia_lib_dir / 'jsonreader.lib',
    skia_lib_dir / 'libjpeg.lib',
    skia_lib_dir / 'libjpeg12.lib',
    skia_lib_dir / 'libjpeg16.lib',
    skia_lib_dir / 'libpng.lib',
    skia_lib_dir / 'libwebp.lib',
    skia_lib_dir / 'libwebp_sse41.lib',
    skia_lib_dir / 'skcms.lib',
    skia_lib_dir / 'skia.lib',
    skia_lib_dir / 'skottie.lib',
    skia_lib_dir / 'skparagraph.lib',
    skia_lib_dir / 'skresources.lib',
    skia_lib_dir / 'sksg.lib',
    skia_lib_dir / 'skshaper.lib',
    skia_lib_dir / 'skunicode_core.lib',
    skia_lib_dir / 'skunicode_icu.lib',
    skia_lib_dir / 'spirv_cross.lib',
    skia_lib_dir / 'svg.lib',
    skia_lib_dir / 'wuffs.lib',
    skia_lib_dir / 'zlib.lib',
  ]
elif skiaOs == 'linux'
  linkArgs = [
    skia_lib_dir / 'libskparagraph.a',
    skia_lib_dir / 'libpng.a',
    skia_lib_dir / 'libjsonreader.a',
    skia_lib_dir / 'libskresources.a',
    skia_lib_dir / 'libwuffs.a',
    skia_lib_dir / 'libjpeg.a',
    skia_lib_dir / 'libskia.a',
    skia_lib_dir / 'libexpat.a',
    skia_lib_dir / 'libwebp.a',
    skia_lib_dir / 'libskunicode_icu.a',
    skia_lib_dir / 'libfreetype2.a',
    skia_lib_dir / 'libskshaper.a',
    skia_lib_dir / 'libpiex.a',
    skia_lib_dir / 'libdng_sdk.a',
    skia_lib_dir / 'libharfbuzz.a',
    skia_lib_dir / 'libbentleyottmann.a',
    skia_lib_dir / 'libjpeg12.a',
    skia_lib_dir / 'libskcms.a',
    skia_lib_dir / 'libsvg.a',
    skia_lib_dir / 'libsksg.a',
    skia_lib_dir / 'libskunicode_core.a',
    skia_lib_dir / 'libwebp_sse41.a',
    skia_lib_dir / 'libjpeg16.a',
    skia_lib_dir / 'libzlib.a',
    skia_lib_dir / 'libicu.a',
    skia_lib_dir / 'libskottie.a',
  ]
endif

skia_dep = declare_dependency(
  version : '2',
  link_args : linkArgs,
  include_directories : include_directories(
    './include',
    './modules/svg/include',
    './src',
    './',
    './third_party/externals/angle2/include',
    './third_party/externals/freetype/include',
    './third_party/externals/harfbuzz/src',
    './third_party/externals/icu/source/common',
    './third_party/externals/libpng',
    './third_party/externals/libwebp/src',
    './third_party/externals/swiftshader/include',
    './third_party/externals/zlib',
    './third_party/icu',
  ),
)

meson.override_dependency('skia', skia_dep)

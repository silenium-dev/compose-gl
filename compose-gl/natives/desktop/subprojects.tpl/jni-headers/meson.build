project(
  'jni-headers', 'cpp',
  meson_version : '>=1.2.0',
)

if host_machine.system() == 'windows'
  jniIncludeDir = 'win32'
elif host_machine.system() == 'linux'
  jniIncludeDir = 'linux'
endif

jni_version = get_option('jni_version')
jni_headers_dep = declare_dependency(
  include_directories: include_directories(
    './versions' / jni_version / 'include',
    './versions' / jni_version / 'include' / jniIncludeDir,
  )
)
meson.override_dependency('jni-headers', jni_headers_dep)

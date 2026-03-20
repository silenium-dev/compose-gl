skia_dep = skia.get_variable('skia_dep')
#jni = dependency('jni')
jni_dep = jni.get_variable('jni_dep')

deps = [jni_dep, skia_dep]
#deps = [jni]
link_args = []

sources = [
  'library.cpp'
]

os = host_machine.system()
if os == 'linux'
  add_project_arguments('-DTARGET_LINUX', language : 'cpp')
elif os == 'windows'
  sources += ['windows/D3DInterop.cpp']
  add_project_arguments('-DTARGET_WINDOWS', '/MT', language : 'cpp')
endif

shared_library(
  'compose-gl',
  sources,
  dependencies : deps,
  link_args : link_args,
  cpp_args : ['-Wimplicit-fallthrough'],
)

project('compose-gl',
        'cpp', 'java',
        default_options : {
          'cpp_std' : 'c++20',
          'wrap_mode' : 'forcefallback',
        },
        meson_version : '>=1.2.0',
)

os = host_machine.system()
if os == 'linux'
  add_global_arguments('-fPIC', language : 'c')
  add_global_arguments('-fPIC', language : 'cpp')
endif

skia = subproject('skia',
                  default_options : {
                    'wrap_mode' : 'forcefallback',
                    'default_library' : 'static',
                    'prefer_static' : true,
                  }
)
jni_headers = subproject('jni-headers',
                         default_options : {
                           'wrap_mode' : 'forcefallback',
                         }
)
subdir('src')

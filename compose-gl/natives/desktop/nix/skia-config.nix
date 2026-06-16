{ pkgs
}:
let
  skia = { os, arch, hash }: rec {
    version = "m144-22f58c9fd4";
    buildType = "Release";
    source_url = "https://nexus.silenium.dev/repository/github-releases/JetBrains/skia/releases/download/${version}/Skia-${version}-${os}-${buildType}-${arch}.zip";
    source_hash = hash;
    source_filename = "skia.zip";
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "x86_64-windows" = skia {
    os = "windows";
    arch = "x64";
    hash = "sha256-e3H0dAN+X3EhWihIPYkJ0jTUvRMCINt2DquVO5hlOk8=";
  };
  "aarch64-windows" = skia {
    os = "windows";
    arch = "arm64";
    hash = "sha256-9UiIDBu2BNo9dJ3w9U1u+ypVWI5jPr9ipb+QV2t8Cj8=";
  };
  "x86_64-linux" = skia {
    os = "linux";
    arch = "x64";
    hash = "sha256-02BG4wjcjqLn1Ph/uPRFM9rR4lLkm1vSItfQBTioJyI=";
  };
  "aarch64-linux" = skia {
    os = "linux";
    arch = "arm64";
    hash = "sha256-yYgmuMiwSGCt4lflal8/Vieu6RJPH/rs17+5QFMGO/Y=";
  };
}

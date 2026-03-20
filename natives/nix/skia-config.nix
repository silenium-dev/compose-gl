{ pkgs
}:
let
  skia = { os, arch, hash }: rec {
    version = "m144-e2e6623374-4";
    buildType = "Release";
    source_url = "https://nexus.silenium.dev/repository/github-releases/JetBrains/skia-pack/releases/download/${version}/Skia-${version}-${os}-${buildType}-${arch}.zip";
    source_hash = hash;
    source_filename = "skia.zip";
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "x86_64-windows" = skia {
    os = "windows";
    arch = "x64";
    hash = "sha256-UIabCYpPcvWENocJOhqP3ZTl1h2YOXAku05SEif+3Lo=";
  };
  "aarch64-windows" = skia {
    os = "windows";
    arch = "arm64";
    hash = "sha256-Y64TeottjDfj1ZkVt0rasVopng/A7MBVk+nPbYDv+DY=";
  };
  "x86_64-linux" = skia {
    os = "linux";
    arch = "x64";
    hash = "sha256-iVYo7K+PYN7ltMOwRXSikDNUCASREKAI15rUZoOCp5s=";
  };
  "aarch64-linux" = skia {
    os = "linux";
    arch = "arm64";
    hash = "sha256-LGbcRebt5vqHaC1hejkAWYlFktPQzS1/cq/e2DD7Op4=";
  };
}

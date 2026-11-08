{ pkgs
}:
let
  versionsJson = pkgs.lib.importJSON ./versions.json;
  skia = { target, hash, version }: rec {
    inherit version;
    buildType = target.buildType;
    os = target.os;
    arch = target.arch;
    source_url = "https://nexus.silenium.dev/repository/github-releases/JetBrains/skia/releases/download/${version}/Skia-${version}-${os}-${buildType}-${arch}.zip";
    source_hash = hash;
    source_filename = "skia.zip";
    directory = pkgs.lib.strings.removeSuffix ".zip" source_filename;
  };
in
(pkgs.lib.mapAttrs (platform: value: skia (value // { version = versionsJson.version; })) versionsJson.platforms)

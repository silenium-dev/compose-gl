{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=fe416aaedd397cacb33a610b33d60ff2b431b127";
    jni-utils.url = "github:silenium-dev/jni-utils";
  };

  outputs = { nixpkgs, jni-utils, ... }:
    let
      pkgs = nixpkgs.legacyPackages."x86_64-linux";
      fs = nixpkgs.lib.fileset;
    in
    {
      packages."x86_64-linux" = jni-utils.lib.buildJNILib {
        name = "compose-gl";
        version = "0.1.0";
        mesonTarget = "compose-gl";
        buildType = "release";
        libName = "compose-gl";
        libDir = "src";
        targetSystems = [
          "x86_64-linux"
          "aarch64-linux"
          "x86_64-windows"
          "aarch64-windows"
        ];

        additionalNativeInputs = targetSystem: pkgs: [ pkgs.p7zip ];
        additionalInputs = targetSystem: pkgs:
          if jni-utils.lib.isLinux targetSystem
          then [ pkgs.libxcb pkgs.libxau pkgs.libxdmcp ]
          else [ ];
        sources = targetSystem:
          let
            skia = import ./nix/skia-config.nix { inherit pkgs; };
            sourceFiles = fs.unions [
              ./src
              ./meson.build
              ./subprojects.tpl
            ];
          in
          [
            (builtins.path {
              path = fs.toSource {
                root = ./.;
                fileset = sourceFiles;
              };
              name = "compose-gl";
            })
            (pkgs.fetchurl {
              name = "skia.zip";
              url = skia."${targetSystem}".source_url;
              hash = skia."${targetSystem}".source_hash;
            })
          ];

        unpack = targetSystem: ''
          runHook preUnpack

          IFS=' ' read -r -a sourceArray <<< "$srcs"
          cp -r "''${sourceArray[0]}" "compose-gl"
          chmod -R +w compose-gl
          mkdir -p compose-gl/subprojects/skia
          7z x -y -ocompose-gl/subprojects/skia "''${sourceArray[1]}"
          cp -r compose-gl/subprojects.tpl/skia/* compose-gl/subprojects/skia/

          runHook postUnpack
        '';
        postUnpackPhase = targetSystem: ''
          shopt -s globstar

          sourceRoot="$(pwd)/compose-gl"
          echo "changing source root: $sourceRoot"
          cd "$sourceRoot"
        '';
      };
    };
}

{
  description = "DMX platform dev shell with plugin-build/watch (fedwiki | zettelkasten | dita)";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";

  outputs = { self, nixpkgs }:
  let
    forAllSystems = nixpkgs.lib.genAttrs [
      "x86_64-darwin" "aarch64-darwin" "x86_64-linux" "aarch64-linux"
    ];
  in
  {
    devShells = forAllSystems (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # optional 'bnd' CLI — not present in all 24.05 channels
        maybeBnd = pkgs.bnd or null;
        withBnd  = pkgs.lib.optionals (maybeBnd != null) [ maybeBnd ];

        jdkFor = ver:
          if ver == "8"  then pkgs.jdk8
          else if ver == "11" then pkgs.jdk11
          else if ver == "17" then pkgs.jdk17
          else pkgs.jdk8;

        plugin-build = pkgs.writeShellScriptBin "plugin-build" ''
          set -euo pipefail

          usage() {
            cat <<USAGE
Usage:
  plugin-build [fedwiki|zettelkasten|dita] [--jdk 8|11|17]
  plugin-build                # run inside a plugin dir

Notes:
  - Defaults: plugin inferred from CWD, --jdk 8 (for dmx-dita), -DskipTests
  - Copies newest target/*.jar to <repo-root>/bundle-deploy/
USAGE
          }

          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          WANT_JDK="8"
          TARGET_PLUGIN=""

          while [ $# -gt 0 ]; do
            case "$1" in
              --help|-h) usage; exit 0;;
              --jdk) WANT_JDK="''${2:-8}"; shift 2;;
              fedwiki|zettelkasten|dita) TARGET_PLUGIN="$1"; shift;;
              *) echo "Unknown arg: $1"; usage; exit 2;;
            esac
          done

          if [ -z "''${TARGET_PLUGIN:-}" ]; then
            base="$(basename "$PWD")"
            case "$base" in
              dmx-fedwiki|fedwiki) TARGET_PLUGIN="fedwiki";;
              dmx-zettelkasten|zettelkasten) TARGET_PLUGIN="zettelkasten";;
              dmx-dita|dita) TARGET_PLUGIN="dita";;
            esac
          fi

          if [ -z "''${TARGET_PLUGIN:-}" ]; then
            echo "Error: Can't infer plugin. Specify one of: fedwiki zettelkasten dita"; usage; exit 2;
          fi

          PLUGDIR="$ROOT/modules-external/dmx-$TARGET_PLUGIN"
          if [ ! -d "$PLUGDIR" ]; then
            if [ -f "pom.xml" ]; then PLUGDIR="$PWD"; else
              echo "Error: $PLUGDIR not found and no pom.xml in CWD"; exit 1;
            fi
          fi

          echo "==> Building plugin: $TARGET_PLUGIN (JDK $WANT_JDK)"

          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            if MAVEN_JAVA_HOME="$(/usr/libexec/java_home -v "$WANT_JDK" 2>/dev/null)"; then
              export JAVA_HOME="$MAVEN_JAVA_HOME"
              export PATH="$JAVA_HOME/bin:$PATH"
            fi
          fi

          ( cd "$PLUGDIR"
            echo "Running: mvn -q -DskipTests package"
            mvn -q -DskipTests package
          )

          NEWEST_JAR="$(ls -1t "$PLUGDIR"/target/*.jar 2>/dev/null | head -n1 || true)"
          if [ -z "$NEWEST_JAR" ]; then
            echo "Error: No jar found under $PLUGDIR/target"; exit 1;
          fi

          DEPLOY="$ROOT/bundle-deploy"
          mkdir -p "$DEPLOY"
          cp -f "$NEWEST_JAR" "$DEPLOY/"
          echo "✅ Deployed: $DEPLOY/$(basename "$NEWEST_JAR")"

          if command -v bnd >/dev/null 2>&1; then
            echo "Tip: check OSGi imports/exports via:"
            echo "  bnd print -i \"$DEPLOY/$(basename "$NEWEST_JAR")\""
          else
            echo "(optional) 'bnd' CLI not found in this nix env."
          fi
        '';

        plugin-watch = pkgs.writeShellScriptBin "plugin-watch" ''
          set -euo pipefail

          usage() {
            cat <<USAGE
Usage:
  plugin-watch [fedwiki|zettelkasten|dita] [--jdk 8|11|17]

Watches src/** and pom.xml, rebuilds with Maven, and copies newest jar to bundle-deploy/.
USAGE
          }

          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          TARGET_PLUGIN=""
          WANT_JDK="8"

          while [ $# -gt 0 ]; do
            case "$1" in
              --help|-h) usage; exit 0;;
              --jdk) WANT_JDK="''${2:-8}"; shift 2;;
              fedwiki|zettelkasten|dita) TARGET_PLUGIN="$1"; shift;;
              *) echo "Unknown arg: $1"; usage; exit 2;;
            esac
          done

          if [ -z "''${TARGET_PLUGIN:-}" ]; then
            base="$(basename "$PWD")"
            case "$base" in
              dmx-fedwiki|fedwiki) TARGET_PLUGIN="fedwiki";;
              dmx-zettelkasten|zettelkasten) TARGET_PLUGIN="zettelkasten";;
              dmx-dita|dita) TARGET_PLUGIN="dita";;
            esac
          fi
          [ -n "''${TARGET_PLUGIN:-}" ] || { echo "Error: specify plugin"; usage; exit 2; }

          PLUGDIR="$ROOT/modules-external/dmx-$TARGET_PLUGIN"
          if [ ! -d "$PLUGDIR" ]; then
            if [ -f "pom.xml" ]; then PLUGDIR="$PWD"; else
              echo "Error: $PLUGDIR not found and no pom.xml in CWD"; exit 1;
            fi
          fi

          echo "==> Watching $TARGET_PLUGIN (JDK $WANT_JDK)"

          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            if JAVA_HOME="$(/usr/libexec/java_home -v "$WANT_JDK" 2>/dev/null)"; then
              export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
            fi
          fi

          DEPLOY="$ROOT/bundle-deploy"
          mkdir -p "$DEPLOY"

          build_and_copy() {
            ( cd "$PLUGDIR"
              mvn -q -DskipTests package || return 1
              NEWEST_JAR="$(ls -1t target/*.jar | head -n1)"
              cp -f "$NEWEST_JAR" "$DEPLOY/"
              echo "🔁 Deployed: $DEPLOY/$(basename "$NEWEST_JAR")"
            )
          }

          build_and_copy || true
          if command -v watchexec >/dev/null 2>&1; then
            watchexec -w "$PLUGDIR/src" -w "$PLUGDIR/pom.xml" --shell=none -- \
              bash -lc 'build_and_copy'
          else
            echo "watchexec not found; polling every 3s."
            LAST=""
            while true; do
              NOW="$(find "$PLUGDIR/src" "$PLUGDIR/pom.xml" -type f -print0 2>/dev/null | xargs -0 stat -f '%m' 2>/dev/null || echo 0)"
              if [ "''${NOW:-0}" != "''${LAST:-1}" ]; then LAST="$NOW"; build_and_copy || true; fi
              sleep 3
            done
          fi
        '';
      in
      {
        default = pkgs.mkShell {
          packages =
            [
              (jdkFor "8") (jdkFor "11") (jdkFor "17")
              pkgs.maven pkgs.git pkgs.findutils pkgs.coreutils pkgs.gnused
              pkgs.watchexec
              plugin-build plugin-watch
            ] ++ withBnd;

          shellHook = ''
            echo "Commands available: plugin-build, plugin-watch"
            if ! command -v bnd >/dev/null 2>&1; then
              echo "(optional) 'bnd' CLI not found in this nixpkgs; skipping."
            fi
          '';
        };
      });
  };
}

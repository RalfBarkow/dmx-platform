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

        # ---------- Helpers: backend ----------
        dmx-run-backend = pkgs.writeShellScriptBin "dmx-run-backend" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          [ -f "$DMX_DIR/pom.xml" ] || { echo "dmx-run-backend: no pom.xml under $DMX_DIR"; exit 1; }
          WANT_JDK="''${1:-''${DMX_JAVA:-8}}"
          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            if JAVA_HOME="$(/usr/libexec/java_home -v "$WANT_JDK" 2>/dev/null)"; then
              export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
            fi
          fi
          echo "==> DMX backend (JDK $WANT_JDK) @ $DMX_DIR"
          ( cd "$DMX_DIR" && mvn pax:run )
        '';

        dmx-run-backend-port = pkgs.writeShellScriptBin "dmx-run-backend-port" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          PORT="''${1:-8081}"
          WANT_JDK="''${2:-''${DMX_JAVA:-8}}"
          [ -f "$DMX_DIR/pom.xml" ] || { echo "dmx-run-backend-port: no pom.xml under $DMX_DIR"; exit 1; }
          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            if JAVA_HOME="$(/usr/libexec/java_home -v "$WANT_JDK" 2>/dev/null)"; then
              export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
            fi
          fi
          echo "==> DMX backend (port $PORT, JDK $WANT_JDK) @ $DMX_DIR"
          ( cd "$DMX_DIR" && mvn -Dorg.osgi.service.http.port="$PORT" pax:run )
        '';

        dmx-run-backend-j11 = pkgs.writeShellScriptBin "dmx-run-backend-j11" ''
          exec dmx-run-backend 11
        '';

        # ---------- Helpers: build ----------
        dmx-build = pkgs.writeShellScriptBin "dmx-build" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          [ -f "$DMX_DIR/pom.xml" ] || { echo "dmx-build: no pom.xml under $DMX_DIR"; exit 1; }
          echo "==> Building DMX platform (-P all -DskipTests) @ $DMX_DIR"
          ( cd "$DMX_DIR" && mvn -T 1C clean install -P all -DskipTests )
        '';

        dmx-build-j11 = pkgs.writeShellScriptBin "dmx-build-j11" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          [ -f "$DMX_DIR/pom.xml" ] || { echo "dmx-build-j11: no pom.xml under $DMX_DIR"; exit 1; }
          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            if JAVA_HOME="$(/usr/libexec/java_home -v 11 2>/dev/null)"; then
              export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
            fi
          fi
          echo "==> Building DMX platform with JDK 11 (-P all -DskipTests) @ $DMX_DIR"
          ( cd "$DMX_DIR" && mvn -T 1C clean install -P all -DskipTests )
        '';

        # ---------- Helpers: frontend ----------
        dmx-run-frontend = pkgs.writeShellScriptBin "dmx-run-frontend" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          [ -f "$DMX_DIR/pom.xml" ] || { echo "dmx-run-frontend: run from dmx-platform or set DMX_DIR"; exit 1; }
          echo "==> Webpack Dev Server (npm run dev) @ $DMX_DIR"
          ( cd "$DMX_DIR" && NODE_OPTIONS="''${NODE_OPTIONS:---openssl-legacy-provider}" npm run dev )
        '';

        # ---------- Helpers: admin ----------
        dmx-free-8080 = pkgs.writeShellScriptBin "dmx-free-8080" ''
          set -euo pipefail
          PORT="''${1:-8080}"
          if ! command -v lsof >/dev/null 2>&1; then
            echo "lsof not found in PATH"; exit 1
          fi
          PIDS="$(lsof -t -iTCP:''${PORT} -sTCP:LISTEN || true)"
          [ -z "''${PIDS:-}" ] && { echo "No listener on $PORT"; exit 0; }
          echo "Terminating listeners on $PORT: $PIDS"
          for p in $PIDS; do kill -15 "$p" || true; done
          sleep 1
          PIDS2="$(lsof -t -iTCP:''${PORT} -sTCP:LISTEN || true)"
          if [ -n "''${PIDS2:-}" ]; then
            echo "Force killing: $PIDS2"
            for p in $PIDS2; do kill -9 "$p" || true; done
          fi
          echo "Port $PORT is free."
        '';

        dmx-reset-db = pkgs.writeShellScriptBin "dmx-reset-db" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          DB="$DMX_DIR/dmx-db"
          [ -d "$DB" ] || { echo "No DB dir at $DB (nothing to reset)"; exit 0; }
          TS="$(date +%Y%m%d-%H%M%S)"
          BK="$DMX_DIR/dmx-db.bak-$TS"
          echo "Backing up $DB -> $BK"
          mv "$DB" "$BK"
          echo "Reset done (empty DB will be recreated on next start)."
        '';

        dmx-plugin-dev-link = pkgs.writeShellScriptBin "dmx-plugin-dev-link" ''
          set -euo pipefail
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          DMX_DIR="''${DMX_DIR:-$ROOT}"
          PM="$DMX_DIR/modules/dmx-webclient/src/main/js/plugin-manager.js"
          [ -f "$PM" ] || { echo "plugin-manager.js not found at $PM"; exit 1; }
          add_line() {
            local needle="$1"
            local line="$2"
            if grep -Fq "$needle" "$PM"; then
              echo "Already linked: $needle"
            else
              ${pkgs.gnused}/bin/sed -i '/\/\/ while development add your plugins here/a '"$line" "$PM"
              echo "Linked: $needle"
            fi
          }
          # Link whichever plugins exist locally
          [ -d "$ROOT/modules-external/dmx-zettelkasten" ] && add_line "dmx-zettelkasten" \
            "initPlugin(require('modules-external/dmx-zettelkasten/src/main/js/plugin.js').default)"
          [ -d "$ROOT/modules-external/dmx-dita" ] && add_line "dmx-dita" \
            "initPlugin(require('modules-external/dmx-dita/src/main/js/plugin.js').default)"
          [ -d "$ROOT/modules-external/dmx-fedwiki" ] && add_line "dmx-fedwiki" \
            "initPlugin(require('modules-external/dmx-fedwiki/src/main/js/plugin.js').default)"
          echo "Done."
        '';

        # ---------- bnd helper ----------
        bnd-print = pkgs.writeShellScriptBin "bnd-print" ''
          set -euo pipefail
          [ $# -ge 1 ] || { echo "Usage: bnd-print <bundle.jar> [args…]"; exit 2; }
          JAR="$1"; shift || true
          [ -f "$JAR" ] || { echo "No such file: $JAR"; exit 1; }
          if command -v /usr/libexec/java_home >/dev/null 2>&1; then
            J17="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
            if [ -n "''${J17:-}" ]; then export JAVA_HOME="$J17"; export PATH="$JAVA_HOME/bin:$PATH"; fi
          fi
          if command -v bnd >/dev/null 2>&1; then
            exec bnd print -i "$JAR" "''$@"
          fi
          if command -v unzip >/dev/null 2>&1; then
            echo "bnd not found; showing META-INF/MANIFEST.MF"
            unzip -p "$JAR" META-INF/MANIFEST.MF || { echo "(no manifest)"; exit 1; }
            exit 0
          fi
          echo "Neither bnd nor unzip available."
          exit 1
        '';

        # ---------- plugin build/watch ----------
        plugin-build = pkgs.writeShellScriptBin "plugin-build" ''
          set -euo pipefail
          usage() {
            cat <<USAGE
Usage:
  plugin-build [fedwiki|zettelkasten|dita] [--jdk 8|11|17]
  plugin-build                # run inside a plugin dir
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
          [ -n "''${TARGET_PLUGIN:-}" ] || { echo "Error: specify plugin"; usage; exit 2; }
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
          [ -n "''${NEWEST_JAR:-}" ] || { echo "No jar found under $PLUGDIR/target"; exit 1; }
          DEPLOY="$ROOT/bundle-deploy"
          mkdir -p "$DEPLOY"
          cp -f "$NEWEST_JAR" "$DEPLOY/"
          echo "✅ Deployed: $DEPLOY/$(basename "$NEWEST_JAR")"
          if command -v bnd >/dev/null 2>&1; then
            echo "Tip: bnd-print \"$DEPLOY/$(basename "$NEWEST_JAR")\""
          fi
        '';

        plugin-watch = pkgs.writeShellScriptBin "plugin-watch" ''
          set -euo pipefail
          usage() {
            cat <<USAGE
Usage:
  plugin-watch [fedwiki|zettelkasten|dita] [--jdk 8|11|17]
USAGE
          }
          ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
          TARGET_PLUGIN=""; WANT_JDK="8"
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
          DEPLOY="$ROOT/bundle-deploy"; mkdir -p "$DEPLOY"
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
            LAST=""; while true; do
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
              pkgs.nodejs_20 pkgs.watchexec pkgs.unzip pkgs.lsof
              # helpers
              plugin-build plugin-watch
              dmx-run-backend dmx-run-backend-port dmx-run-backend-j11
              dmx-build dmx-build-j11 dmx-run-frontend
              dmx-free-8080 dmx-reset-db dmx-plugin-dev-link
              bnd-print
            ] ++ withBnd;

          shellHook = ''
            echo "Commands available: $(printf "%s " plugin-build plugin-watch dmx-run-backend dmx-run-backend-port dmx-run-backend-j11 dmx-build dmx-build-j11 dmx-run-frontend dmx-free-8080 dmx-reset-db dmx-plugin-dev-link bnd-print)"
            if ! command -v bnd >/dev/null 2>&1; then
              echo "(optional) 'bnd' CLI not found in this nixpkgs; use 'bnd-print' wrapper."
            fi
          '';
        };
      });
  };
}

{
  description = "Nix devShell for dmx-zettelkasten + DMX platform";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";

  outputs = { self, nixpkgs }:
  let
    forAllSystems = f:
      nixpkgs.lib.genAttrs
        [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ]
        (system: f (import nixpkgs { inherit system; }));
  in
  {
    devShells = forAllSystems (pkgs:
      let
        nodejs    = pkgs.nodejs_20;   # change to nodejs_20 if DMX supports it
        jdk8      = pkgs.jdk8;        # DMX template asks for Java 8
        jdk11     = pkgs.jdk11;
        # default JDK for shell (can stay 8)
        defaultJdk = jdk8;
        maven     = pkgs.maven;
        sed       = pkgs.gnused;      # ensure GNU sed (for -i)
        git       = pkgs.git;
        curl      = pkgs.curl;
        lsof      = pkgs.lsof;
        bash      = pkgs.bash;
        coreutils = pkgs.coreutils;

        writeCmd = name: text: pkgs.writeShellScriptBin name ''
          set -euo pipefail
          ${text}
        '';

        # Resolve DMX dir (default: sibling ../dmx-platform)
        dmxDirResolve = ''
          DMX_DIR="''${DMX_DIR:-$(cd "$(git rev-parse --show-toplevel)/.."; pwd)/dmx-platform}"
        '';

        cmd_clone = writeCmd "dmx-clone" ''
          ${dmxDirResolve}
          if [ -d "$DMX_DIR/.git" ]; then
            echo "dmx-platform already present at: $DMX_DIR"
            exit 0
          fi
          echo "Cloning dmx-platform into $DMX_DIR ..."
          git clone https://github.com/dmx-systems/dmx-platform "$DMX_DIR"
          echo "Done."
        '';

        cmd_build = writeCmd "dmx-build" ''
          ${dmxDirResolve}
          echo "Building DMX platform (all modules, skip tests) ..."
          cd "$DMX_DIR"
          mvn -T 1C clean install -P all -DskipTests
        '';

        cmd_link = writeCmd "dmx-plugin-dev-link" ''
          ${dmxDirResolve}
          PM="$DMX_DIR/modules/dmx-webclient/src/main/js/plugin-manager.js"
          LINE="initPlugin(require('modules-external/dmx-zettelkasten/src/main/js/plugin.js').default)"

          if grep -Fq "modules-external/dmx-zettelkasten" "$PM"; then
            echo "Plugin already linked in plugin-manager.js"
            exit 0
          fi

          if ! grep -Fq "// while development add your plugins here" "$PM"; then
            echo "Marker comment not found in $PM" >&2
            exit 1
          fi

          # Insert our initPlugin line right after the marker
          ${sed}/bin/sed -i '/\/\/ while development add your plugins here/a '"$LINE" "$PM"
          echo "Linked dmx-zettelkasten into Webclient (plugin-manager.js)."
        '';

        cmd_run_backend = writeCmd "dmx-run-backend" ''
          ${dmxDirResolve}
          cd "$DMX_DIR"
          echo "Starting DMX backend (mvn pax:run) ..."
          mvn pax:run
        '';

        # Free a TCP port (default 8080) using lsof (macOS/Linux)
        cmd_free_8080 = writeCmd "dmx-free-8080" ''
          PORT="''${1:-8080}"
          echo "Looking for listeners on TCP port $PORT ..."
          PIDS="$(${lsof}/bin/lsof -t -iTCP:''${PORT} -sTCP:LISTEN || true)"
          if [ -z "$PIDS" ]; then
            echo "No process is listening on $PORT."
            exit 0
          fi
          echo "Found PID(s): $PIDS"
          for p in $PIDS; do
            echo "Sending SIGTERM to $p"
            kill -15 "$p" || true
          done
          sleep 1
          PIDS2="$(${lsof}/bin/lsof -t -iTCP:''${PORT} -sTCP:LISTEN || true)"
          if [ -n "$PIDS2" ]; then
            echo "Still listening: $PIDS2 — sending SIGKILL"
            for p in $PIDS2; do kill -9 "$p" || true; done
          fi
          echo "Port $PORT is free."
        '';

        # Reset the dev DB (backs up dmx-db to dmx-db.bak-<timestamp>)
        cmd_reset_db = writeCmd "dmx-reset-db" ''
          ${dmxDirResolve}
          DB_DIR="$DMX_DIR/dmx-db"
          if [ ! -d "$DB_DIR" ]; then
            echo "No DB directory at $DB_DIR (nothing to reset)."
            exit 0
          fi
          TS="$(date +%Y%m%d-%H%M%S)"
          BK="$DMX_DIR/dmx-db.bak-$TS"
          echo "Backing up $DB_DIR -> $BK"
          mv "$DB_DIR" "$BK"
          echo "Reset done. DMX will recreate an empty DB on next start."
        '';

        cmd_run_frontend = writeCmd "dmx-run-frontend" ''
          ${dmxDirResolve}
          cd "$DMX_DIR"
          echo "Starting Webpack Dev Server with OpenSSL legacy provider (Node>=17 workaround) ..."
          NODE_OPTIONS="--openssl-legacy-provider" npm run dev
        '';

        # Start backend on a custom HTTP port (default 8081)
        cmd_run_backend_port = writeCmd "dmx-run-backend-port" ''
          ${dmxDirResolve}
          PORT="''${1:-8081}"
          cd "$DMX_DIR"
          echo "Starting DMX backend on port $PORT ..."
          mvn -Dorg.osgi.service.http.port="$PORT" pax:run
        '';

        cmd_plugin_build = writeCmd "plugin-build" ''
          usage() { echo "Usage: plugin-build [fedwiki|zettelkasten] [--jdk 8|11] (or run inside that plugin dir)"; exit 1; }

          # find DMX platform root (works when called from inside modules-external/* too)
          find_dmx_root() {
            local d="$PWD"
            while [ "$d" != "/" ]; do
              if [ -d "$d/bundle-deploy" ] && [ -d "$d/modules" ]; then echo "$d"; return 0; fi
              if [ -f "$d/pom.xml" ] && grep -q '<artifactId>dmx</artifactId>' "$d/pom.xml"; then echo "$d"; return 0; fi
              d="$(dirname "$d")"
            done
            # fallback to sibling pattern
            echo "$(cd "$(git rev-parse --show-toplevel)/.."; pwd)/dmx-platform"
          }

          TARGET=""
          JAVA_VER="''${DMX_JAVA:-11}"

          # robust arg parsing: --jdk=8, --jdk 8, 8, 11, jdk8, jdk11
          while [ $# -gt 0 ]; do
            case "$1" in
              fedwiki|zettelkasten) TARGET="$1"; shift ;;
              --jdk=*)              JAVA_VER="''${1#--jdk=}"; shift ;;
              --jdk)                shift; [ $# -gt 0 ] || { echo "ERROR: --jdk needs a value (8 or 11)"; exit 1; }; JAVA_VER="$1"; shift ;;
              8|11)                 JAVA_VER="$1"; shift ;;
              jdk8)                 JAVA_VER=8; shift ;;
              jdk11)                JAVA_VER=11; shift ;;
              -h|--help)            usage ;;
              *) echo "Unknown arg: $1"; usage ;;
            esac
          done

          # infer target when run inside a plugin dir
          if [ -z "''${TARGET}" ]; then
            if [ -f pom.xml ] && grep -q '<artifactId>dmx-fedwiki</artifactId>' pom.xml; then
              TARGET="fedwiki"
            elif [ -f pom.xml ] && grep -q '<artifactId>dmx-zettelkasten</artifactId>' pom.xml; then
              TARGET="zettelkasten"
            else
              usage
            fi
          fi

          REPO_ROOT="$(git rev-parse --show-toplevel)"

          case "''${TARGET}" in
            fedwiki)
              if [ -f pom.xml ] && grep -q '<artifactId>dmx-fedwiki</artifactId>' pom.xml; then
                PLUG_DIR="$PWD"
              else
                PLUG_DIR="''${REPO_ROOT}/modules-external/dmx-fedwiki"
              fi
              ;;
            zettelkasten)
              if [ -f pom.xml ] && grep -q '<artifactId>dmx-zettelkasten</artifactId>' pom.xml; then
                PLUG_DIR="$PWD"
              else
                PLUG_DIR="''${REPO_ROOT}/modules-external/dmx-zettelkasten"
              fi
              ;;
            *) usage ;;
          esac

          cd "''${PLUG_DIR}"

          # choose JDK
          if [ "''${JAVA_VER}" = "8" ]; then
            export JAVA_HOME="${jdk8}"
            export PATH="''${JAVA_HOME}/bin:''${PATH}"
            MVN_PROFILE="-P java8"
            echo ">> Using JDK 8 for dmx-''${TARGET}"
          elif [ "''${JAVA_VER}" = "11" ]; then
            export JAVA_HOME="${jdk11}"
            export PATH="''${JAVA_HOME}/bin:''${PATH}"
            MVN_PROFILE="-P java11"
            echo ">> Using JDK 11 for dmx-''${TARGET}"
          else
            echo "ERROR: Invalid JDK version ''${JAVA_VER}'. Use 8 or 11."
            exit 1
          fi

          echo "Building dmx-''${TARGET} (mvn -DskipTests package ''${MVN_PROFILE}) ..."
          mvn -DskipTests package ''${MVN_PROFILE}

          AID="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.artifactId)"
          VER="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
          JAR="target/''${AID}-''${VER}.jar"
          [ -f "''${JAR}" ] || { echo "ERROR: built jar not found at ''${JAR}" >&2; exit 2; }

          DMX_DIR="''${DMX_DIR:-$(find_dmx_root)}"
          DEST="''${DMX_DIR}/bundle-deploy"
          mkdir -p "''${DEST}"

          echo "Deploying ''${JAR} -> ''${DEST} ..."
          cp -v "''${JAR}" "''${DEST}/"
          echo "Done. If DMX is running, FileInstall should hot-reload the bundle."
        '';

        cmd_plugin_watch = writeCmd "plugin-watch" ''
          PLUGIN="''${1:-}"
          shift || true

          # Auto-detect plugin if inside plugin directory
          if [ -z "''${PLUGIN}" ]; then
            case "''${PWD}" in
              */modules-external/dmx-fedwiki*)       PLUGIN="fedwiki" ;;
              */modules-external/dmx-zettelkasten*)  PLUGIN="zettelkasten" ;;
              *)
                echo "Usage: plugin-watch [fedwiki|zettelkasten] [--jdk 8|11]"
                echo "Or run from inside the plugin directory"
                exit 1
              ;;
            esac
          fi

          # Determine plugin directory
          REPO_ROOT="$(git rev-parse --show-toplevel)"
          case "''${PLUGIN}" in
            fedwiki)      PLUG_DIR="''${REPO_ROOT}/modules-external/dmx-fedwiki" ;;
            zettelkasten) PLUG_DIR="''${REPO_ROOT}/modules-external/dmx-zettelkasten" ;;
            *) echo "Unknown plugin: ''${PLUGIN}"; exit 1 ;;
          esac

          echo "Watching ''${PLUGIN} sources in ''${PLUG_DIR} -> rebuild & hot-deploy on changes ..."

          # Change to plugin directory and run watchexec there
          cd "''${PLUG_DIR}"
          ${pkgs.watchexec}/bin/watchexec \
            -w src/main/java -w src/main/resources -w src/main/js \
            --shell=none --restart --clear \
            -- plugin-build "''${PLUGIN}" "$@"
        '';

        cmd_run_backend_j11 = writeCmd "dmx-run-backend-j11" ''
          ${dmxDirResolve}
          cd "$DMX_DIR"
          echo "Starting DMX backend on JDK 11 ..."
          export JAVA_HOME="${jdk11}"
          export PATH="${jdk11}/bin:$PATH"
          mvn pax:run
        '';

        cmd_build_j11 = writeCmd "dmx-build-j11" ''
          ${dmxDirResolve}
          cd "$DMX_DIR"
          echo "Building DMX platform with JDK 11 (still targeting whatever the POM sets) ..."
          export JAVA_HOME="${jdk11}"
          export PATH="${jdk11}/bin:$PATH"
          mvn -T 1C clean install -P all -DskipTests
        '';

      in
      {
        default = pkgs.mkShell {
          packages = [
            defaultJdk maven nodejs git curl sed lsof coreutils bash
          ];

          # Many JDK packages export JAVA_HOME automatically; keep it explicit:
          shellHook = ''
            export JAVA_HOME="${defaultJdk}"
            export PATH="$PATH:${defaultJdk}/bin"
            # Webpack + Node>=17 (OpenSSL 3) workaround for dev & build
            export NODE_OPTIONS="--openssl-legacy-provider"
            echo
            echo "dmx-zettelkasten devshell ready."
            echo "Helper commands:"
            echo "  dmx-clone                 - clone dmx-platform next to this repo (or use \$DMX_DIR)"
            echo "  dmx-build                 - build dmx-platform (-P all, skip tests)"
            echo "  dmx-plugin-dev-link       - link this plugin into Webclient for HMR"
            echo "  dmx-run-backend           - start DMX backend (pax:run)"
            echo "  dmx-run-backend-port [P]  - backend on a custom port (default 8081)"
            echo "  dmx-run-frontend          - start Webpack Dev Server (npm run dev)"
            echo "  plugin-build              - build this plugin jar (deploys to bundle-deploy)"
            echo "  plugin-watch              - watch src/* and auto-build + hot-deploy"
            echo "  dmx-run-backend-j11       - run backend on Java 11"
            echo "  dmx-build-j11             - build platform using Java 11 toolchain"
            echo "  dmx-free-8080 [PORT]      - free a TCP port (SIGTERM then SIGKILL)"
            echo "  dmx-reset-db              - backup & reset dmx-db"
            echo
            echo "Tip: export DMX_DIR=/path/to/dmx-platform if it's not ../dmx-platform"
            echo
          '';

          buildInputs = [ ];

          # Install helper commands into the environment
          nativeBuildInputs = [
            cmd_clone
            cmd_build
            cmd_link
            cmd_run_backend
            cmd_run_backend_port
            cmd_run_frontend
            cmd_plugin_build
            cmd_plugin_watch
            cmd_free_8080
            cmd_reset_db
            cmd_run_backend_j11
            cmd_build_j11
          ];
        };
      });
  };
}

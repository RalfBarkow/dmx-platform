{
  description = "Nix devShell for dmx-zettelkasten + DMX platform";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";

  outputs = { self, nixpkgs }:
  let
    forAllSystems = f:
      nixpkgs.lib.genAttrs [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ] (system:
        f (import nixpkgs { inherit system; })
      );
  in
  {
    devShells = forAllSystems (pkgs:
      let
        nodejs = pkgs.nodejs_18;   # Change to nodejs_20 if DMX supports it
        jdk    = pkgs.jdk8;        # DMX template asks for Java 8
        maven  = pkgs.maven;
        sed    = pkgs.gnused;      # ensure GNU sed (for -i)
        git    = pkgs.git;
        curl   = pkgs.curl;
        bash   = pkgs.bash;
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

        cmd_run_frontend = writeCmd "dmx-run-frontend" ''
          ${dmxDirResolve}
          cd "$DMX_DIR"
          echo "Starting Webpack Dev Server with OpenSSL legacy provider (Node>=17 workaround) ..."
          NODE_OPTIONS="--openssl-legacy-provider" npm run dev
        '';

        cmd_plugin_build = writeCmd "plugin-build" ''
          REPO_ROOT="$(git rev-parse --show-toplevel)"
          cd "$REPO_ROOT"
          echo "Building dmx-zettelkasten plugin (mvn clean package) ..."
          mvn clean package
        '';
      in
      {
        default = pkgs.mkShell {
          packages = [
            jdk maven nodejs git curl sed coreutils bash
          ];

          # Many JDK packages export JAVA_HOME automatically; keep it explicit:
          shellHook = ''
            export JAVA_HOME="${jdk}"
            export PATH="$PATH:${jdk}/bin"
            echo
            echo "dmx-zettelkasten devshell ready."
            echo "Helper commands:"
            echo "  dmx-clone           - clone dmx-platform next to this repo (or use \$DMX_DIR)"
            echo "  dmx-build           - build dmx-platform (skip tests)"
            echo "  dmx-plugin-dev-link - link this plugin into Webclient for HMR"
            echo "  dmx-run-backend     - start DMX backend (pax:run)"
            echo "  dmx-run-frontend    - start Webpack Dev Server (npm run dev)"
            echo "  plugin-build        - build this plugin jar (deploys to bundle-deploy)"
            echo
            echo "Tip: export DMX_DIR=/path/to/dmx-platform if it's not ../dmx-platform"
            echop
          '';

          buildInputs = [ ];

          # Install helper commands into the environment
          nativeBuildInputs = [
            cmd_clone
            cmd_build
            cmd_link
            cmd_run_backend
            cmd_run_frontend
            cmd_plugin_build
          ];
        };
      });
  };
}

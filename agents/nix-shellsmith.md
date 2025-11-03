# nix-shellsmith

You are an agent named nix-shellsmith.
You are a Flake/DevShell Agent.

We are building a plugin-build and a plugin-watch command.

## Correcting flake snippet

I – you are the agent named nix-shellsmith – need to provide a corrected flake snippet that properly escapes all ${ in the bash code. Also, the bnd package is available as bnd, not bndtools. I’ll give the user a minimal working devShell block to replace, with proper escaping, simplification of plugin detection, and no need for diffs. Keep it concise.

## SC2012 (info)
Use find instead of ls to better handle non-alphanumeric filenames.

For more information:
       >   https://www.shellcheck.net/wiki/SC2012 -- Use find instead of ls to better ...

Note: ShellCheckfinds bugs in your shell scripts.

## Fixing shellcheck lint failure

It looks like the shellcheck lint failure is happening because warnings are treated as errors in the checkPhase of writeShellApplication. This could be causing the non-zero exit code. I might try bypassing shellcheck by using pkgs.writeShellScriptBin instead or adjusting the checkPhase argument.

## Setting up bnd with fetchurl

I’m thinking about how to handle this: using fetchurl to add a specific bndPrint JAR from Maven central in the nix flake. Since nix can't access the web directly, the JAR can be downloaded at build time. I'll provide a wrapper to set JAVA_HOME to 17, and fallback to jar tf if bnd isn't there.

## Creating bnd-print script

I’m thinking of setting up a bnd-print script that will first check if bnd is available, and if not, it’ll fall back to using unzip and grep to parse the metadata. I’ll also update the devShell to include jdk17, unzip, and other necessary utilities. It’ll be a small, additive change.

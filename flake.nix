{
  description = "Description for the project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parsn.url = "github:hercules-ci/flake-parts";
    boulder.url = "github:berkeleytrue/nix-boulder-banner";
  };

  outputs = inputs @ {flake-parts, ...}:
    flake-parts.lib.mkFlake {inherit inputs;} {
      imports = [
        inputs.boulder.flakeModule
      ];

      systems = ["x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin"];
      perSystem = {
        pkgs,
        config,
        ...
      }: let
        dev = pkgs.writeShellScriptBin "dev" ''
          echo "Starting dev repl"
          ${pkgs.babashka}/bin/bb dev
        '';
      in {
        formatter.default = pkgs.alejandra;
        boulder.commands = [
          {
            exec = dev;
            description = "Start a dev repl";
          }
        ];

        devShells.default = pkgs.mkShell {
          name = "conduit-clj";
          inputsFrom = [
            config.boulder.devShell
          ];

          buildInputs = with pkgs; [
            babashka
            clojure
            clojure-lsp
            neil
          ];
        };
      };
      flake = {
      };
    };
}

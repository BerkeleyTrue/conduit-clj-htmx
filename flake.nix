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
      }: {
        formatter.default = pkgs.alejandra;
        boulder.commands = [
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
          ];
        };
      };
      flake = {
      };
    };
}

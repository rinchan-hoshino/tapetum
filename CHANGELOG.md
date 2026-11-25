# Changelog

## [1.1.0] - 2026-11-25

### Added
- Full 10-target multi-version matrix support:
  - Minecraft `1.20.1` (Fabric & Forge, compatible with Sodium 0.5.x / Embeddium 0.3.x and Iris 1.7.x / Oculus 1.8.x)
  - Minecraft `1.21.1` (Fabric & NeoForge)
  - Minecraft `26.1.2`, `26.2`, and `26.3` (Fabric & NeoForge, compatible with Sodium 0.9.x, Iris 1.11.x, and RenderPearl `VertexFormat` on `26.3`)
- Native Forge `1.20.1` subproject with `pack.mcmeta` (`pack_format: 15`) and `ConfigScreenHandler` integration.
- Self-contained cleanroom compileOnly stubs (`compatStub_*`) removing all external local classpath dependencies.
- Redesigned 512x512 pixel-art Tapetum badge icon.

### Changed
- Added `DEVELOPMENT.md` for build instructions and streamlined `README.md` for store listings.

## [1.0.0] - 2026-11-24

### Added
- Initial release for Minecraft 1.21.1 (NeoForge & Fabric).
- Iris 54-byte direct vertex writer for shader-active entity rendering.
- Pre-baked mesh cache with live `Pre-baked Meshes` counter.
- Adapter support for GeckoLib, TaCZ, Touhou Little Maid, and SlashBlade Resharped.
- `TapetumConfigScreen` for runtime status inspection and configuration.

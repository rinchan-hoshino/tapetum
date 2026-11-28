# Tapetum Feature Map

| Feature | Description | Module |
| --- | --- | --- |
| Iris/Oculus 54B Direct Writer | Direct 54-byte off-heap vertex writer for Iris/Oculus shader entity pipelines across 1.20.1, 1.21.1, 26.1.2, 26.2, and 26.3 | `common` (`v120`, `v121`, `v26_1`, `v26_2`, `v26_3`) |
| Mesh Pre-baking Cache | Pre-bakes vanilla `ModelPart.Cube` (via `BakedQuadMesh.plainBuilder()`) and high-poly entity/block-entity meshes with per-quad mid-UV, tangent math, and cross-product face-normal recovery fallback | `common` |
| Fast-path Enchanted Glint | Always-on `FormatAwareWriter` bridge (`VertexMultiConsumerFormatMixin`) plus conflict-free `VertexBufferWriter` (`VertexMultiConsumerFastMixin`, gated when Sodium ships its own `DoubleMixin`) on 1.20.1, 1.21.1, and 26.1.2 | `common` |
| Smart Shadow Culling | Skips redundant decorative entity and detail-layer shadow passes during Iris/Oculus shadow rendering | `common` |
| Mod Adapters (1.21.1) | Built-in optional adapters for GeckoLib 4, AzureLib, TaCZ, Touhou Little Maid, SimpleBedrockModel, Kaleidoscope Doll, and SlashBlade Resharped | `common` (`v121`), `neoforge` (`v121`) |
| Config Screen | In-game `TapetumConfigScreen` (`GuiGraphics` on 1.20.1/1.21.1, `GuiGraphicsExtractor` on 26.x) with live diagnostics | `common` |
| Multi-Loader Entrypoints | Fabric, Forge (1.20.1 with `pack_format=15`), and NeoForge (1.21.1 & 26.x) | `fabric`, `forge`, `neoforge` |

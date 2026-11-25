# Development

## Build Matrix

Build and audit all 10 targets defined in `profiles.json` (`1.20.1` Fabric/Forge, `1.21.1` Fabric/NeoForge, `26.1.2` Fabric/NeoForge, `26.2` Fabric/NeoForge, `26.3` Fabric/NeoForge):

```bash
python3 scripts/build-matrix.py
```

Build a single Minecraft version profile:

```bash
python3 scripts/build-matrix.py --profile 1.21.1
```

## Unit Tests

Run unit tests and bytecode contract checks on NeoForge:

```bash
./gradlew :neoforge:test -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache
```

## Regenerating the Mod Icon

```bash
python3 scripts/draw_icon.py
```

## License

`GPL-3.0-or-later` — see [LICENSE](LICENSE).

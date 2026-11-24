#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/3] Running JUnit tests and building NeoForge + Fabric JARs..."
./gradlew :neoforge:test :neoforge:build :fabric:build --console=plain

mkdir -p build/releases
NEO_JAR="build/profiles/1.21.1/neoforge/libs/tapetum-neoforge-1.0.0+1.21.1.jar"
FAB_JAR="build/profiles/1.21.1/fabric/libs/tapetum-fabric-1.0.0+1.21.1.jar"

cp -f "$NEO_JAR" "build/releases/tapetum-1.0.0+1.21.1-neoforge.jar"
cp -f "$FAB_JAR" "build/releases/tapetum-1.0.0+1.21.1-fabric.jar"

echo "[2/3] Verifying release JAR contents..."
python3 - <<'PY'
import zipfile, sys

common_required = [
    "LICENSE",
    "tapetum.mixins.json",
    "assets/tapetum/icon.png",
    "assets/tapetum/lang/en_us.json",
    "assets/tapetum/lang/zh_cn.json",
    "assets/tapetum/lang/zh_tw.json",
    "assets/tapetum/lang/ja_jp.json",
]

checks = {
    "build/releases/tapetum-1.0.0+1.21.1-neoforge.jar": common_required + [
        "META-INF/neoforge.mods.toml",
        "META-INF/services/com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster",
        "dev/rinchan/tapetum/neoforge/TapetumNeoForge.class",
        "dev/rinchan/tapetum/mixin/tacz/TaCZIrisCompatCacheMixin.class",
    ],
    "build/releases/tapetum-1.0.0+1.21.1-fabric.jar": common_required + [
        "fabric.mod.json",
        "tapetum.fabric.mixins.json",
        "dev/rinchan/tapetum/fabric/TapetumFabric.class",
        "dev/rinchan/tapetum/fabric/TapetumFabricClient.class",
        "dev/rinchan/tapetum/fabric/TapetumModMenu.class",
    ],
}

for jar_path, required in checks.items():
    with zipfile.ZipFile(jar_path, "r") as zf:
        names = set(zf.namelist())
        missing = [r for r in required if r not in names]
        if missing:
            print(f"ERROR: {jar_path} missing: {missing}", file=sys.stderr)
            sys.exit(1)
        if "fabric" in jar_path and "dev/rinchan/tapetum/mixin/tacz/TaCZIrisCompatCacheMixin.class" in names:
            print(f"ERROR: {jar_path} should not contain NeoForge-only TaCZIrisCompatCacheMixin", file=sys.stderr)
            sys.exit(1)
        print(f"OK: {jar_path} ({len(names)} entries)")
PY

echo "[3/3] Verification complete:"
ls -lh build/releases/

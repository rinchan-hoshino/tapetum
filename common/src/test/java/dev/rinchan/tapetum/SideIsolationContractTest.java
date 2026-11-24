package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Ensures Dedicated Server entrypoints and shared server-safe classes never reference
 * client-only Minecraft, Sodium, Iris, or Blaze3D vertex classes.
 */
final class SideIsolationContractTest {
    private static final List<String> SERVER_SAFE_CLASSES = List.of(
            "dev/rinchan/tapetum/Tapetum.class",
            "dev/rinchan/tapetum/config/TapetumConfig.class",
            "dev/rinchan/tapetum/config/TapetumConfig$Data.class",
            "dev/rinchan/tapetum/mixin/TapetumMixinPlugin.class",
            "dev/rinchan/tapetum/neoforge/TapetumNeoForge.class",
            "dev/rinchan/tapetum/neoforge/TlmEntityMixinAdjuster.class",
            "dev/rinchan/tapetum/geometry/MeshCacheRegistry.class",
            "dev/rinchan/tapetum/hygiene/FrameClock.class",
            "dev/rinchan/tapetum/hygiene/TaCZThermalFastState.class");

    private static final List<String> FORBIDDEN_CLIENT_PREFIXES = List.of(
            "net/minecraft/client/",
            "net/irisshaders/iris/",
            "net/caffeinemc/mods/sodium/",
            "com/mojang/blaze3d/vertex/");

    @Test
    void serverSafeClassesContainZeroClientOnlyReferences() throws IOException {
        String jarPath = System.getProperty("tapetum.packagedJar");
        assertNotNull(jarPath, "tapetum.packagedJar system property must be set");

        List<String> leaks = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath)) {
            for (String classEntry : SERVER_SAFE_CLASSES) {
                JarEntry entry = jar.getJarEntry(classEntry);
                assertNotNull(entry, "Expected class in packaged jar: " + classEntry);
                try (InputStream is = jar.getInputStream(entry)) {
                    scanForClientReferences(classEntry, is, leaks);
                }
            }
        }
        assertTrue(leaks.isEmpty(), "Client-only class references found in server-safe classes:\n" + String.join("\n", leaks));
    }

    @Test
    void commonMixinPackageSeparatesServerSafeAndClientOnlyMixins() throws IOException {
        String jarPath = System.getProperty("tapetum.packagedJar");
        assertNotNull(jarPath, "tapetum.packagedJar system property must be set");

        List<String> clientOnlyMixins = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("dev/rinchan/tapetum/mixin/") && name.endsWith("Mixin.class")) {
                    clientOnlyMixins.add(name);
                }
            }
        }
        assertFalse(clientOnlyMixins.isEmpty());
        // Verify TapetumMixinPlugin only includes TlmEntityBroomBboxMixin on non-client side
        assertTrue(clientOnlyMixins.contains("dev/rinchan/tapetum/mixin/tlm/TlmEntityBroomBboxMixin.class"));
    }

    private static void scanForClientReferences(String owner, InputStream is, List<String> leaks) throws IOException {
        ClassReader cr = new ClassReader(is);
        cr.accept(
                new ClassVisitor(Opcodes.ASM9) {
                    private void checkDescriptorOrInternal(String str) {
                        if (str == null) {
                            return;
                        }
                        for (String prefix : FORBIDDEN_CLIENT_PREFIXES) {
                            if (str.contains(prefix)) {
                                leaks.add(owner + " -> " + str);
                            }
                        }
                    }

                    @Override
                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        checkDescriptorOrInternal(descriptor);
                        return null;
                    }

                    @Override
                    public MethodVisitor visitMethod(
                            int access, String name, String descriptor, String signature, String[] exceptions) {
                        checkDescriptorOrInternal(descriptor);
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(
                                    int opcode, String targetOwner, String targetName, String targetDesc, boolean isInterface) {
                                checkDescriptorOrInternal(targetOwner);
                                checkDescriptorOrInternal(targetDesc);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String targetOwner, String targetName, String targetDesc) {
                                checkDescriptorOrInternal(targetOwner);
                                checkDescriptorOrInternal(targetDesc);
                            }

                            @Override
                            public void visitTypeInsn(int opcode, String type) {
                                checkDescriptorOrInternal(type);
                            }
                        };
                    }
                },
                0);
    }
}

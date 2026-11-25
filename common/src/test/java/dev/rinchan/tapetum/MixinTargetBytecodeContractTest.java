package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Verifies every compiled @Mixin targeting Minecraft / Blaze3D classes in the packaged Tapetum JAR
 * against real runtime bytecode, and ensures zero compileOnly stub classes leak into packaged JARs.
 */
final class MixinTargetBytecodeContractTest {
    private static final List<String> INJECTOR_ANNOTATIONS = List.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/Overwrite;");

    private static final List<String> FORBIDDEN_STUB_PREFIXES = List.of(
            "net/caffeinemc/mods/sodium/",
            "me/jellysquid/mods/sodium/",
            "net/irisshaders/iris/",
            "com/terraformersmc/modmenu/",
            "software/bernie/geckolib/",
            "mod/azure/azurelib/",
            "com/tacz/",
            "com/github/tartaricacid/",
            "com/github/ysbbbbbb/",
            "mods/flammpfeil/");

    @Test
    void minecraftMixinTargetsExistAndNoStubsLeakIntoJar() throws IOException {
        String jarPath = System.getProperty("tapetum.packagedJar");
        assertNotNull(jarPath, "tapetum.packagedJar system property must be set");

        Map<String, ClassNode> cache = new HashMap<>();
        List<String> failures = new ArrayList<>();
        int checkedMixins = 0;

        try (JarFile packaged = new JarFile(jarPath)) {
            var entries = packaged.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                for (String forbidden : FORBIDDEN_STUB_PREFIXES) {
                    if (name.startsWith(forbidden)) {
                        failures.add("Leaked compileOnly stub class in packaged JAR: " + name);
                    }
                }
                if (!name.endsWith(".class")) {
                    continue;
                }
                ClassNode mixinNode = readClass(packaged.getInputStream(entry));
                AnnotationNode mixinAnn =
                        findAnnotation(mixinNode.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
                if (mixinAnn == null) {
                    continue;
                }
                checkedMixins++;
                List<String> targetInternalNames = extractMixinTargets(mixinAnn);
                assertFalse(targetInternalNames.isEmpty(), "Empty @Mixin targets on " + mixinNode.name);

                for (String targetInternal : targetInternalNames) {
                    if (!targetInternal.startsWith("net/minecraft/") && !targetInternal.startsWith("com/mojang/")) {
                        continue;
                    }
                    ClassNode targetNode = loadClasspathClass(targetInternal, cache);
                    if (targetNode == null) {
                        failures.add(mixinNode.name + ": missing Minecraft target class " + targetInternal);
                        continue;
                    }
                    verifyMixinMethods(mixinNode, targetNode, cache, failures);
                }
            }
        }

        assertTrue(checkedMixins >= 2, "Expected at least 2 mixin classes, found " + checkedMixins);
        assertTrue(failures.isEmpty(), "Mixin bytecode contract failures:\n" + String.join("\n", failures));
    }

    @Test
    void releaseMatrixAuditWhenRequired() throws IOException {
        if (!"true".equalsIgnoreCase(System.getProperty("tapetum.requireAllReleases"))) {
            return;
        }
        String releasesDir = System.getProperty("tapetum.releasesDir");
        assertNotNull(releasesDir, "tapetum.releasesDir must be set");
        Path dir = Path.of(releasesDir);
        assertTrue(Files.isDirectory(dir), "Releases directory missing: " + dir);
        List<Path> jars;
        try (var stream = Files.list(dir)) {
            jars = stream.filter(p -> p.toString().endsWith(".jar")).toList();
        }
        assertTrue(jars.size() == 10, "Expected 10 release JARs, found " + jars.size());
        for (Path jar : jars) {
            try (JarFile jf = new JarFile(jar.toFile())) {
                assertNotNull(jf.getJarEntry("tapetum.mixins.json"), "Missing tapetum.mixins.json in " + jar.getFileName());
                assertNotNull(jf.getJarEntry("assets/tapetum/icon.png"), "Missing icon.png in " + jar.getFileName());
                var entries = jf.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    for (String forbidden : FORBIDDEN_STUB_PREFIXES) {
                        assertFalse(name.startsWith(forbidden), "Stub leaked into " + jar.getFileName() + ": " + name);
                    }
                }
            }
        }
    }

    private static void verifyMixinMethods(
            ClassNode mixinNode, ClassNode targetNode, Map<String, ClassNode> cache, List<String> failures) {
        for (MethodNode m : mixinNode.methods) {
            if (m.visibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode ann : m.visibleAnnotations) {
                if (!INJECTOR_ANNOTATIONS.contains(ann.desc)) {
                    continue;
                }
                if ("Lorg/spongepowered/asm/mixin/Overwrite;".equals(ann.desc)) {
                    if (!hasMethod(targetNode, m.name, m.desc, cache)) {
                        failures.add(mixinNode.name + "." + m.name + m.desc + " -> @Overwrite target missing on " + targetNode.name);
                    }
                    continue;
                }
                for (String spec : getStringList(ann, "method")) {
                    if (!matchesMethodSpec(targetNode, spec, cache)) {
                        failures.add(mixinNode.name + "." + m.name + " -> method '" + spec + "' not found on " + targetNode.name);
                    }
                }
            }
        }
    }

    private static boolean matchesMethodSpec(ClassNode node, String spec, Map<String, ClassNode> cache) {
        int paren = spec.indexOf('(');
        String name = paren >= 0 ? spec.substring(0, paren) : spec;
        String desc = paren >= 0 ? spec.substring(paren) : null;
        return hasMethod(node, name, desc, cache);
    }

    private static boolean hasMethod(ClassNode node, String name, String desc, Map<String, ClassNode> cache) {
        ClassNode cur = node;
        while (cur != null) {
            for (MethodNode m : cur.methods) {
                if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) {
                    return true;
                }
            }
            if (cur.superName == null || "java/lang/Object".equals(cur.superName)) {
                break;
            }
            cur = loadClasspathClass(cur.superName, cache);
        }
        return false;
    }

    private static List<String> extractMixinTargets(AnnotationNode mixinAnn) {
        List<String> targets = new ArrayList<>();
        if (mixinAnn.values == null) {
            return targets;
        }
        for (int i = 0; i < mixinAnn.values.size(); i += 2) {
            String key = (String) mixinAnn.values.get(i);
            Object val = mixinAnn.values.get(i + 1);
            if ("value".equals(key) && val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Type t) {
                        targets.add(t.getInternalName());
                    }
                }
            } else if ("targets".equals(key) && val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) {
                        targets.add(s.replace('.', '/'));
                    }
                }
            }
        }
        return targets;
    }

    private static List<String> getStringList(AnnotationNode ann, String key) {
        List<String> out = new ArrayList<>();
        if (ann.values == null) {
            return out;
        }
        for (int i = 0; i < ann.values.size(); i += 2) {
            if (key.equals(ann.values.get(i)) && ann.values.get(i + 1) instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> anns, String desc) {
        if (anns == null) {
            return null;
        }
        for (AnnotationNode ann : anns) {
            if (desc.equals(ann.desc)) {
                return ann;
            }
        }
        return null;
    }

    private static ClassNode loadClasspathClass(String internalName, Map<String, ClassNode> cache) {
        ClassNode cached = cache.get(internalName);
        if (cached != null) {
            return cached;
        }
        String entryName = internalName + ".class";
        try (InputStream is = MixinTargetBytecodeContractTest.class.getClassLoader().getResourceAsStream(entryName)) {
            if (is != null) {
                ClassNode cn = readClass(is);
                cache.put(internalName, cn);
                return cn;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static ClassNode readClass(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cn;
    }
}

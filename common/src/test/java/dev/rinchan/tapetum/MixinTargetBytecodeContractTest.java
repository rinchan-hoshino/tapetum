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
 * Verifies every compiled @Mixin class in the packaged Tapetum JAR against real target
 * bytecode from Minecraft and optional mod JARs (Sodium, Iris, GeckoLib, AzureLib, TaCZ, TLM, SlashBlade).
 */
final class MixinTargetBytecodeContractTest {
    private static final List<String> INJECTOR_ANNOTATIONS = List.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/Overwrite;");

    @Test
    void allMixinTargetsAndMethodSignaturesExistInRealBytecode() throws IOException {
        String jarPath = System.getProperty("tapetum.packagedJar");
        String cpDir = System.getProperty("tapetum.classpathDir");
        String jijDir = System.getProperty("tapetum.extractedJijDir");
        assertNotNull(jarPath, "tapetum.packagedJar system property must be set");
        assertNotNull(cpDir, "tapetum.classpathDir system property must be set");

        List<JarFile> modJars = openModJars(Path.of(cpDir), jijDir != null ? Path.of(jijDir) : null);
        Map<String, ClassNode> cache = new HashMap<>();
        List<String> failures = new ArrayList<>();
        int checkedMixins = 0;

        try (JarFile packaged = new JarFile(jarPath)) {
            var entries = packaged.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                ClassNode mixinNode = readClass(packaged.getInputStream(entry));
                AnnotationNode mixinAnn = findAnnotation(mixinNode.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
                if (mixinAnn == null) {
                    continue;
                }
                checkedMixins++;
                List<String> targetInternalNames = extractMixinTargets(mixinAnn);
                assertFalse(targetInternalNames.isEmpty(), "Empty @Mixin targets on " + mixinNode.name);

                for (String targetInternal : targetInternalNames) {
                    ClassNode targetNode = loadTargetClass(targetInternal, modJars, cache);
                    if (targetNode == null) {
                        failures.add(mixinNode.name + ": missing target class " + targetInternal);
                        continue;
                    }
                    verifyMixinMethods(mixinNode, targetNode, modJars, cache, failures);
                }
            }
        } finally {
            for (JarFile jf : modJars) {
                jf.close();
            }
        }

        assertTrue(checkedMixins >= 20, "Expected at least 20 mixin classes, found " + checkedMixins);
        assertTrue(failures.isEmpty(), "Mixin bytecode contract failures:\n" + String.join("\n", failures));
    }

    private static void verifyMixinMethods(
            ClassNode mixinNode,
            ClassNode targetNode,
            List<JarFile> modJars,
            Map<String, ClassNode> cache,
            List<String> failures) {
        for (MethodNode m : mixinNode.methods) {
            if (m.visibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode ann : m.visibleAnnotations) {
                if (!INJECTOR_ANNOTATIONS.contains(ann.desc)) {
                    continue;
                }
                if ("Lorg/spongepowered/asm/mixin/Overwrite;".equals(ann.desc)) {
                    if (!hasMethod(targetNode, m.name, m.desc, modJars, cache)) {
                        failures.add(mixinNode.name + "." + m.name + m.desc + " -> @Overwrite target missing on " + targetNode.name);
                    }
                    continue;
                }
                List<String> methods = getStringList(ann, "method");
                for (String spec : methods) {
                    if (!matchesMethodSpec(targetNode, spec, modJars, cache)) {
                        failures.add(mixinNode.name + "." + m.name + " -> method '" + spec + "' not found on " + targetNode.name);
                    }
                }
            }
        }
    }

    private static boolean matchesMethodSpec(
            ClassNode node, String spec, List<JarFile> modJars, Map<String, ClassNode> cache) {
        int paren = spec.indexOf('(');
        String name = paren >= 0 ? spec.substring(0, paren) : spec;
        String desc = paren >= 0 ? spec.substring(paren) : null;
        return hasMethod(node, name, desc, modJars, cache);
    }

    private static boolean hasMethod(
            ClassNode node, String name, String desc, List<JarFile> modJars, Map<String, ClassNode> cache) {
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
            cur = loadTargetClass(cur.superName, modJars, cache);
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

    private static ClassNode loadTargetClass(
            String internalName, List<JarFile> modJars, Map<String, ClassNode> cache) {
        ClassNode cached = cache.get(internalName);
        if (cached != null) {
            return cached;
        }
        String entryName = internalName + ".class";
        for (JarFile jf : modJars) {
            JarEntry entry = jf.getJarEntry(entryName);
            if (entry != null) {
                try (InputStream is = jf.getInputStream(entry)) {
                    ClassNode cn = readClass(is);
                    cache.put(internalName, cn);
                    return cn;
                } catch (IOException ignored) {
                }
            }
        }
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

    private static List<JarFile> openModJars(Path cpDir, Path jijDir) throws IOException {
        List<String> jarNames = List.of(
                "sodium-neoforge-0.8.13+mc1.21.1.jar",
                "iris-neoforge-1.8.14-beta.1+mc1.21.1.jar",
                "geckolib-neoforge-1.21.1-4.9.3.jar",
                "azurelib-neo-1.21.1-3.2.8.jar",
                "tacz-neoforge-1.21.1-1.2.0.jar",
                "touhoulittlemaid-1.5.3-neoforge+mc1.21.1.jar",
                "SlashBladeResharped-2.1.9-1.21.1.jar",
                "kaleidoscopedoll-1.4.2-neoforge+mc1.21.1.jar");
        List<JarFile> out = new ArrayList<>();
        for (String name : jarNames) {
            Path p = cpDir.resolve(name);
            if (Files.exists(p)) {
                out.add(new JarFile(p.toFile()));
            }
        }
        if (jijDir != null && Files.isDirectory(jijDir)) {
            for (String jij : List.of("sodium-mod.jar", "simplebedrockmodel-v2.jar")) {
                Path p = jijDir.resolve(jij);
                if (Files.exists(p)) {
                    out.add(new JarFile(p.toFile()));
                }
            }
        }
        return out;
    }
}

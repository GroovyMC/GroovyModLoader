/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.internal.scripts;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import groovy.lang.GroovyClassLoader;
import groovy.util.logging.Slf4j;
import groovyjarjarasm.asm.ClassWriter;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModClassVisitor;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.IModFile;
import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public final class ScriptFileCompiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Supplier<List<GImport>> IMPORTS = Suppliers.memoize(() -> {
        final class Helper {
            private static void readNested(@Nullable JsonArray array, Consumer<JsonElement> consumer) {
                if (array == null) return;
                array.forEach(it -> {
                    if (it.isJsonArray()) readNested(it.getAsJsonArray(), consumer);
                    else if (!it.isJsonNull()) consumer.accept(it);
                });
            }

            @Nullable
            @SuppressWarnings("SameParameterValue")
            private static <T> T atIndexOrNull(T[] values, int index) {
                if (index >= values.length) return null;
                return values[index];
            }
        }

        try (final var is = ScriptFileCompiler.class.getResourceAsStream("/script_imports.json5")) {
            if (is != null) {
                final List<GImport> imports = new ArrayList<>();

                final var json = GSON.fromJson(new InputStreamReader(is), JsonObject.class);

                final List<String> packages = new ArrayList<>();
                Helper.readNested(json.getAsJsonArray("packages"), it -> packages.add(it.getAsString()));
                imports.add(new PackageImport(packages.toArray(String[]::new)));

                Helper.readNested(json.getAsJsonArray("classes"), it -> {
                    final String[] split = it.getAsString().split(" as ");
                    imports.add(new ClassImport(split[0], Helper.atIndexOrNull(split, 1)));
                });

                Helper.readNested(json.getAsJsonArray("statics"), it -> {
                    final String[] methodSplit = it.getAsString().split("#");
                    final String[] aliasSplit = methodSplit[1].split(" as ");
                    imports.add(new StaticImport(methodSplit[0], aliasSplit[0], Helper.atIndexOrNull(aliasSplit, 1)));
                });

                return imports;
            }
        } catch (IOException e) {
            LOGGER.error("Encountered exception reading script compilation imports: ", e);
        }

        return List.of();
    });

    private final FileSystem fs;
    private final String modId, rootPackage;
    private final AtomicBoolean wasCompiled;
    private final ModFile modFile;

    public ScriptFileCompiler(FileSystem fs, String modId, String rootPackage, AtomicBoolean wasCompiled, ModFile modFile) {
        this.fs = fs;
        this.modId = modId;
        this.rootPackage = rootPackage;
        this.wasCompiled = wasCompiled;
        this.modFile = modFile;
    }

    public void compile(ModFileScanData scanData) {
        if (wasCompiled.get()) return; wasCompiled.set(true);
        LOGGER.info("Compiling script mod {}", modId);

        try (final Stream<Path> stream = Files.walk(fs.getPath("scripts"))
                .filter(it -> {
                    final String fileName = it.toString();
                    return fileName.endsWith(".groovy") && !fileName.equals("mods.groovy");
                })) {
            // Compile the classes
            compileClasses(stream.toList());

            final Path mainClassPath = fs.getPath(rootPackage, "Main.class");
            // And if we don't have a main class, generate it
            if (!Files.exists(mainClassPath) && !Files.exists(fs.getPath(rootPackage, "main.class"))) {
                // If the main class doesn't exist, create it
                final var cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
                generateMainClass().accept(cw);
                if (!Files.exists(mainClassPath.getParent())) Files.createDirectories(mainClassPath.getParent());
                Files.write(mainClassPath, cw.toByteArray());
            }
        } catch (IOException exception) {
            LOGGER.error("Encountered exception compiling class: ", exception);
        }

        modFile.scanFile((Path path) -> {
            try (final InputStream inStream = Files.newInputStream(path)) {
                final ModClassVisitor mcv = new ModClassVisitor();
                final ClassReader cr = new ClassReader(inStream);
                cr.accept(mcv, 0);
                mcv.buildData(scanData.getClasses(), scanData.getAnnotations());
            } catch (final IOException | IllegalArgumentException ignored) {
                // We can ignore this
            }
        });
    }

    private void compileClasses(final List<Path> paths) throws IOException {
        final CompilationUnit unit = createCompilationUnit();
        paths.forEach(LamdbaExceptionUtils.rethrowConsumer(path -> unit.addSource(path.getFileName().toString().replace(".groovy", ""),
                Files.readString(path))));

        CompilationUnit.ClassgenCallback collector = createCollector(unit);
        unit.setClassgenCallback(collector);
        unit.compile(Phases.SEMANTIC_ANALYSIS);
        // Set the package name for the classes if not explicitly defined
        unit.getAST().getClasses().forEach(classNode -> {
            if (!classNode.hasPackageName())
                classNode.setName(rootPackage + "." + classNode.getName());
        });
        unit.compile(Phases.CLASS_GENERATION);
        paths.forEach(LamdbaExceptionUtils.rethrowConsumer(Files::delete));
    }

    private CompilationUnit.ClassgenCallback createCollector(CompilationUnit unit) {
        return (classVisitor, classNode) -> {
            if (classNode.getNameWithoutPackage().equalsIgnoreCase("main")) {
                final var annotation = classVisitor.visitAnnotation("Lcom/matyrobbrt/gml/GMod;", true);
                annotation.visit("value", modId);
                annotation.visitEnd();
            }
            byte[] data = ((ClassWriter) classVisitor).toByteArray();
            BytecodeProcessor bytecodePostprocessor = unit.getConfiguration().getBytecodePostprocessor();
            if (bytecodePostprocessor != null) {
                data = bytecodePostprocessor.processBytecode(classNode.getName(), data);
            }
            final Path path = fs.getPath(classNode.getName().replace('.', '/') + ".class");
            try {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
                Files.write(path, data);
            } catch (IOException e) {
                LOGGER.error("Exception saving script: ", e);
                throw new RuntimeException(e);
            }
        };
    }

    @SuppressWarnings("removal")
    private CompilationUnit createCompilationUnit() {
        final var compilerConfig = new CompilerConfiguration()
                .addCompilationCustomizers(
                        setupImports(new ImportCustomizer()),
                        new ASTTransformationCustomizer(Map.of("category", modId), Slf4j.class)
                );

        compilerConfig.setOptimizationOptions(Map.of(
                CompilerConfiguration.PARALLEL_PARSE, false,
                CompilerConfiguration.GROOVYDOC, true,
                CompilerConfiguration.RUNTIME_GROOVYDOC, true
        ));

        final var transformingCL = FMLLoader.getGameLayer().findLoader("forge");
        final var cl = java.security.AccessController.doPrivileged((PrivilegedAction<GroovyClassLoader>) () -> new GroovyClassLoader(transformingCL));

        return new CompilationUnit(compilerConfig, null, cl);
    }

    private ImportCustomizer setupImports(ImportCustomizer customizer) {
        IMPORTS.get().forEach(it -> it.add(customizer));
        return customizer;
    }

    private ClassNode generateMainClass() {
        final var node = new ClassNode();
        node.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, rootPackage + "/Main", null, Type.getInternalName(Object.class), null);
        {
            final var annotation = node.visitAnnotation("Lcom/matyrobbrt/gml/GMod;", true);
            annotation.visit("value", modId);
            annotation.visitEnd();
        }
        {
            // Add the public constructor
            final var mv = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(5, label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLocalVariable("this", "L" + rootPackage + "/Main;", null, label0, label1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        node.visitEnd();
        return node;
    }

    public static boolean isScriptMod(IModFile file) {
        return (boolean)file.getModFileInfo().getFileProperties().getOrDefault("groovyscript", false);
    }
}

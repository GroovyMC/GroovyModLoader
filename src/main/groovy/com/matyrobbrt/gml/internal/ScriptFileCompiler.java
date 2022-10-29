/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.internal;

import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import groovy.lang.GroovyClassLoader;
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
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public final class ScriptFileCompiler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final FileSystem fs;
    private final String modId, rootPackage;
    private final AtomicBoolean wasCompiled;
    private final ModFile modFile;

    ScriptFileCompiler(FileSystem fs, String modId, String rootPackage, AtomicBoolean wasCompiled, ModFile modFile) {
        this.fs = fs;
        this.modId = modId;
        this.rootPackage = rootPackage;
        this.wasCompiled = wasCompiled;
        this.modFile = modFile;
    }

    void compile(ModFileScanData scanData) {
        if (wasCompiled.get()) return; wasCompiled.set(true);
        LOGGER.info("Compiling script mod {}", modId);

        try (final Stream<Path> stream = Files.walk(fs.getPath("scripts"))
                .filter(it -> it.toString().endsWith(".groovy"))) {
            // Compile the classes
            compileClasses(stream.toList());

            Path mainClassPath = fs.getPath(rootPackage, "Main.class");
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
                ModClassVisitor mcv = new ModClassVisitor();
                ClassReader cr = new ClassReader(inStream);
                cr.accept(mcv, 0);
                mcv.buildData(scanData.getClasses(), scanData.getAnnotations());
            } catch (final IOException | IllegalArgumentException ignored) {
                // We can ignore this
            }
        });
    }

    private void compileClasses(final List<Path> paths) throws IOException {
        CompilationUnit unit = createCompilationUnit();
        paths.forEach(LamdbaExceptionUtils.rethrowConsumer(path -> unit.addSource(path.getFileName().toString().replace(".groovy", ""),
                Files.readString(path))));

        CompilationUnit.ClassgenCallback collector = createCollector(unit);
        unit.setClassgenCallback(collector);
        unit.compile(Phases.CONVERSION);
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
            Path path = fs.getPath(classNode.getName().replace('.', '/') + ".class");
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
                .addCompilationCustomizers(setupImports(new ImportCustomizer()));

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
        return customizer
                .addStarImports(
                        // GML stuff
                        "com.matyrobbrt.gml", "com.matyrobbrt.gml.bus", "com.matyrobbrt.gml.bus.type",
                        // Lifecycle events and eventbus API
                        "net.minecraftforge.eventbus.api", "net.minecraftforge.fml.event.lifecycle", "net.minecraft.network.chat",
                        // Add some mc events
                        "net.minecraftforge.event", "net.minecraftforge.event.entity", "net.minecraftforge.event.entity.living", "net.minecraftforge.event.entity.item", "net.minecraftforge.event.entity.player", "net.minecraftforge.event.level", "net.minecraftforge.event.server")
                .addImport("CommonSetupEvent", "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent")
                .addImport("ClientSetupEvent", "net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent")
                .addImport("EBS", "com.matyrobbrt.gml.bus.EventBusSubscriber")
                .addStaticImport("com.mojang.logging.LogUtils", "getLogger");
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

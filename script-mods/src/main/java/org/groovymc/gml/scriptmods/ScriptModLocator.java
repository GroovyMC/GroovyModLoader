/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.base.Suppliers;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Feature;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import com.google.common.jimfs.SystemJimfsFileSystemProvider;
import org.groovymc.gml.scriptmods.util.ConfigurableBuilder;
import org.groovymc.gml.scriptmods.util.FileSystemInjector;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import cpw.mods.niofs.union.UnionFileSystemProvider;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.CodeSigner;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class ScriptModLocator implements IModLocator {
    private static final UnionFileSystemProvider UFSP = (UnionFileSystemProvider) FileSystemProvider.installedProviders().stream().filter(fsp->fsp.getScheme().equals("union")).findFirst().orElseThrow(()->new IllegalStateException("Couldn't find UnionFileSystemProvider"));
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModLocator.class);
    public static final String SCRIPTS_DIR = "scripts";
    private static final boolean INJECTED_FS;

    static {
        boolean managedFsInjection;
        try {
            //noinspection deprecation
            FileSystemInjector.injectFileSystem(new SystemJimfsFileSystemProvider());
            managedFsInjection = true;
        } catch (Exception exception) {
            LOGGER.error("Encountered exception injecting Jimfs FS: ", exception);
            managedFsInjection = false;
        }
        INJECTED_FS = managedFsInjection;

        LOGGER.info("Injected Jimfs file system");
    }

    private final TriFunction<FileSystem, IModFile, String, IConfigurable> infoParser;

    public ScriptModLocator(TriFunction<FileSystem, IModFile, String, IConfigurable> infoParser) {
        this.infoParser = infoParser;
    }

    @SuppressWarnings("unused")
    public ScriptModLocator() {
        this((fs, file, modId) -> new ConfigurableBuilder()
                .add("modLoader", "gml")
                .add("loaderVersion", "[1,)")
                .add("license", "All Rights Reserved")
                .addList("mods", new ConfigurableBuilder()
                        .add("modId", modId)
                        .add("version", "1.0.0"))
                .add("properties", Map.of("groovyscript", true))
                .build());
    }

    @Override
    public List<ModFileOrException> scanMods() {
        final var files = new ArrayList<ModFileOrException>();
        getScanDirs().forEach(LamdbaExceptionUtils.rethrowConsumer(dir -> {
            final var dirAbs = dir.toAbsolutePath();
            if (!Files.exists(dirAbs)) {
                LOGGER.info("Skipped loading script mods from directory {} as it did not exist.", dirAbs);
                return;
            }

            try (final var stream = Files.walk(dir, 1)
                    .map(Path::toAbsolutePath)
                    .filter(it -> !dirAbs.equals(it))) {
                stream.map(this::createMod).forEach(files::add);
            }
        }));
        return files;
    }

    @Override
    public String name() {
        return "GScriptMods";
    }

    @Override
    public void scanFile(IModFile file, Consumer<Path> pathConsumer) {
        LOGGER.debug(LogMarkers.SCAN, "Scan started: {}", file);
        try (Stream<Path> files = Files.find(file.getSecureJar().getRootPath(), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
            files.forEach(pathConsumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.debug(LogMarkers.SCAN, "Scan finished: {}", file);
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    @SuppressWarnings("resource")
    protected ModFileOrException createMod(Path inPath) {
        LOGGER.info("Creating mod info for script mod {}", inPath);
        var modId = withoutExtension(inPath).toLowerCase(Locale.ROOT);

        final FileSystem fs;
        try {
            fs = Jimfs.newFileSystem(Configuration.builder(PathType.unix())
                    .setRoots("/")
                    .setWorkingDirectory("/")
                    .setAttributeViews("basic")
                    .setSupportedFeatures(Feature.SECURE_DIRECTORY_STREAM, Feature.FILE_CHANNEL)
                    .build());
        } catch (Exception exception) {
            LOGGER.error("Encountered exception creating fs: ", exception);
            throw new RuntimeException(exception);
        }

        try {
            if (Files.isDirectory(inPath)) {
                final var scriptsDir = fs.getPath(SCRIPTS_DIR);
                Files.createDirectories(scriptsDir);
                // The mod has multiple scripts, let's find them
                try (final var stream = Files.list(inPath).filter(Files::isRegularFile)
                        .filter(file -> FileUtils.fileExtension(file).equals("groovy"))) {
                    // ... and add them to the fs
                    stream.forEach(LamdbaExceptionUtils.rethrowConsumer(script -> {
                        final var outPath = scriptsDir.resolve(script.getFileName().toString());
                        Files.copy(script, outPath);
                    }));
                }
            } else if (Files.isRegularFile(inPath)) {
                // This file is the main file of the mod, so let's add it as Main.groovy
                var mainPath = fs.getPath(SCRIPTS_DIR, "Main.groovy");
                Files.createDirectories(mainPath.getParent());
                Files.write(mainPath, Files.readAllBytes(inPath));
            } else {
                throw new IllegalArgumentException("Script mod at " + inPath + " is not a directory nor a file!");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to set up script mod: ", e);
            throw new RuntimeException(e);
        }

        final PathGetter pathGetter;
        if (INJECTED_FS) {
            // We managed to do unsafe hacks to inject the file system.
            // No need to use union as a delegate
            pathGetter = fs::getPath;
        } else {
            // Unfortunately, we didn't manage to inject the file system.
            // So, we use union as a delegate so that class loaders can use the URIs to query classes
            var union = UFSP.newFileSystem((a, b) -> true, fs.getPath("/"));
            pathGetter = union::getPath;
        }

        var manifest = new Manifest();
        manifest.getMainAttributes().putValue("Implementation-Version", "1.0.0");
        var mjm = new ModJarMetadata();
        var sj = new ScriptJar(
                fs, inPath, mjm, modId, new SecureJar.ModuleDataProvider() {
            @Override
            public String name() {
                return modId;
            }

            @Override
            public ModuleDescriptor descriptor() {
                return mjm.descriptor();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Optional<URI> findFile(String name) {
                return Optional.of(pathGetter.getPath(name)).filter(Files::exists).map(Path::toUri);
            }

            @Override
            public Optional<InputStream> open(String name) {
                return Optional.of(pathGetter.getPath(name)).filter(Files::exists).map(LamdbaExceptionUtils.rethrowFunction(Files::newInputStream));
            }

            @Override
            public Manifest getManifest() {
                return manifest;
            }

            @Override
            public CodeSigner[] verifyAndGetSigners(String cname, byte[] bytes) {
                return new CodeSigner[0];
            }
        });

        var mod = new ScriptModFile(sj, this, file -> new ModFileInfo((ModFile) file, infoParser.apply(fs, file, modId), List.of()), modId, modId);

        mjm.setModFile(mod);
        return new ModFileOrException(mod, null);
    }

    private static String withoutExtension(final Path path) {
        String fileName = path.getFileName().toString();
        int idx = fileName.lastIndexOf('.');
        if (idx > -1) {
            return fileName.substring(0, idx);
        } else {
            return fileName;
        }
    }

    public List<Path> getScanDirs() {
        return SCAN_DIRS_CONFIG.get().stream().map(FMLPaths.GAMEDIR.get()::resolve).map(Path::toAbsolutePath).toList();
    }

    private static final Supplier<List<String>> SCAN_DIRS_CONFIG = Suppliers.memoize(() -> getScanDirsFromConfig(FMLPaths.CONFIGDIR.get().resolve("gml-script-mods.toml")));

    private static List<String> getScanDirsFromConfig(Path configPath) {
        try (final var configData = CommentedFileConfig.builder(configPath)
                .onFileNotFound((file, configFormat) -> {
                    Files.write(file, List.of(
                            "# The folders (relative to the base game directory) Groovy script mods should be read from.",
                            "folders = [\"mods/scripts\"]"
                    ));
                    return true;
                })
                .build()) {
            configData.load();

            return configData.getOrElse("folders", List.of("mods/scripts"));
        } catch (Exception exception) {
            LOGGER.error("Failed to load script mods config file from {}: ", configPath, exception);
        }
        return List.of("mods/scripts");
    }

    @FunctionalInterface
    private interface PathGetter {
        Path getPath(String first, String... more);
    }
}

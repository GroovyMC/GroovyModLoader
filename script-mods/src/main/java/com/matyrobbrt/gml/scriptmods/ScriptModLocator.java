package com.matyrobbrt.gml.scriptmods;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Feature;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import com.matyrobbrt.gml.scriptmods.cfg.ConfigurableBuilder;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import cpw.mods.niofs.union.UnionFileSystemProvider;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.CodeSigner;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class ScriptModLocator implements IModLocator {
    private static final UnionFileSystemProvider UFSP = (UnionFileSystemProvider) FileSystemProvider.installedProviders().stream().filter(fsp->fsp.getScheme().equals("union")).findFirst().orElseThrow(()->new IllegalStateException("Couldn't find UnionFileSystemProvider"));
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModLocator.class);
    @Override
    public List<ModFileOrException> scanMods() {
        final var files = new ArrayList<ModFileOrException>();
        getScanDirs().forEach(LamdbaExceptionUtils.rethrowConsumer(dir -> {
            try (final var stream = Files.list(dir).filter(Files::isRegularFile)) {
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
        var modId = inPath.getFileName().toString().toLowerCase(Locale.ROOT).replace(".groovy", "");
        FileSystem fs;
        try {
            var method = MethodHandles.privateLookupIn(Jimfs.class, MethodHandles.lookup()).findStatic(Jimfs.class, "newFileSystem", MethodType.methodType(FileSystem.class, URI.class, Configuration.class));
            fs = (FileSystem) method.invoke(new URI("jimfs", "mod#" + modId, null, null), Configuration.builder(PathType.unix())
                    .setRoots("/")
                    .setWorkingDirectory("/")
                    .setAttributeViews("basic")
                    .setSupportedFeatures(Feature.SECURE_DIRECTORY_STREAM, Feature.FILE_CHANNEL)
                    .build());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try {
            // This file is the main file of the mod, so let's add it as Main.groovy
            var mainPath = fs.getPath("scripts", "Main.groovy");
            Files.createDirectories(mainPath.getParent());
            Files.write(mainPath, Files.readAllBytes(inPath));
        } catch (IOException e) {
            LOGGER.error("Failed to set up script mod: ", e);
            throw new RuntimeException(e);
        }
        var union = UFSP.newFileSystem((a, b) -> true, fs.getPath("/"));

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
                return Optional.of(union.getPath(name)).filter(Files::exists).map(Path::toUri);
            }

            @Override
            public Optional<InputStream> open(String name) {
                return Optional.of(union.getPath(name)).filter(Files::exists).map(LamdbaExceptionUtils.rethrowFunction(Files::newInputStream));
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

        var conf = new ConfigurableBuilder()
                .add("modLoader", "gml")
                .add("loaderVersion", "[1,)")
                .add("license", "All Rights Reserved")
                .addList("mods", new ConfigurableBuilder()
                        .add("modId", modId)
                        .add("version", "1.0.0"))
                .build();
        var mod = new ScriptModFile(sj, this, file -> new ModFileInfo((ModFile) file, conf, List.of()), modId, modId);

        mjm.setModFile(mod);
        return new ModFileOrException(mod, null);
    }

    public List<Path> getScanDirs() {
        return List.of(FMLPaths.GAMEDIR.get().resolve("mods/scripts"));
    }
}

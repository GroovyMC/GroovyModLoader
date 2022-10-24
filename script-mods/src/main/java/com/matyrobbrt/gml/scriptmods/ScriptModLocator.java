package com.matyrobbrt.gml.scriptmods;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.matyrobbrt.gml.scriptmods.cfg.ConfigurableBuilder;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
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
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class ScriptModLocator implements IModLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModLocator.class);
    @Override
    public List<ModFileOrException> scanMods() {
        final var files = new ArrayList<ModFileOrException>();
        getScanDirs().forEach(LamdbaExceptionUtils.rethrowConsumer(dir -> {
            try (final var stream = Files.list(dir)) {
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

    protected IModLocator.ModFileOrException createMod(Path inPath) {
        LOGGER.info("Creating mod info for script mod {}", inPath);
        var modId = inPath.getFileName().toString().replace(".groovy", "");
        FileSystem fs;
        try {
            var method = MethodHandles.privateLookupIn(Jimfs.class, MethodHandles.lookup()).findStatic(Jimfs.class, "newFileSystem", MethodType.methodType(FileSystem.class, URI.class, Configuration.class));
            fs = (FileSystem) method.invoke(new java.net.URI("jimfs", "mod#" + modId, null, null), Configuration.windows());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try {
            // This file is the main file of the mod, so let's add it as Main.groovy
            Files.write(fs.getPath("scripts", "Main.groovy"), Files.readAllBytes(inPath));
        } catch (IOException e) {
            LOGGER.error("Failed to set up script mod: ", e);
            throw new RuntimeException(e);
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
                return inPath.toUri();
            }

            @Override
            public Optional<URI> findFile(String name) {
                return Optional.of(fs.getPath(name)).filter(Files::exists).map(Path::toUri);
            }

            @Override
            public Optional<InputStream> open(String name) {
                return Optional.of(fs.getPath(name)).filter(Files::exists).map(LamdbaExceptionUtils.rethrowFunction(Files::newInputStream));
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
                .add("loaderVersion", "[1.0.0,)")
                .addList("mods", new ConfigurableBuilder()
                        .add("modId", modId)
                        .add("version", "1.0.0"))
                .build();
        var mod = new ScriptModFile(sj, this, file -> new ModFileInfo((ModFile) file, conf, List.of()), modId, modId);

        mjm.setModFile(mod);
        return new IModLocator.ModFileOrException(mod, null);
    }

    public List<Path> getScanDirs() {
        return List.of(Path.of("mods/scripts"));
    }
}

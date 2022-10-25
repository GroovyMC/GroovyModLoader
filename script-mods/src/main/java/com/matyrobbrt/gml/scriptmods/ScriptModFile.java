package com.matyrobbrt.gml.scriptmods;

import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModProvider;
import net.minecraftforge.forgespi.locating.ModFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class ScriptModFile extends ModFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModFile.class);

    public final AtomicBoolean wasCompiled = new AtomicBoolean();
    public final FileSystem fs;
    public final String rootPackage;
    public final String modId;
    public ScriptModFile(ScriptJar scriptJar, IModProvider provider, ModFileFactory.ModFileInfoParser parser, String rootPackage, String modId) {
        super(scriptJar, provider, parser, "MOD");
        fs = scriptJar.fileSystem();
        this.rootPackage = rootPackage;
        this.modId = modId;
    }
}

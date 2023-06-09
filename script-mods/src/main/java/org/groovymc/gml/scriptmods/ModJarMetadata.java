/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods;

import cpw.mods.jarhandling.JarMetadata;
import net.minecraftforge.forgespi.locating.IModFile;

import java.lang.module.ModuleDescriptor;
import java.util.Objects;

public final class ModJarMetadata implements JarMetadata {
    private IModFile modFile;
    private ModuleDescriptor descriptor;

    ModJarMetadata() {
    }

    public void setModFile(IModFile file) {
        this.modFile = file;
    }

    @Override
    public String name() {
        return modFile.getModFileInfo().moduleName();
    }

    @Override
    public String version() {
        return modFile.getModFileInfo().versionString();
    }

    @Override
    public ModuleDescriptor descriptor() {
        if (descriptor != null) return descriptor;
        var bld = ModuleDescriptor.newAutomaticModule(name())
                .version(version())
                .packages(modFile.getSecureJar().getPackages());
        modFile.getSecureJar().getProviders().stream()
                .filter(p -> !p.providers().isEmpty())
                .forEach(p -> bld.provides(p.serviceName(), p.providers()));
        modFile.getModFileInfo().usesServices().forEach(bld::uses);
        descriptor = bld.build();
        return descriptor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModJarMetadata) obj;
        return Objects.equals(this.modFile, that.modFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modFile);
    }

    @Override
    public String toString() {
        return "ModJarMetadata[" +"modFile=" + modFile + ']';
    }
}
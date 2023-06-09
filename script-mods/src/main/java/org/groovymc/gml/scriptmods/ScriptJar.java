/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;

public record ScriptJar(FileSystem fileSystem, Path scriptPath, JarMetadata metadata, String packageName, ModuleDataProvider moduleDataProvider) implements SecureJar {
    @Override
    public Path getPrimaryPath() {
        return scriptPath;
    }

    @Override
    public CodeSigner[] getManifestSigners() {
        return new CodeSigner[0];
    }

    @Override
    public Status verifyPath(Path path) {
        return Status.VERIFIED;
    }

    @Override
    public Status getFileStatus(String name) {
        return Status.VERIFIED;
    }

    @Override
    public Attributes getTrustedManifestEntries(String name) {
        return null;
    }

    @Override
    public boolean hasSecurityData() {
        return false;
    }

    @Override
    public Set<String> getPackages() {
        return Set.of(packageName);
    }

    @Override
    public List<Provider> getProviders() {
        return List.of();
    }

    @Override
    public String name() {
        return metadata.name();
    }

    @Override
    public Path getPath(String first, String... rest) {
        return fileSystem.getPath(first, rest);
    }

    @Override
    public Path getRootPath() {
        return fileSystem.getPath("/");
    }
}

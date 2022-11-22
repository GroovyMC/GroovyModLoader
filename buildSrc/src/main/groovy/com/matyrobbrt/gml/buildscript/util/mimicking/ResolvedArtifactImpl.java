package com.matyrobbrt.gml.buildscript.util.mimicking;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;

import javax.annotation.Nullable;
import java.io.File;

@NonNullApi
public record ResolvedArtifactImpl(
        File file, ResolvedModuleVersion moduleVersion,
        String name, String type, String extension, String classifier,
        ComponentArtifactIdentifier identifier
) implements ResolvedArtifact {
    @Override
    public File getFile() {
        return file;
    }

    @Override
    public ResolvedModuleVersion getModuleVersion() {
        return moduleVersion;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Nullable
    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public ComponentArtifactIdentifier getId() {
        return identifier;
    }
}

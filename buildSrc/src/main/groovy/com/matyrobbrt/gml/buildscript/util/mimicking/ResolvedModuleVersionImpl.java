package com.matyrobbrt.gml.buildscript.util.mimicking;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedModuleVersion;

@NonNullApi
public record ResolvedModuleVersionImpl(
        ModuleVersionIdentifier id
) implements ResolvedModuleVersion {

    public ResolvedModuleVersionImpl(String group, String name, String version, ModuleIdentifier module) {
        this(new ModuleVersionIdentifier() {
            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getGroup() {
                return group;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public ModuleIdentifier getModule() {
                return module;
            }
        });
    }

    public ResolvedModuleVersionImpl(String group, String name, String version) {
        this(group, name, version, new ModuleIdentifier() {
            @Override
            public String getGroup() {
                return group;
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }

    @Override
    public ModuleVersionIdentifier getId() {
        return id;
    }
}

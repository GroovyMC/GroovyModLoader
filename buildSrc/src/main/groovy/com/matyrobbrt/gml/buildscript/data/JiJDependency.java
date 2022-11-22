package com.matyrobbrt.gml.buildscript.data;

import com.matyrobbrt.gml.buildscript.transform.ArtifactTransformer;
import com.matyrobbrt.gml.buildscript.util.DependencyUtils;
import com.matyrobbrt.gml.buildscript.JiJExtension;
import com.matyrobbrt.gml.buildscript.util.mimicking.ComponentArtifactIdentifierImpl;
import com.matyrobbrt.gml.buildscript.util.mimicking.ResolvedArtifactImpl;
import com.matyrobbrt.gml.buildscript.util.mimicking.ResolvedModuleVersionImpl;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.jvm.tasks.Jar;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record JiJDependency(
        String group, String artifact, String version, String range,
        boolean includeMetadata, boolean isObfuscated, String jarPath,
        File file, List<ArtifactTransformer> transformers
) {
    public static class Provider {
        private List<JiJDependency> cached;
        @Nonnull
        protected Supplier<List<JiJDependency>> supplier;

        public Provider(Supplier<List<JiJDependency>> supplier) {
            this.supplier = supplier;
        }

        public final List<JiJDependency> resolve() {
            if (cached != null) return cached;
            return cached = supplier.get();
        }
    }

    public static final class ConfigurationDependencyProvider extends Provider {
        private final JiJExtension extension;
        @SuppressWarnings({"DuplicatedCode", "PatternVariableCanBeUsed"})
        public ConfigurationDependencyProvider(Project project, List<Configuration> configurations) {
            super(null);
            this.extension = project.getExtensions().getByType(JiJExtension.class);
            this.supplier = () -> {
                final List<JiJDependency> dependencies = new ArrayList<>();
                for (final Configuration configuration : configurations) {
                    for (final Dependency dep : configuration.getAllDependencies()) {
                        if (dep instanceof ModuleDependency) {
                            final ModuleDependency moduleDependency = (ModuleDependency) dep;
                            for (final ResolvedArtifact artifact : DependencyUtils.getResolvedDependency(project, moduleDependency).getAllModuleArtifacts()) {
                                dependencies.add(fromArtifact(configuration, moduleDependency, artifact));
                            }
                        }
                    }
                }
                return dependencies;
            };
        }
        private JiJDependency fromArtifact(Configuration configuration, ModuleDependency dependency, ResolvedArtifact artifact) {
            return extension.resolve(dependency, configuration)
                    .build(artifact, dependency);
        }
    }

    public static Provider fromJar(Supplier<Jar> jarTask, JiJDependencyData dependencyData) {
        return new Provider(() -> List.of(dependencyData.build(
                artifactFromJar(jarTask.get()), null
        )));
    }

    private static ResolvedArtifact artifactFromJar(Jar jar) {
        return new ResolvedArtifactImpl(
                jar.getArchiveFile().get().getAsFile(),
                new ResolvedModuleVersionImpl(
                        jar.getProject().getGroup().toString(),
                        jar.getArchiveBaseName().get(),
                        jar.getArchiveVersion().get()
                ),
                jar.getArchiveBaseName().get(),
                "jar", jar.getArchiveExtension().get(),
                jar.getArchiveClassifier().get(),
                new ComponentArtifactIdentifierImpl(jar.getArchiveFileName().get())
        );
    }
}

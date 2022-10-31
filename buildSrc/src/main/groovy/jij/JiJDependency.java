package jij;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.jvm.tasks.Jar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public record JiJDependency(
        String group, String artifact, String version, @Nullable String range, File file
) {

    @SuppressWarnings("NullableProblems")
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
        private final Map<String, String> upperBounds = new HashMap<>();
        private final Map<String, String> ranges = new HashMap<>();
        private final Set<String> nextMajors = new HashSet<>();
        public ConfigurationDependencyProvider(Project project, List<Configuration> configurations) {
            super(null);
            this.supplier = () -> configurations.stream()
                    .flatMap(it -> it.getAllDependencies().stream())
                    .filter(ModuleDependency.class::isInstance)
                    .map(ModuleDependency.class::cast)
                    .flatMap(it -> DependencyUtils.getResolvedDependency(project, it).getAllModuleArtifacts()
                            .stream().map(art -> new ArtifactWithDep(art, it)))
                    .map(this::fromArtifact)
                    .toList();
        }

        public void setRange(String artifact, String range) {
            ranges.put(artifact, range);
        }
        public void setUpperBound(String artifact, String upperBound) {
            upperBounds.put(artifact, upperBound);
        }
        public void nextMajorBound(String artifact) {
            nextMajors.add(artifact);
        }

        private JiJDependency fromArtifact(ArtifactWithDep artifact) {
            final var val = artifact.artifact.getModuleVersion().getId();
            return new JiJDependency(
                    val.getGroup(), val.getName(), val.getVersion(),
                    DependencyUtils.isValidVersionRange(artifact.dependency.getVersion()) ? artifact.dependency.getVersion() : resolveRange(getId(artifact.artifact), val.getVersion()),
                    artifact.artifact.getFile()
            );
        }

        private String getId(ResolvedArtifact artifact) {
            final var val = artifact.getModuleVersion().getId();
            var id = val.getGroup() + ':' + val.getName();
            if (artifact.getClassifier() != null && !artifact.getClassifier().isBlank()) id += (":" + artifact.getClassifier());
            if (artifact.getClassifier() != null && !artifact.getClassifier().equals("jar")) id += ("@" + artifact.getClassifier());
            return id;
        }

        private String resolveRange(String artifact, String version) {
            if (ranges.containsKey(artifact)) return ranges.get(artifact);
            if (upperBounds.containsKey(artifact)) {
                return "[" + version + "," + upperBounds.get(artifact) + ")";
            }
            if (nextMajors.contains(artifact)) return DependencyUtils.range(version, DependencyUtils.nextMajor(version));
            return null;
        }

        private record ArtifactWithDep(ResolvedArtifact artifact, ModuleDependency dependency) {}
    }

    public static Provider fromJar(Supplier<Jar> jarTask, String versionRange) {
        return new Provider(() -> {
            final Jar jar = jarTask.get();
            final String actualRange = versionRange == null ?
                    DependencyUtils.range(jar.getArchiveVersion().get(), DependencyUtils.nextMajor(jar.getArchiveVersion().get())) :
                    versionRange;
            return List.of(new JiJDependency(
                    jar.getProject().getGroup().toString(),
                    jar.getArchiveBaseName().get(),
                    jar.getArchiveVersion().get(), actualRange,
                    jar.getArchiveFile().get().getAsFile()
            ));
        });
    }
}

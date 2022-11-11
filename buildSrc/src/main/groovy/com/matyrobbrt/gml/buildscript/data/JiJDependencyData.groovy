package com.matyrobbrt.gml.buildscript.data

import com.matyrobbrt.gml.buildscript.transform.ArtifactTransformer
import com.matyrobbrt.gml.buildscript.util.DependencyUtils
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact

@CompileStatic
class JiJDependencyData {
    public final DependencyAction<String> nextMajor = { ResolvedArtifact artifact, ModuleDependency dependency ->
        final currentVersion = getVersionAsString(artifact, dependency)
        DependencyUtils.range(currentVersion, DependencyUtils.nextMajor(currentVersion))
    }

    DependencyAction<String> group = { ResolvedArtifact artifact, ModuleDependency dependency -> artifact.moduleVersion.id.group }
    DependencyAction<String> artifactId = { ResolvedArtifact artifact, ModuleDependency dependency -> artifact.moduleVersion.id.name }
    DependencyAction<String> version = { ResolvedArtifact artifact, ModuleDependency dependency -> artifact.moduleVersion.id.version }
    DependencyAction<String> versionRange = { ResolvedArtifact artifact, ModuleDependency dependency -> "[${getVersionAsString(artifact, dependency)},)" }
    boolean includeMetadata = true
    DependencyAction<String> path = { ResolvedArtifact artifact, ModuleDependency dependency -> artifact.file.name }
    boolean obfuscated
    final List<ArtifactTransformer> transformers = []

    void group(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        group = closure as DependencyAction<String>
    }
    void setGroup(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        group = closure as DependencyAction<String>
    }
    void group(String value) {
        group = { a, b -> value }
    }
    void setGroup(String value) {
        group = { a, b -> value }
    }
    void group(DependencyAction<String> action) {
        version = action
    }

    void artifactId(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        artifactId = closure as DependencyAction<String>
    }
    void artifactId(String value) {
        artifactId = { a, b -> value }
    }
    void setArtifactId(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        artifactId = closure as DependencyAction<String>
    }
    void setArtifactId(String value) {
        artifactId = { a, b -> value }
    }
    void artifactId(DependencyAction<String> action) {
        artifactId = action
    }

    void version(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        version = closure as DependencyAction<String>
    }
    void version(String value) {
        version = { a, b -> value }
    }
    void setVersion(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        version = closure as DependencyAction<String>
    }
    void setVersion(String value) {
        version = { a, b -> value }
    }
    void version(DependencyAction<String> action) {
        version = action
    }

    String getVersionAsString(ResolvedArtifact artifact, ModuleDependency dependency) {
        version.call(artifact, dependency)
    }

    void versionRange(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        versionRange = closure as DependencyAction<String>
    }
    void versionRange(String value) {
        versionRange = { a, b -> value }
    }
    void setVersionRange(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        versionRange = closure as DependencyAction<String>
    }
    void setVersionRange(String value) {
        versionRange = { a, b -> value }
    }
    void versionRange(DependencyAction<String> action) {
        versionRange = action
    }

    void path(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        path = closure  as DependencyAction<String>
    }
    void path(String value) {
        path = { a, b -> value }
    }
    void setPath(@ClosureParams(
            value = SimpleType, options = ['org.gradle.api.artifacts.ResolvedArtifact', 'org.gradle.api.artifacts.ModuleDependency']
    ) Closure<String> closure) {
        path = closure  as DependencyAction<String>
    }
    void setPath(String value) {
        path = { a, b -> value }
    }
    void path(DependencyAction<String> action) {
        path = action
    }

    void transform(ArtifactTransformer transformer) {
        this.transformers.add(transformer)
    }

    void transform(Class<? extends ArtifactTransformer> transformer) {
        this.transformers.add(transformer.getDeclaredConstructor().newInstance())
    }

    JiJDependency build(ResolvedArtifact artifact, ModuleDependency dependency) {
        return new JiJDependency(
                group.call(artifact, dependency), artifactId.call(artifact, dependency),
                version.call(artifact, dependency), versionRange.call(artifact, dependency),
                includeMetadata, obfuscated, path.call(artifact, dependency),
                artifact.file, transformers
        )
    }

    @CompileStatic
    @FunctionalInterface
    static interface DependencyAction<T> {
        T call(ResolvedArtifact artifact, ModuleDependency dependency)
    }
}

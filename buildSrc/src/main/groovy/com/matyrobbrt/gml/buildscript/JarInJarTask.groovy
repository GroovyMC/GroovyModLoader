package com.matyrobbrt.gml.buildscript

import com.matyrobbrt.gml.buildscript.util.PrivateJavaCalls
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import com.matyrobbrt.gml.buildscript.util.JiJUtils
import com.matyrobbrt.gml.buildscript.data.JiJDependencyData
import com.matyrobbrt.gml.buildscript.data.JiJDependency
import com.matyrobbrt.gml.buildscript.data.JiJDependency.Provider
import com.matyrobbrt.gml.buildscript.transform.TransformerManager
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.ConfigureUtil

import java.nio.file.Files
import java.util.function.Function
import java.util.function.Predicate

@CompileStatic
abstract class JarInJarTask extends Jar {
    public final CopySpec jarJarCopySpec
    protected final ConfigurableFileCollection metadataFiles = getProject().files({
        project.files(writeMetadata(new File(project.buildDir, "$name/metadata.json")))
    })
    private final TransformerManager transformerManager = TransformerManager.forFile(new File(project.buildDir, "$name/transformers.out"))

    protected final List<Provider> providers = []

    JarInJarTask() {
        super()
        this.jarJarCopySpec = this.mainSpec.addChild()
        this.jarJarCopySpec.into('META-INF/jarjar')
    }

    @Override
    @TaskAction
    protected void copy() {
        PrivateJavaCalls.renameFromMap(jarJarCopySpec, fileNames)
        jarJarCopySpec.from(includedFiles)
        jarJarCopySpec.from(metadataFiles)
        super.copy()
    }

    Map metadata
    @Input
    Map getMetadata() {
        if (this.@metadata === null) {
            final jsons = providers.stream().flatMap { it.resolve().stream() }
                    .filter { it.includeMetadata() }
                    .filter(distinct())
                    .sorted(Comparator.<JiJDependency, String>comparing({ JiJDependency dep -> dep.group() + ':' + dep.version() } as Function<JiJDependency, String>))
                    .map {
                        JiJUtils.makeJarJsonMap(
                                it.group(), it.artifact(), it.version(), it.jarPath(), it.range(), it.isObfuscated()
                        ) }.toList()
            this.@metadata = [
                    'jars': jsons
            ]
        }
        return this.@metadata
    }

    protected File writeMetadata(File file) {
        final path = file.toPath()
        if (!Files.exists(path)) {
            Files.createDirectories(path.parent)
        }
        Files.writeString(path, JsonOutput.prettyPrint(new JsonBuilder(getMetadata()).toString()))
        return file
    }

    void fromConfiguration(Configuration... configurations) {
        provider(new JiJDependency.ConfigurationDependencyProvider(project, List.of(configurations)))
    }

    void fromJar(Jar jar, @DelegatesTo(JiJDependencyData) Closure configuration = null) {
        provider(JiJDependency.fromJar(() -> jar, ConfigureUtil.configure(configuration, new JiJDependencyData())))
        dependsOn(jar)
    }

    void fromJar(TaskProvider<? extends Task> jar, @DelegatesTo(JiJDependencyData) Closure configuration = null) {
        provider(JiJDependency.fromJar(() -> (Jar)jar.get(), ConfigureUtil.configure(configuration, new JiJDependencyData())))
        dependsOn(jar)
    }

    <T extends Provider> T provider(T provider) {
        providers.add(provider)
        return provider
    }

    FileCollection includedFiles

    @InputFiles
    FileCollection getIncludedFiles() {
        if (this.@includedFiles === null) {
            return this.@includedFiles = project.files(providers.stream()
                    .flatMap { it.resolve().stream() }
                    .filter(distinct())
                    .map { getFileAndTransform(it) }
                    .sorted()
                    .toArray())
        }
        return this.@includedFiles
    }

    File getFileAndTransform(JiJDependency dependency) {
        if (dependency.transformers().isEmpty()) {
            fileNames[dependency.file().name] = dependency.jarPath()
            return dependency.file()
        }

        final newFilePath = new File(project.buildDir, "$name/transformed/${getNewName(dependency)}")
        if (!newFilePath.exists()) {
            final newFilePathPath = newFilePath.toPath()
            Files.createDirectories(newFilePathPath.parent)
            try (final stream = dependency.file().newInputStream()) {
                Files.write(newFilePathPath, applyTransformers(stream, dependency))
            }
        }
        return newFilePath
    }

    private final Map<String, String> fileNames = [:]
    private String getNewName(JiJDependency dependency) {
        final fileName = transformerManager.getName(dependency)
        fileNames[fileName] = dependency.jarPath()
        return fileName
    }

    private static byte[] applyTransformers(InputStream input, JiJDependency dependency) {
        byte[] bytes = input.readAllBytes()
        dependency.transformers().each {
            bytes = it.transform(dependency, bytes)
        }
        return bytes
    }

    private static Predicate<JiJDependency> distinct() {
        Set<File> files = new HashSet<>()
        Map<String, String> versions = new HashMap<>()
        return { JiJDependency dep ->
            if (!files.add(dep.file())) return false
            String oldVersion = versions.put(dep.group() + ':' + dep.artifact(), dep.version())
            if (oldVersion) {
                if (new DefaultArtifactVersion(oldVersion) < new DefaultArtifactVersion(dep.version())) {
                    return true
                }
                // Keep the old version in case it is greater than the current
                versions.put(dep.group() + ':' + dep.artifact(), oldVersion)
                return false
            }
            return true
        } as Predicate<JiJDependency>
    }
}
package jij


import groovy.transform.CompileStatic
import jij.JiJDependency.Provider
import jij.hash.HashFunction
import jij.transform.ArtifactTransformer
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

import java.nio.file.Files
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

@CompileStatic
abstract class JarInJarTask extends Jar {
    private final CopySpec jarJarCopySpec
    protected final ConfigurableFileCollection metadata = getProject().files({
        project.files(writeMetadata(new File(project.buildDir, "$name/metadata.json")))
    })

    protected final List<Provider> providers = []
    protected final Map<String, ArtifactTransformer> transformers = new HashMap<>()
    protected Function<String, ArtifactTransformer> transformerFunction = { null }

    JarInJarTask() {
        super()
        this.jarJarCopySpec = this.mainSpec.addChild()
        this.jarJarCopySpec.into('META-INF/jarjar')
    }

    @Override
    @TaskAction
    protected void copy() {
        PrivateJavaCalls.renameFromMap(jarJarCopySpec, fileHashToName)
        jarJarCopySpec.from(includedFiles)
        jarJarCopySpec.from(metadata)
        super.copy()
    }

    protected File writeMetadata(File path) {
        final jsons = providers.stream().flatMap { it.resolve().stream() }
                .filter(distinct())
                .sorted(Comparator.<JiJDependency, String>comparing({ JiJDependency dep -> dep.group() + ':' + dep.version() } as Function<JiJDependency, String>))
                .map {
                    JiJUtils.makeJarJson(
                        it.group(), it.artifact(), it.version(), it.file().name, it.range()
                ) }.toList()
        JiJUtils.writeJiJ(path, jsons)
        return path
    }

    void fromJar(Jar jar, String versionRange = null) {
        provider(JiJDependency.fromJar(() -> jar, versionRange))
        dependsOn(jar)
    }

    void fromJar(TaskProvider<? extends Task> jar, String versionRange = null) {
        provider(JiJDependency.fromJar(() -> (Jar)jar.get(), versionRange))
        dependsOn(jar)
    }

    void fromConfiguration(Configuration configuration, @DelegatesTo(
            value = JiJDependency.ConfigurationDependencyProvider,
            strategy = Closure.DELEGATE_FIRST
    ) Closure configurator = {}) {
        final prov = provider(new JiJDependency.ConfigurationDependencyProvider(project, [configuration]))
        configurator.delegate = prov
        configurator.resolveStrategy = Closure.DELEGATE_FIRST
        configurator(prov)
    }

    <T extends Provider> T provider(T provider) {
        providers.add(provider)
        return provider
    }

    void transform(String artifact, ArtifactTransformer transformer) {
        transformers[artifact] = transformer
    }

    void setTransformerFunction(Function<String, ArtifactTransformer> transformerFunction) {
        this.transformerFunction = transformerFunction
    }

    FileCollection includedFiles

    @InputFiles
    FileCollection getIncludedFiles() {
        if (this.@includedFiles === null) {
            return this.@includedFiles = project.files(providers.stream().flatMap { it.resolve().stream() }
                    .filter(distinct())
                    .map { getFileAndTransform(it) }
                    .sorted()
                    .toArray())
        }
        return this.@includedFiles
    }

    File getFileAndTransform(JiJDependency dependency) {
        final transformer = transformers.computeIfAbsent(dependency.group() + ':' + dependency.artifact(), transformerFunction)
        if (transformer === null || !transformer.shouldTransform(dependency)) return dependency.file()
        final newFilePath = new File(project.buildDir, "$name/transformed/${getHash(dependency, transformer)}.jar")
        if (!newFilePath.exists()) {
            final newFilePathPath = newFilePath.toPath()
            Files.createDirectories(newFilePathPath.parent)
            Files.write(newFilePathPath, transformer.transform(dependency))
        }
        return newFilePath
    }

    private final Map<String, String> fileHashToName = [:]
    private String getHash(JiJDependency dependency, ArtifactTransformer transformer) {
        final fileHash = HashFunction.SHA1.hash(dependency.file())
        final fullHash = HashFunction.MD5.hash("${dependency.group() + ':' + dependency.artifact() + ':' + dependency.version()}-$fileHash-${transformer.hash()}")
        fileHashToName[fullHash + '.jar'] = dependency.file().name
        return fullHash
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

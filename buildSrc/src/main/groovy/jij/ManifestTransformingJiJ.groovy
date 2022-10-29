package jij

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jij.JiJUtils
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

import javax.annotation.Nullable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Hack workaround until Forge changes the default mod type. <br>
 * This JiJ task transforms all non-mods JiJ'd dependencies and adds a FMLModType to their manifest.
 */
@CompileStatic
abstract class ManifestTransformingJiJ extends DefaultTask {
    public static final List<String> EXCLUSIONS = [
            'META-INF/MANIFEST.MF'
    ]
    private final List<Configuration> configurations = []
    @Internal List<Configuration> getConfigurations() { configurations }

    @Input
    abstract MapProperty<String, String> getRanges()
    @Input
    abstract MapProperty<String, String> getUpperBounds()
    @Input
    abstract MapProperty<Object, Object> getManifestAttributes()

    @OutputFile
    abstract RegularFileProperty getOutput()

    @TaskAction
    @CompileStatic
    void run() {
        final List<JsonObject> metadatas = []
        final Set<String> ids = []
        dependencies.each {
            final artifact = getResolvedDependency(it)
            artifact.allModuleArtifacts.each {
                final val = it.moduleVersion.id
                final id = val.group + ':' + val.name
                if (id in ids) return
                ids.add(id)
                metadatas.push(JiJUtils.makeJarJson(
                        val.group, val.name, val.version,
                        it.file.toPath().fileName.toString(),
                        resolveRange(id, val.version)
                ))
            }
        }

        final out = output.get().asFile.toPath()
        Files.createDirectories(out.parent)
        Files.deleteIfExists(out)

        try (final os = new ZipOutputStream(Files.newOutputStream(out))) {
            os.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
            final osAttr = new Attributes()
            manifestAttributes.get().forEach { Object key, Object value ->
                osAttr.putValue(key.toString(), value.toString())
            }
            writeManifest(osAttr, new DataOutputStream(os))
            os.closeEntry()

            resolvedDependencies.stream()
                    .flatMap { it.allModuleArtifacts.stream() }
                    .map { new WithDependencyMetadata(it, it.file) }
                    .distinct()
                    .forEach {
                final bytes = new ByteArrayOutputStream()
                try (final depOs = new JarOutputStream(bytes)) {
                    final asZip = new JarFile(it.file)

                    final manifest = new Manifest(asZip.manifest)
                    // Only transform if not a mod
                    if (asZip.getEntry('META-INF/mods.toml') === null) {
                        manifest.mainAttributes.putValue('FMLModType', 'LIBRARY')
                        // TODO do we actually want to replace the module name?
                        manifest.mainAttributes.putValue('Automatic-Module-Name', 'com.matyrobbrt.gml.groovyjij.' + it.dependency.moduleVersion.id.group + '.' 
                                + it.dependency.moduleVersion.id.name.replace('-', ''))
                    }
                    depOs.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
                    manifest.write(depOs)
                    depOs.closeEntry()

                    asZip.entries().asIterator().each {
                        if (it.name in EXCLUSIONS || it.name.endsWith('module-info.class')) return
                        depOs.putNextEntry(makeNewEntry(it))
                        depOs.write(asZip.getInputStream(it).readAllBytes())
                        depOs.closeEntry()
                    }
                }

                os.putNextEntry(new ZipEntry('META-INF/jarjar/' + it.file.toPath().fileName.toString()))
                os.write(bytes.toByteArray())
                os.closeEntry()
            }

            final var fullJson = new JsonObject()
            final jsonArray = new JsonArray()
            metadatas.each(jsonArray.&add)
            fullJson.add 'jars', jsonArray
            final metadataBytes = new GsonBuilder().setPrettyPrinting().create()
                    .toJson(fullJson).getBytes(StandardCharsets.UTF_8)
            os.putNextEntry(new ZipEntry('META-INF/jarjar/metadata.json'))
            os.write(metadataBytes)
            os.closeEntry()
        }
    }

    void setRange(String artifact, String range) {
        ranges.put(artifact, range)
    }
    void setUpperBound(String artifact, String upperBound) {
        upperBounds.put(artifact, upperBound)
    }

    String resolveRange(String artifact, String version) {
        if (ranges.get().containsKey(artifact)) return ranges.get()[artifact]
        if (upperBounds.get().containsKey(artifact)) {
            return "[${version},${upperBounds.get()[artifact]})"
        }
        return null
    }

    @Internal
    Set<ExternalModuleDependency> getDependencies() {
        return this.configurations.stream().flatMap(config -> config.getAllDependencies().stream())
                .filter { it instanceof ExternalModuleDependency }
                .map { it as ExternalModuleDependency }
                .collect(Collectors.toSet())
    }

    @Internal
    Set<ResolvedDependency> getResolvedDependencies() {
        return getDependencies().stream()
                .map(this::getResolvedDependency)
                .collect(Collectors.toSet())
    }

    @InputFiles
    FileCollection getInputDependencies() {
        return project.files(resolvedDependencies.stream()
                .flatMap { it.allModuleArtifacts.stream() }
                .map { it.file }
                .sorted()
                .<Object>map({it})
                .toArray(Object[]::new))
    }

    void setConfigurations(List<Configuration> configurations) {
        this.configurations.clear()
        this.configurations.addAll(configurations)
    }

    void configuration(@Nullable final Configuration configuration) {
        if (configuration == null) {
            return
        }

        this.configurations.add(configuration)
    }

    static ZipEntry makeNewEntry(ZipEntry oldEntry) {
        ZipEntry newEntry = new ZipEntry(oldEntry.name)
        if (oldEntry.getComment() !== null) newEntry.setComment(oldEntry.getComment())
        return newEntry
    }

    @CompileStatic
    private ResolvedDependency getResolvedDependency(final ExternalModuleDependency dependency) {
        ExternalModuleDependency toResolve = dependency.copy()
        toResolve.version(constraint -> constraint.strictly(dependency.version))

        final Set<ResolvedDependency> deps = getProject().getConfigurations().detachedConfiguration(toResolve).getResolvedConfiguration().getFirstLevelModuleDependencies()
        if (deps.isEmpty()) {
            throw new IllegalArgumentException("Failed to resolve: $toResolve")
        }

        return deps.iterator().next()
    }

    static void writeManifest(Attributes attributes, DataOutputStream out) throws IOException {
        StringBuilder buffer = new StringBuilder(72)
        for (Map.Entry<Object, Object> e : attributes.entrySet()) {
            buffer.setLength(0)
            buffer.append(e.getKey().toString())
            buffer.append(": ")
            buffer.append(e.getValue())
            println72(out, buffer.toString())
        }
        println(out)
    }

    private static final byte[] NEW_LINE = '\r\n'.getBytes(StandardCharsets.UTF_8)
    private static final byte[] SPACE = ' '.getBytes(StandardCharsets.UTF_8)

    private static void println(OutputStream out) throws IOException {
        out.write(NEW_LINE)
    }

    private static void println72(OutputStream out, String line) throws IOException {
        if (!line.isEmpty()) {
            byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8)
            int length = lineBytes.length
            // first line can hold one byte more than subsequent lines which
            // start with a continuation line break space
            out.write(lineBytes[0])
            int pos = 1
            while (length - pos > 71) {
                out.write(lineBytes, pos, 71)
                pos += 71
                println(out)
                out.write(SPACE)
            }
            out.write(lineBytes, pos, length - pos)
        }
        println(out)
    }

    @Canonical
    @CompileStatic
    static class WithDependencyMetadata {
        ResolvedArtifact dependency
        File file

        @Override
        int hashCode() {
            return Objects.hash(file)
        }

        @Override
        boolean equals(Object obj) {
            return obj instanceof WithDependencyMetadata && obj.file == file
        }
    }
}

import com.github.javaparser.resolution.declarations.ResolvedDeclaration
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.bundling.Jar

import java.nio.charset.StandardCharsets
import java.nio.file.Files

@CompileStatic
class JiJUtils {
    static JsonObject makeJarJson(
            String group, String artifact, String version, String path,
            String range = null
    ) {
        final subJson = new JsonObject()

        final identifier = new JsonObject()
        identifier.addProperty('group', group)
        identifier.addProperty('artifact', artifact)
        subJson.add('identifier', identifier)

        final versionJson = new JsonObject()
        versionJson.addProperty('range', (range ?: "[$version,)") as String)
        versionJson.addProperty('artifactVersion', version)
        subJson.add('version', versionJson)

        subJson.addProperty('path', "META-INF/jarjar/$path")
        subJson.addProperty('isObfuscated', false)

        return subJson
    }

    static void writeJiJ(File path, JsonObject ... objects) {
        Files.createDirectories(path.toPath().parent)
        try (final out = Files.newOutputStream(path.toPath())) {
            final var fullJson = new JsonObject()
            final jsonArray = new JsonArray()
            objects.each(jsonArray.&add)
            fullJson.add 'jars', jsonArray
            out.write(new Gson().toJson(fullJson).getBytes(StandardCharsets.UTF_8))
        }
    }

    static ResolvedDependency getResolvedDependency(final Project project, final ExternalModuleDependency dependency) {
        ExternalModuleDependency toResolve = dependency.copy()
        toResolve.version(constraint -> constraint.strictly(dependency.version))

        final Set<ResolvedDependency> deps = project.getConfigurations().detachedConfiguration(toResolve).getResolvedConfiguration().getFirstLevelModuleDependencies()
        if (deps.isEmpty()) {
            throw new IllegalArgumentException("Failed to resolve: $toResolve")
        }

        return deps.iterator().next()
    }

    @CompileDynamic
    static void includeDependency(final Jar task, final ExternalModuleDependency dependency, final List<JsonObject> jars) {
        final project = task.project
        final resolved = getResolvedDependency(project, dependency)
        final file = resolved.allModuleArtifacts.find().file
        task.from(project.files(file)) { CopySpec spec ->
            spec.into 'META-INF/jarjar/'
        }
        jars.add(makeJarJson(
                dependency.group, dependency.name, dependency.version,
                file.getName()
        ))
    }
}

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import groovy.transform.CompileStatic

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
        try (final out = Files.newOutputStream(path.toPath())) {
            final var fullJson = new JsonObject()
            final jsonArray = new JsonArray()
            objects.each(jsonArray.&add)
            fullJson.add 'jars', jsonArray
            out.write(new Gson().toJson(fullJson).getBytes(StandardCharsets.UTF_8))
        }
    }
}

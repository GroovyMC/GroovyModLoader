package jij;

import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.file.CopySpec;

import java.util.Map;

class PrivateJavaCalls {
    static void versionString(ExternalModuleDependency dependency, String version) {
        dependency.version(mutableVersionConstraint -> mutableVersionConstraint.strictly(version));
    }

    static void renameFromMap(CopySpec spec, Map<String, String> map) {
        spec.rename(s -> map.getOrDefault(s, s));
    }
}

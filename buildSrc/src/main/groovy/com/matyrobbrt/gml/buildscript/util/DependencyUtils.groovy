package com.matyrobbrt.gml.buildscript.util

import groovy.transform.CompileStatic
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
import org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency

@CompileStatic
class DependencyUtils {
    static boolean isValidVersionRange(final String range) {
        try {
            final VersionRange data = VersionRange.createFromVersionSpec(range)
            return data.hasRestrictions() && data.getRecommendedVersion() == null && !range.contains('+')
        } catch (InvalidVersionSpecificationException ignored) {
            return false
        }
    }

    static ResolvedDependency getResolvedDependency(final Project project, final ModuleDependency dependency) {
        ModuleDependency toResolve = dependency.copy()
        if (toResolve instanceof ExternalModuleDependency) {
            PrivateJavaCalls.versionString(toResolve, dependency.version)
        }

        final Set<ResolvedDependency> deps = project.getConfigurations().detachedConfiguration(toResolve).getResolvedConfiguration().getFirstLevelModuleDependencies()
        if (deps.isEmpty()) {
            throw new IllegalArgumentException("Failed to resolve: $toResolve")
        }

        return deps.iterator().next()
    }

    static String nextMajor(String version) {
        (Integer.parseInt(version.split('\\.')[0]) + 1) + '.0.0'
    }

    static String range(String lower, String upper) {
        return "[$lower,$upper)"
    }
}

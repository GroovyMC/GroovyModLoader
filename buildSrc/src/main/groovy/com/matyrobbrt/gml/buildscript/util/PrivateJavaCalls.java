package com.matyrobbrt.gml.buildscript.util;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.CopySpec;

import java.util.Map;

public class PrivateJavaCalls {
    public static void versionString(ExternalModuleDependency dependency, String version) {
        dependency.version(mutableVersionConstraint -> mutableVersionConstraint.strictly(version));
    }

    public static void renameFromMap(CopySpec spec, Map<String, String> map) {
        spec.rename(s -> map.getOrDefault(s, s));
    }

    @SuppressWarnings("rawtypes")
    public static void configureAttributes(ModuleDependency dependency, @DelegatesTo(
            value = AttributeContainer.class, strategy = Closure.DELEGATE_FIRST
    ) Closure closure) {
        dependency.attributes(attributeContainer -> {
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(attributeContainer);
            closure.call(attributeContainer);
        });
    }
}

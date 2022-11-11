package com.matyrobbrt.gml.buildscript.transform

import groovy.transform.CompileStatic
import com.matyrobbrt.gml.buildscript.data.JiJDependency

@CompileStatic
interface ArtifactTransformer {
    byte[] transform(JiJDependency dependency, byte[] dependencyBytes)

    String hash()
}
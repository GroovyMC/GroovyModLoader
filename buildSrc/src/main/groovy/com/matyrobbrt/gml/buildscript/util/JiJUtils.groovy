package com.matyrobbrt.gml.buildscript.util

import groovy.transform.CompileStatic
import org.gradle.api.file.RelativePath

@CompileStatic
class JiJUtils {
    private static final RelativePath ROOT_PATH = RelativePath.parse(false, 'META-INF/jarjar/')

    static Map makeJarJsonMap(
            String group, String artifact, String version, String path,
            String range = null, boolean obfuscated = false
    ) {
        return [
                identifier: [
                        'group': group,
                        'artifact': artifact
                ],
                version: [
                        artifactVersion: version,
                        range: (range ?: "[$version,)").toString()
                ],
                path: walkPaths(RelativePath.parse(true, ROOT_PATH, path).segmentIterator()).join('/'),
                isObfuscated: obfuscated
        ]
    }

    @SuppressWarnings('GroovyEmptyStatementBody')
    static List<String> walkPaths(ListIterator<String> iterator) {
        final paths = new ArrayList<String>()
        while (iterator.hasNext()) {
            final next = iterator.next()
            if (next == '.') {}
            else if (next == '..') {
                paths.removeLast()
            } else {
                paths.add(next)
            }
        }
        return paths
    }
}

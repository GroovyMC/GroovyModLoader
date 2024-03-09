package org.groovymc.gml.mod

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.groovymc.gml.GMod

@GMod('gml')
@CompileStatic
@Slf4j(category = 'gml')
final class GMLMod {
    GMLMod() {
        log.info('Initialised GML mod. Version: {}', getClass().getPackage().implementationVersion)
    }
}

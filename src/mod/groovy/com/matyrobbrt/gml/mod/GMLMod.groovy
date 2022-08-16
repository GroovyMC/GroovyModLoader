package com.matyrobbrt.gml.mod

import com.matyrobbrt.gml.GMod
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@GMod('gml')
@CompileStatic
@Slf4j(category = 'gml')
final class GMLMod {
    GMLMod() {
        log.info('Initialised GML mod. Version: {}', getClass().getPackage().implementationVersion)
    }
}

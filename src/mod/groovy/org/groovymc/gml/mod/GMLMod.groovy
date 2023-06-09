/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.mod

import org.groovymc.gml.GMod
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

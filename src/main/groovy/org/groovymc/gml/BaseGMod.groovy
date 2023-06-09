/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml

import org.groovymc.gml.bus.GModEventBus
import groovy.transform.CompileStatic
import net.minecraftforge.eventbus.api.IEventBus

/**
 * An interface providing different CompileStatic utility methods for {@link org.groovymc.gml.GMod GMods}.
 */
@CompileStatic
interface BaseGMod {
    /**
     * Gets the mod event bus.
     * @return the mod event bus
     */
    default GModEventBus getModBus() {
        throw new IllegalArgumentException('Transformer failed injection')
    }
    /**
     * Gets the forge event bus.
     * @return the forge event bus
     */
    default IEventBus getForgeBus() {
        throw new IllegalArgumentException('Transformer failed injection')
    }
}
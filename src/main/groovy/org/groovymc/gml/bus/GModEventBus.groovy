/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.bus

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import net.minecraftforge.eventbus.api.IEventBus

/**
 * An {@link IEventBus} that may provide some groovy-specific methods in the future.
 */
@Canonical
@CompileStatic
final class GModEventBus implements IEventBus {
    @Delegate
    final IEventBus delegate
}

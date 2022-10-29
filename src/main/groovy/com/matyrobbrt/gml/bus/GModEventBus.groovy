/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.bus

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import net.minecraftforge.eventbus.api.IEventBus

@Canonical
@CompileStatic
final class GModEventBus implements IEventBus {
    @Delegate
    final IEventBus delegate
}

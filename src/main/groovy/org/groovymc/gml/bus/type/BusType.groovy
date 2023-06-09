/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.bus.type

/**
 * Indicates a type of event bus.
 * @see ForgeBus
 * @see ModBus
 */
sealed interface BusType permits ForgeBus, ModBus {
}
